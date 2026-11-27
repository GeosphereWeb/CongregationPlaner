package de.geosphere.congregationplaner.notice.domain.usecase

import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGES
import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGE_BYTES
import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGE_NAME_LENGTH
import de.geosphere.congregationplaner.notice.domain.model.MAX_TITLE_LENGTH
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeException
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

private fun ByteArray.isPng() = size >= PNG_SIGNATURE.size && copyOf(PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)

private fun AppUser.plannerCongregation(policy: TalkSchedulePermissionPolicy): CongregationRef? =
    congregation?.takeIf { policy.canPlanTalks(this) }

private fun AppUser.viewerCongregation(policy: TalkSchedulePermissionPolicy): CongregationRef? =
    congregation?.takeIf { policy.canViewTalkPlanning(this) }

private fun notAllowed() = Result.failure<Nothing>(TalkNoticeException.NotAllowed())

private fun isValidImageName(name: String) = name.isNotEmpty() && name.length <= MAX_IMAGE_NAME_LENGTH

/** The notice settings of the user's congregation; nothing without a congregation or insight into the planning. */
@Inject
class ObserveTalkNoticeSettingsUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    operator fun invoke(user: AppUser): Flow<TalkNoticeSettings> =
        user.viewerCongregation(policy)?.let(repository::observeSettings) ?: emptyFlow()
}

/** Saves the notice settings; only planners may; the title is trimmed and must be 1..120 characters. */
@Inject
class SaveTalkNoticeSettingsUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, settings: TalkNoticeSettings): Result<TalkNoticeSettings> {
        val congregation = user.plannerCongregation(policy) ?: return notAllowed()
        val title = settings.title.trim()
        if (title.isEmpty() || title.length > MAX_TITLE_LENGTH) {
            return Result.failure(TalkNoticeException.InvalidTitle())
        }
        return repository.saveSettings(congregation, settings.copy(title = title))
    }
}

/** Loads the images of the congregation sorted by name. */
@Inject
class LoadNoticeImagesUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser): Result<List<NoticeImage>> {
        val congregation = user.viewerCongregation(policy) ?: return notAllowed()
        return repository.loadImages(congregation).map { images -> images.sortedBy { it.name.lowercase() } }
    }
}

/** Adds a PNG image: name 1..60 characters, at most 700 KB, PNG signature, at most 10 images. */
@Inject
class AddNoticeImageUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, name: String, png: ByteArray): Result<NoticeImage> {
        val congregation = user.plannerCongregation(policy) ?: return notAllowed()
        val trimmed = name.trim()
        val error = when {
            !isValidImageName(trimmed) -> TalkNoticeException.InvalidImageName()
            png.size > MAX_IMAGE_BYTES -> TalkNoticeException.ImageTooLarge()
            !png.isPng() -> TalkNoticeException.NotAPng()
            else -> null
        }
        return if (error != null) {
            Result.failure(error)
        } else {
            addIfRoom(congregation, trimmed, png)
        }
    }

    private suspend fun addIfRoom(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage> =
        repository.loadImages(congregation).mapCatching { images ->
            if (images.size >= MAX_IMAGES) throw TalkNoticeException.TooManyImages()
        }.mapCatching { repository.addImage(congregation, name, png).getOrThrow() }
}

@Inject
class RenameNoticeImageUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String, name: String): Result<Unit> {
        val congregation = user.plannerCongregation(policy) ?: return notAllowed()
        val trimmed = name.trim()
        if (!isValidImageName(trimmed)) return Result.failure(TalkNoticeException.InvalidImageName())
        return repository.renameImage(congregation, id, trimmed)
    }
}

/** Deletes an image; if it is the default image, the default is cleared first. */
@Inject
class DeleteNoticeImageUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.plannerCongregation(policy) ?: return notAllowed()
        val settings = repository.observeSettings(congregation).first()
        if (settings.defaultImageId == id) {
            repository.saveSettings(congregation, settings.copy(defaultImageId = null))
                .onFailure { return Result.failure(it) }
        }
        return repository.deleteImage(congregation, id)
    }
}
