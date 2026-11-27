package de.geosphere.congregationplaner.notice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.notice.data.NoticeFileSource
import de.geosphere.congregationplaner.notice.data.NoticePdfRenderer
import de.geosphere.congregationplaner.notice.domain.model.DEFAULT_NOTICE_ROWS
import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGES
import de.geosphere.congregationplaner.notice.domain.model.MAX_NOTICE_ROWS
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.model.buildTalkNotice
import de.geosphere.congregationplaner.notice.domain.usecase.AddNoticeImageUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.DeleteNoticeImageUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.LoadNoticeImagesUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.ObserveTalkNoticeSettingsUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.RenameNoticeImageUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.SaveTalkNoticeSettingsUseCase
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import de.geosphere.congregationplaner.notice.domain.model.isValidImageName as isValidName

private const val DEFAULT_IMAGE_NAME = "Grafik"

/** Like [runCatching], but a cancelled coroutine stays cancelled instead of becoming a failure. */
private inline fun <T> runCatchingKeepingCancellation(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
    Result.failure(e)
}

data class TalkNoticeUiState(
    val canManage: Boolean = false,
    val isAvailable: Boolean = false,
    val settings: TalkNoticeSettings = TalkNoticeSettings(),
    val titleDraft: String = "",
    val images: List<NoticeImage> = emptyList(),
    val start: LocalDate? = null,
    val count: Int = DEFAULT_NOTICE_ROWS,
    /** The graphic on the next PDF; `null` means none. */
    val selectedImageId: String? = null,
    val isBusy: Boolean = false,
    val savedFile: String? = null,
    val errorMessage: String? = null,
    val pendingDeleteImage: NoticeImage? = null,
) {
    val canAddImage: Boolean get() = !isBusy && images.size < MAX_IMAGES
    val canDecreaseCount: Boolean get() = count > 1
    val canIncreaseCount: Boolean get() = count < MAX_NOTICE_ROWS
}

