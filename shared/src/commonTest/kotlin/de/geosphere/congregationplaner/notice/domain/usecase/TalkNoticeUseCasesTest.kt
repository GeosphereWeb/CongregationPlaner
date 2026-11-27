package de.geosphere.congregationplaner.notice.domain.usecase

import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeException
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FakeTalkNoticeRepository : TalkNoticeRepository {
    val settings = MutableStateFlow(TalkNoticeSettings())
    val images = mutableListOf<NoticeImage>()
    private var nextId = 1

    override fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings> = settings

    override suspend fun saveSettings(
        congregation: CongregationRef,
        settings: TalkNoticeSettings,
    ): Result<TalkNoticeSettings> {
        this.settings.value = settings
        return Result.success(settings)
    }

    override suspend fun loadImages(congregation: CongregationRef): Result<List<NoticeImage>> =
        Result.success(images.toList())

    override suspend fun addImage(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage> =
        Result.success(NoticeImage("img${nextId++}", name, png).also { images += it })

    override suspend fun renameImage(congregation: CongregationRef, id: String, name: String): Result<Unit> {
        val index = images.indexOfFirst { it.id == id }
        images[index] = NoticeImage(id, name, images[index].png)
        return Result.success(Unit)
    }

    override suspend fun deleteImage(congregation: CongregationRef, id: String): Result<Unit> {
        images.removeAll { it.id == id }
        return Result.success(Unit)
    }
}

class TalkNoticeUseCasesTest {
    private val repository = FakeTalkNoticeRepository()
    private val policy = TalkSchedulePermissionPolicy()
    private val observe = ObserveTalkNoticeSettingsUseCase(repository, policy)
    private val save = SaveTalkNoticeSettingsUseCase(repository, policy)
    private val load = LoadNoticeImagesUseCase(repository, policy)
    private val add = AddNoticeImageUseCase(repository, policy)
    private val rename = RenameNoticeImageUseCase(repository, policy)
    private val delete = DeleteNoticeImageUseCase(repository, policy)

    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val helper = planner.copy(uid = "h", roles = setOf(CongregationRole.TALK_HELPER))
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2)

    @Test
    fun `helpers may only observe settings and load images`() = runBlocking {
        assertEquals(TalkNoticeSettings(), observe(helper).first())
        assertTrue(load(helper).isSuccess)
        assertIs<TalkNoticeException.NotAllowed>(save(helper, TalkNoticeSettings()).exceptionOrNull())
        assertIs<TalkNoticeException.NotAllowed>(add(helper, "a", png).exceptionOrNull())
        assertIs<TalkNoticeException.NotAllowed>(rename(helper, "x", "a").exceptionOrNull())
        assertIs<TalkNoticeException.NotAllowed>(delete(helper, "x").exceptionOrNull())
        assertTrue(observe(planner.copy(roles = emptySet())).toList().isEmpty())
        assertTrue(load(planner.copy(congregation = null)).exceptionOrNull() is TalkNoticeException.NotAllowed)
    }

    @Test
    fun `invalid titles are rejected and the title is trimmed`() = runBlocking {
        assertIs<TalkNoticeException.InvalidTitle>(save(planner, TalkNoticeSettings(title = " ")).exceptionOrNull())
        val tooLong = TalkNoticeSettings(title = "a".repeat(121))
        assertIs<TalkNoticeException.InvalidTitle>(save(planner, tooLong).exceptionOrNull())
        assertEquals("Titel", save(planner, TalkNoticeSettings(title = "  Titel ")).getOrThrow().title)
        assertEquals("Titel", repository.settings.value.title)
    }

    @Test
    fun `image validation covers size signature name and count`() = runBlocking {
        val big = png + ByteArray(700_000)
        assertIs<TalkNoticeException.ImageTooLarge>(add(planner, "a", big).exceptionOrNull())
        assertIs<TalkNoticeException.NotAPng>(add(planner, "a", byteArrayOf(1, 2, 3)).exceptionOrNull())
        assertIs<TalkNoticeException.InvalidImageName>(add(planner, "", png).exceptionOrNull())
        assertIs<TalkNoticeException.InvalidImageName>(add(planner, "a".repeat(61), png).exceptionOrNull())

        repeat(10) { assertTrue(add(planner, "img $it", png).isSuccess) }
        assertIs<TalkNoticeException.TooManyImages>(add(planner, "eleven", png).exceptionOrNull())
        assertEquals(10, repository.images.size)
    }

    @Test
    fun `images load sorted by name and can be renamed`() = runBlocking {
        add(planner, "Zebra", png)
        val b = add(planner, "apfel", png).getOrThrow()
        assertEquals(listOf("apfel", "Zebra"), load(planner).getOrThrow().map { it.name })

        assertIs<TalkNoticeException.InvalidImageName>(rename(planner, b.id, " ").exceptionOrNull())
        assertTrue(rename(planner, b.id, " Zora ").isSuccess)
        assertEquals(listOf("Zebra", "Zora"), load(planner).getOrThrow().map { it.name })
    }

    @Test
    fun `deleting the default image clears the default`() = runBlocking {
        val image = add(planner, "a", png).getOrThrow()
        repository.settings.value = TalkNoticeSettings(defaultImageId = image.id)

        assertTrue(delete(planner, image.id).isSuccess)

        assertNull(repository.settings.value.defaultImageId)
        assertTrue(repository.images.isEmpty())
    }

    @Test
    fun `deleting another image keeps the default`() = runBlocking {
        val keep = add(planner, "a", png).getOrThrow()
        val other = add(planner, "b", png).getOrThrow()
        repository.settings.value = TalkNoticeSettings(defaultImageId = keep.id)

        assertTrue(delete(planner, other.id).isSuccess)

        assertEquals(keep.id, repository.settings.value.defaultImageId)
        assertEquals(listOf(keep), repository.images)
    }
}
