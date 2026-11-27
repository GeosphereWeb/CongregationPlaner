package de.geosphere.congregationplaner.notice.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.testing.FakeCongregationMetadataRepository
import de.geosphere.congregationplaner.notice.data.NoticeFileSource
import de.geosphere.congregationplaner.notice.data.NoticePdfRenderer
import de.geosphere.congregationplaner.notice.data.PickedPng
import de.geosphere.congregationplaner.notice.domain.model.DEFAULT_NOTICE_TITLE
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.notice.domain.usecase.AddNoticeImageUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.DeleteNoticeImageUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.FakeTalkNoticeRepository
import de.geosphere.congregationplaner.notice.domain.usecase.LoadNoticeImagesUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.ObserveTalkNoticeSettingsUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.RenameNoticeImageUseCase
import de.geosphere.congregationplaner.notice.domain.usecase.SaveTalkNoticeSettingsUseCase
import de.geosphere.congregationplaner.notice.testing.testPng
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val PNG = testPng()

private class FakeRenderer(override val isAvailable: Boolean = true) : NoticePdfRenderer {
    var result: Result<ByteArray> = Result.success(byteArrayOf(7, 7))
    var notice: TalkNotice? = null
    var colors: NoticeColorSet? = null
    var png: ByteArray? = null

    override suspend fun render(notice: TalkNotice, colors: NoticeColorSet, png: ByteArray?): Result<ByteArray> {
        this.notice = notice
        this.colors = colors
        this.png = png
        return result
    }
}

private class FakeNoticeFileSource(override val isAvailable: Boolean = true) : NoticeFileSource {
    var picked: Result<PickedPng?> = Result.success(null)
    var saveResult: Result<String?>? = null
    val saved = mutableListOf<Pair<String, ByteArray>>()

    override suspend fun pickPng(): Result<PickedPng?> = picked