/** Drives the talk notice section: settings, graphics, and creating the PDF. */
@Inject
@Suppress("LongParameterList", "TooManyFunctions") // one screen with many independent actions
class TalkNoticeViewModel(
    private val observeSettings: ObserveTalkNoticeSettingsUseCase,
    private val saveSettings: SaveTalkNoticeSettingsUseCase,
    private val loadImages: LoadNoticeImagesUseCase,
    private val addImageUseCase: AddNoticeImageUseCase,
    private val renameImageUseCase: RenameNoticeImageUseCase,
    private val deleteImage: DeleteNoticeImageUseCase,
    private val renderer: NoticePdfRenderer,
    private val fileSource: NoticeFileSource,
    private val schedule: TalkScheduleRepository,
    private val congregations: CongregationMetadataRepository,
    private val policy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TalkNoticeUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private var settingsJob: Job? = null
    private var loadJob: Job? = null
    private var busyJob: Job? = null
    private var settingsLoaded = false
    private var imagesLoaded = false
    private var selectionInitialised = false
    private val slotCache = mutableMapOf<Int, List<TalkSlot>>()

    fun onCurrentUserChanged(user: AppUser, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())) {
        val congregation = user.congregation
        val canManage = policy.canPlanTalks(user) && congregation != null
        val unchanged = currentUser?.congregation == congregation && mutableUiState.value.canManage == canManage
        currentUser = user
        if (unchanged) return
        this.today = today
        // Stale work of the previous user must not write into the new state.
        settingsJob?.cancel()
        loadJob?.cancel()
        busyJob?.cancel()
        settingsLoaded = false
        imagesLoaded = false
        selectionInitialised = false
        slotCache.clear()
        val isAvailable = renderer.isAvailable && fileSource.isAvailable
        mutableUiState.value = TalkNoticeUiState(canManage = canManage, isAvailable = isAvailable)
        if (!canManage || congregation == null || !isAvailable) return
        settingsJob = viewModelScope.launch {
            observeSettings(user)
                .catch {
                    if (it is CancellationException) throw it
                    fail("Die Einstellungen konnten nicht geladen werden: ${it.message}")
                }
                .collect { settings ->
                    val first = !settingsLoaded
                    settingsLoaded = true
                    mutableUiState.update {
                        // The initial state already holds the default title: the first emission always fills the draft.
                        val fillDraft = first || it.settings.title != settings.title
                        it.copy(settings = settings, titleDraft = if (fillDraft) settings.title else it.titleDraft)
                    }
                    initSelection()
                }
        }
        loadJob = viewModelScope.launch {
            refreshImages(user)
            val start = firstSlotDate(congregation, today)
            mutableUiState.update { it.copy(start = start ?: today) }
        }
    }

    // Reads the next year only if this year has no slot left.
    private suspend fun firstSlotDate(congregation: CongregationRef, today: LocalDate): LocalDate? =
        runCatchingKeepingCancellation {
            listOf(today.year, today.year + 1).firstNotNullOfOrNull { year ->
                slotsOfYear(congregation, year).map { it.date }.filter { it >= today }.minOrNull()
            }
        }.getOrNull()

    private suspend fun slotsOfYears(congregation: CongregationRef, year: Int): List<TalkSlot> =
        slotsOfYear(congregation, year) + slotsOfYear(congregation, year + 1)

    /** Reads the slots of [year] once per session; nothing stays subscribed. */
    private suspend fun slotsOfYear(congregation: CongregationRef, year: Int): List<TalkSlot> =
        slotCache[year] ?: schedule.observeYear(congregation, year).first().also { slotCache[year] = it }

    private suspend fun refreshImages(user: AppUser) {
        loadImages(user)
            .onSuccess { images ->
                imagesLoaded = true
                mutableUiState.update { state ->
                    state.copy(
                        images = images,
                        selectedImageId = state.selectedImageId?.takeIf { id -> images.any { it.id == id } },
                    )
                }
                initSelection()
            }
            .onFailure { fail(it.message.orEmpty()) }
    }

    /** Preselects the default graphic once, as soon as both the settings and the images are known. */
    private fun initSelection() {
        if (selectionInitialised || !settingsLoaded || !imagesLoaded) return
        selectionInitialised = true
        mutableUiState.update { state ->
            val default = state.settings.defaultImageId
            state.copy(selectedImageId = default?.takeIf { id -> state.images.any { it.id == id } })
        }
    }

    fun editTitle(text: String) = mutableUiState.update { it.copy(titleDraft = text) }

    fun saveTitle() = changeSettings { it.copy(title = mutableUiState.value.titleDraft) }

    fun chooseColorSet(set: NoticeColorSet) = changeSettings { it.copy(colorSet = set) }

    fun makeDefault(id: String?) = changeSettings { it.copy(defaultImageId = id) }

    private fun changeSettings(change: (TalkNoticeSettings) -> TalkNoticeSettings) {
        val user = manager() ?: return
        launchBusy {
            saveSettings(user, change(mutableUiState.value.settings))
                .onSuccess { saved -> mutableUiState.update { it.copy(isBusy = false, settings = saved) } }
                .onFailure { fail(it.message.orEmpty()) }
        }
    }

    /** The current user if this view model may act for them right now. */
    private fun manager(): AppUser? =
        currentUser?.takeIf { mutableUiState.value.canManage && !mutableUiState.value.isBusy }

    fun addImage() {
        val user = manager() ?: return
        launchBusy {
            val picked = fileSource.pickPng().getOrElse {
                return@launchBusy fail("Die Datei konnte nicht gelesen werden: ${it.message}")
            }
            if (picked == null) {
                mutableUiState.update { it.copy(isBusy = false) }
                return@launchBusy
            }
            addImageUseCase(user, picked.name.ifBlank { DEFAULT_IMAGE_NAME }, picked.bytes)
                .onSuccess { finishImageChange(user) }
                .onFailure { fail(it.message.orEmpty()) }
        }
    }

    fun isValidImageName(name: String): Boolean = isValidName(name)

    fun renameImage(id: String, name: String) {
        val user = manager() ?: return
        launchBusy {
            renameImageUseCase(user, id, name)
                .onSuccess { finishImageChange(user) }
                .onFailure { fail(it.message.orEmpty()) }
        }
    }

    fun requestDeleteImage(image: NoticeImage?) = mutableUiState.update { it.copy(pendingDeleteImage = image) }

    fun confirmDeleteImage() {
        val user = manager() ?: return
        val image = mutableUiState.value.pendingDeleteImage ?: return
        mutableUiState.update { it.copy(pendingDeleteImage = null) }
        launchBusy {
            deleteImage(user, image.id)
                .onSuccess { finishImageChange(user) }
                .onFailure { fail(it.message.orEmpty()) }
        }
    }

    private suspend fun finishImageChange(user: AppUser) {
        refreshImages(user)
        mutableUiState.update { it.copy(isBusy = false) }
    }

    fun selectImage(id: String?) = mutableUiState.update { it.copy(selectedImageId = id) }

    fun changeStart(date: LocalDate) = mutableUiState.update { it.copy(start = date) }

    fun changeCount(count: Int) = mutableUiState.update { it.copy(count = count.coerceIn(1, MAX_NOTICE_ROWS)) }

    fun decreaseCount() = changeCount(mutableUiState.value.count - 1)

    fun increaseCount() = changeCount(mutableUiState.value.count + 1)

    fun createPdf() {
        val user = manager() ?: return
        val congregation = user.congregation ?: return
        val state = mutableUiState.value
        val start = state.start ?: return
        launchBusy {
            val slots = runCatchingKeepingCancellation { slotsOfYears(congregation, start.year) }.getOrElse {
                return@launchBusy fail("Die Termine konnten nicht gelesen werden: ${it.message}")
            }
            val name = runCatchingKeepingCancellation { congregationName(congregation) }.getOrDefault("")
            val notice = buildTalkNotice(slots, start, state.count, state.settings.title, name, today)
            val png = state.images.firstOrNull { it.id == state.selectedImageId }?.png
            val bytes = renderer.render(notice, state.settings.colorSet, png).getOrElse {
                return@launchBusy fail("Der Aushang konnte nicht erstellt werden: ${it.message}")
            }
            val fileName = "Aushang_$start.pdf"
            fileSource.savePdf(fileName, bytes)
                .onSuccess { saved -> mutableUiState.update { it.copy(isBusy = false, savedFile = saved) } }
                .onFailure { fail("Die Datei konnte nicht gespeichert werden: ${it.message}") }
        }
    }

    private suspend fun congregationName(congregation: CongregationRef): String =
        congregations.observeAll(congregation.circuitIds).first()
            .firstOrNull { it.id == congregation.congregationId }?.name.orEmpty()

    fun dismissSaved() = mutableUiState.update { it.copy(savedFile = null) }

    fun dismissError() = mutableUiState.update { it.copy(errorMessage = null) }

    private fun launchBusy(block: suspend () -> Unit) {
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null, savedFile = null) }
        busyJob = viewModelScope.launch { block() }
    }

    private fun fail(message: String) = mutableUiState.update { it.copy(isBusy = false, errorMessage = message) }
}
