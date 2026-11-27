package de.geosphere.congregationplaner.notice.domain.usecase

import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGES
import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGE_BYTES
import de.geosphere.congregationplaner.notice.domain.model.MAX_IMAGE_PIXELS
import de.geosphere.congregationplaner.notice.domain.model.MAX_TITLE_LENGTH
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeException
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.model.isValidImageName
import de.geosphere.congregationplaner.notice.domain.model.pngSize
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first

private fun AppUser.plannerCongregation(policy: TalkSchedulePermissionPolicy): CongregationRef? =
    congregation?.takeIf { policy.canPlanTalks(this) }

private fun AppUser.viewerCongregation(policy: TalkSchedulePermissionPolicy): CongregationRef? =
    congregation?.takeIf { policy.canViewTalkPlanning(this) }

private fun notAllowed() = Result.failure<Nothing>(TalkNoticeException.NotAllowed())

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

/** Adds a PNG image: name 1..60 characters, at most 700 KB and 2000 x 2000 px, valid PNG header, at most 10 images. */
@Inject
class AddNoticeImageUseCase(
    private val repository: TalkNoticeRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, name: String, png: ByteArray): Result<NoticeImage> {
        val congregation = user.plannerCongregation(policy) ?: return notAllowed()
        val trimmed = name.trim()
        val size = pngSize(png)
        val error = when {
            !isValidImageName(trimmed) -> TalkNoticeException.InvalidImageName()
            png.size > MAX_IMAGE_BYTES -> TalkNoticeException.ImageTooLarge()
            size == null -> TalkNoticeException.NotAPng()
            size.first > MAX_IMAGE_PIXELS || size.second > MAX_IMAGE_PIXELS -> TalkNoticeException.ImageTooLargePixels()
            else -> null
        }
        return if (error != null) {
            Result.failure(error)
        } else {
            addIfRoom(congregation, trimmed, png)
        }
    }

    private suspend fun addIfRoom(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage> =
        repository.countImages(congregation).mapCatching { count ->
            if (count >= MAX_IMAGES) throw TalkNoticeException.TooManyImages()
        }.mapCatching {
            repository.addImage(congregation, name, png).getOrThrow()
        }.onFailure { if (it is CancellationException) throw it }
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
        return clearDefault(congregation, id).fold(
            onSuccess = { repository.deleteImage(congregation, id) },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun clearDefault(congregation: CongregationRef, id: String): Result<Unit> {
        val settings = try {
            repository.observeSettings(congregation).first()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            return Result.failure(e)
        }
        return if (settings.defaultImageId == id) {
            repository.saveSettings(congregation, settings.copy(defaultImageId = null)).map { }
        } else {
            Result.success(Unit)
        }
    }
}