    override suspend fun savePdf(fileName: String, bytes: ByteArray): Result<String?> {
        saved += fileName to bytes
        return saveResult ?: Result.success(fileName)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TalkNoticeViewModelTest {
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)
    private val today = LocalDate(2026, 11, 27)

    private val notices = FakeTalkNoticeRepository()
    private val schedule = FakeTalkScheduleRepository(
        listOf(
            TalkSlot(LocalDate(2026, 11, 22), LocalTime(10, 0)),
            TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0), talkNumber = 5, talkTitle = "Titel"),
            TalkSlot(LocalDate(2027, 1, 3), LocalTime(10, 0)),
        ),
    )
    private val requestedYears = mutableListOf<Int>()

    // Like the real repository: only the slots of the requested year.
    private val yearSchedule = object : TalkScheduleRepository by schedule {
        override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> {
            requestedYears += year
            return schedule.observeYear(congregation, year).map { slots -> slots.filter { it.date.year == year } }
        }
    }
    private val renderer = FakeRenderer()
    private val fileSource = FakeNoticeFileSource()
    private val policy = TalkSchedulePermissionPolicy()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: TalkNoticeRepository = notices,
        rendererFake: NoticePdfRenderer = renderer,
        metadata: List<CongregationMetadata> = listOf(
            CongregationMetadata(id = testCongregation.congregationId, name = "Berlin-Mitte"),
        ),
    ) = TalkNoticeViewModel(
        ObserveTalkNoticeSettingsUseCase(repository, policy),
        SaveTalkNoticeSettingsUseCase(repository, policy),
        LoadNoticeImagesUseCase(repository, policy),
        AddNoticeImageUseCase(repository, policy),
        RenameNoticeImageUseCase(repository, policy),
        DeleteNoticeImageUseCase(repository, policy),
        rendererFake,
        fileSource,
        yearSchedule,
        FakeCongregationMetadataRepository(metadata),
        policy,
    )

    @Test
    fun `a member cannot manage and nothing is loaded`() {
        val vm = viewModel().apply { onCurrentUserChanged(member, today) }

        assertEquals(false, vm.uiState.value.canManage)
        assertNull(vm.uiState.value.start)
        assertTrue(vm.uiState.value.images.isEmpty())
        assertTrue(requestedYears.isEmpty())
    }

    @Test
    fun `a planner gets settings and images and the first slot from today`() {
        notices.images += NoticeImage("img", "Logo", PNG)
        notices.settings.value = TalkNoticeSettings(title = "Aushang", defaultImageId = "img")

        val state = viewModel().apply { onCurrentUserChanged(planner, today) }.uiState.value

        assertTrue(state.canManage)
        assertTrue(state.isAvailable)
        assertEquals("Aushang", state.settings.title)
        assertEquals("Aushang", state.titleDraft)
        assertEquals(listOf("img"), state.images.map { it.id })
        assertEquals("img", state.selectedImageId)
        assertEquals(LocalDate(2026, 12, 6), state.start)
    }

    @Test
    fun `without a later slot the start is today`() {
        schedule.slots.value = emptyList()

        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        assertEquals(today, vm.uiState.value.start)
    }

    @Test
    fun `availability needs the renderer and the file dialogs`() {
        val vm = viewModel(rendererFake = FakeRenderer(isAvailable = false)).apply {
            onCurrentUserChanged(planner, today)
        }

        assertEquals(false, vm.uiState.value.isAvailable)
    }

    @Test
    fun `count is clamped to the rows that fit`() {
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.changeCount(20)
        assertEquals(11, vm.uiState.value.count)
        vm.changeCount(0)
        assertEquals(1, vm.uiState.value.count)
    }

    @Test
    fun `the count steppers stay within the rows that fit`() {
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        repeat(20) { vm.increaseCount() }
        assertEquals(11, vm.uiState.value.count)
        assertTrue(!vm.uiState.value.canIncreaseCount)
        assertTrue(vm.uiState.value.canDecreaseCount)
        repeat(20) { vm.decreaseCount() }
        assertEquals(1, vm.uiState.value.count)
        assertTrue(!vm.uiState.value.canDecreaseCount)
        assertTrue(vm.uiState.value.canIncreaseCount)
    }

    @Test
    fun `a graphic can be added until the limit is reached`() {
        val image = NoticeImage("x", "Saal", byteArrayOf(1))
        assertTrue(TalkNoticeUiState(images = List(9) { image }).canAddImage)
        assertTrue(!TalkNoticeUiState(images = List(10) { image }).canAddImage)
    }

    @Test
    fun `createPdf builds the notice and saves the file`() {
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }
        vm.changeCount(5)

        vm.createPdf()

        val state = vm.uiState.value
        assertEquals("Aushang_2026-12-06.pdf", fileSource.saved.single().first)
        assertContentEquals(byteArrayOf(7, 7), fileSource.saved.single().second)
        assertEquals("Aushang_2026-12-06.pdf", state.savedFile)
        assertEquals(false, state.isBusy)
        assertNull(state.errorMessage)
        assertEquals("Berlin-Mitte", renderer.notice?.congregationName)
        assertEquals(listOf("So 06.12.2026", "So 03.01.2027"), renderer.notice?.rows?.map { it.dateText })
        assertEquals(listOf(2026, 2027), requestedYears.takeLast(2))
        assertNull(renderer.png)
    }

    @Test
    fun `createPdf passes the selected graphic and the color set`() {
        notices.images += NoticeImage("img", "Logo", PNG)
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }
        vm.chooseColorSet(NoticeColorSet.GREEN)
        vm.selectImage("img")

        vm.createPdf()

        assertContentEquals(PNG, renderer.png)
        assertEquals(NoticeColorSet.GREEN, renderer.colors)
    }

    @Test
    fun `the saved file shows the name the user chose`() {
        fileSource.saveResult = Result.success("Mein Aushang.pdf")
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.createPdf()

        assertEquals("Mein Aushang.pdf", vm.uiState.value.savedFile)
    }

    @Test
    fun `cancelling the save dialog is neither saved nor an error`() {
        fileSource.saveResult = Result.success(null)
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.createPdf()

        assertNull(vm.uiState.value.savedFile)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isBusy)
    }

    @Test
    fun `a renderer failure shows a German message`() {
        renderer.result = Result.failure(IllegalStateException("kaputt"))
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.createPdf()

        assertEquals("Der Aushang konnte nicht erstellt werden: kaputt", vm.uiState.value.errorMessage)
        assertTrue(fileSource.saved.isEmpty())
        assertEquals(false, vm.uiState.value.isBusy)
    }

    @Test
    fun `deleting the default graphic clears the selection`() {
        notices.images += NoticeImage("img", "Logo", PNG)
        notices.settings.value = TalkNoticeSettings(defaultImageId = "img")
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.requestDeleteImage(vm.uiState.value.images.single())
        vm.confirmDeleteImage()

        val state = vm.uiState.value
        assertNull(state.selectedImageId)
        assertNull(state.pendingDeleteImage)
        assertTrue(state.images.isEmpty())
        assertNull(state.settings.defaultImageId)
    }

    @Test
    fun `addImage stores the picked file and names a blank one Grafik`() {
        fileSource.picked = Result.success(PickedPng("", PNG))
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.addImage()

        assertEquals(listOf("Grafik"), vm.uiState.value.images.map { it.name })
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `addImage with a cancelled dialog changes nothing`() {
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.addImage()

        assertTrue(vm.uiState.value.images.isEmpty())
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isBusy)
    }

    @Test
    fun `addImage with a too large file shows the use case message`() {
        fileSource.picked = Result.success(PickedPng("Logo", ByteArray(700_001) { PNG[it % PNG.size] }))
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.addImage()

        assertEquals("Die Grafik ist größer als 700 KB.", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.images.isEmpty())
        vm.dismissError()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `renameImage and makeDefault update the state`() {
        notices.images += NoticeImage("img", "Logo", PNG)
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.renameImage("img", "Neu")
        vm.makeDefault("img")

        assertEquals(listOf("Neu"), vm.uiState.value.images.map { it.name })
        assertEquals("img", vm.uiState.value.settings.defaultImageId)
    }

    @Test
    fun `the view model tells whether an image name is valid`() {
        val vm = viewModel()

        assertTrue(vm.isValidImageName(" Saal "))
        assertTrue(!vm.isValidImageName("  "))
        assertTrue(!vm.isValidImageName("a".repeat(61)))
    }

    @Test
    fun `saveTitle stores the trimmed draft and an invalid one is an error`() {
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        vm.editTitle("  Neu  ")
        vm.saveTitle()
        assertEquals("Neu", notices.settings.value.title)

        vm.editTitle(" ")
        vm.saveTitle()
        assertEquals("Die Überschrift muss 1 bis 120 Zeichen haben.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `a load of the previous user does not leak into the new user`() {
        val gate = CompletableDeferred<Unit>()
        val otherCongregation = testCongregation.copy(congregationId = "other")
        val userB = planner.copy(uid = "b", congregation = otherCongregation)
        val slow = object : TalkNoticeRepository by notices {
            override suspend fun loadImages(congregation: CongregationRef): Result<List<NoticeImage>> =
                if (congregation == testCongregation) {
                    gate.await()
                    Result.success(listOf(NoticeImage("a", "Von A", PNG)))
                } else {
                    Result.success(listOf(NoticeImage("b", "Von B", PNG)))
                }
        }
        val vm = viewModel(slow).apply { onCurrentUserChanged(planner, today) }

        vm.onCurrentUserChanged(userB, today)
        gate.complete(Unit)

        assertEquals(listOf("b"), vm.uiState.value.images.map { it.id })
    }

    @Test
    fun `default settings fill the title draft`() {
        val vm = viewModel().apply { onCurrentUserChanged(planner, today) }

        assertEquals(DEFAULT_NOTICE_TITLE, vm.uiState.value.titleDraft)
    }

    @Test
    fun `a failing settings flow shows an error instead of crashing`() {
        val failing = object : TalkNoticeRepository by notices {
            override fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings> =
                flow { throw IllegalStateException("boom") }
        }

        val vm = viewModel(failing).apply { onCurrentUserChanged(planner, today) }

        assertTrue(vm.uiState.value.errorMessage?.contains("boom") == true)
    }

    @Test
    fun `an unavailable renderer loads nothing`() {
        notices.images += NoticeImage("img", "Logo", PNG)

        val vm = viewModel(rendererFake = FakeRenderer(isAvailable = false))
            .apply { onCurrentUserChanged(planner, today) }

        assertEquals(false, vm.uiState.value.isAvailable)
        assertTrue(vm.uiState.value.images.isEmpty())
        assertNull(vm.uiState.value.start)
        assertTrue(requestedYears.isEmpty())
    }

    @Test
    fun `two quick settings changes keep both`() {
        val saved = mutableListOf<TalkNoticeSettings>()
        val stale = object : TalkNoticeRepository by notices {
            override fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings> =
                flowOf(TalkNoticeSettings())

            override suspend fun saveSettings(
                congregation: CongregationRef,
                settings: TalkNoticeSettings,
            ): Result<TalkNoticeSettings> {
                saved += settings
                return Result.success(settings)
            }
        }
        val vm = viewModel(stale).apply { onCurrentUserChanged(planner, today) }

        vm.chooseColorSet(NoticeColorSet.GREEN)
        vm.editTitle("Neu")
        vm.saveTitle()

        assertEquals(NoticeColorSet.GREEN, saved.last().colorSet)
        assertEquals("Neu", saved.last().title)
    }
}
