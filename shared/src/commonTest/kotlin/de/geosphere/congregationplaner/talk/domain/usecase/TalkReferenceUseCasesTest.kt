package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TalkReferenceUseCasesTest {
    private val policy = TalkReferenceDataPolicy()
    private val admin = AppUser(uid = "admin", isGlobalAdmin = true)
    private val congregationAdmin = AppUser(uid = "ca", approved = true, role = UserRole.CONGREGATION_ADMIN)

    private val catalog = FakeTalkCatalogRepository()
    private val saveTalk = SaveTalkUseCase(catalog, policy)
    private val deleteTalk = DeleteTalkUseCase(catalog, policy)

    private val events = FakeWeekEventRepository()
    private val saveEvent = SaveWeekEventUseCase(events, policy)
    private val deleteEvent = DeleteWeekEventUseCase(events, policy)

    @Test
    fun `global admin saves a talk with a trimmed title`() = runBlocking {
        val saved = saveTalk(admin, Talk(12, "  Der Weg zum Glück  ", active = false)).getOrThrow()

        assertEquals(Talk(12, "Der Weg zum Glück", active = false), saved)
        assertEquals(listOf(saved), catalog.savedTalks)
    }

    @Test
    fun `talks are only saved with a valid number and title`() = runBlocking {
        val invalid = listOf(
            Talk(0, "Titel"),
            Talk(Talk.MAX_NUMBER + 1, "Titel"),
            Talk(-3, "Titel"),
            Talk(5, "  "),
            Talk(5, "x".repeat(TalkSlot.MAX_TALK_TITLE_LENGTH + 1)),
        )

        invalid.forEach { assertIs<TalkReferenceException.InvalidTalk>(saveTalk(admin, it).exceptionOrNull()) }
        assertTrue(catalog.savedTalks.isEmpty())
        assertTrue(saveTalk(admin, Talk(Talk.MAX_NUMBER, "x".repeat(TalkSlot.MAX_TALK_TITLE_LENGTH))).isSuccess)
    }

    @Test
    fun `only global admins may save or delete talks`() = runBlocking {
        assertIs<TalkReferenceException.NotAllowed>(saveTalk(congregationAdmin, Talk(1, "Titel")).exceptionOrNull())
        assertIs<TalkReferenceException.NotAllowed>(deleteTalk(congregationAdmin, 1).exceptionOrNull())
        assertTrue(catalog.savedTalks.isEmpty())
        assertTrue(catalog.deletedNumbers.isEmpty())
    }

    @Test
    fun `deleting a talk validates the number and reports repository failures`() = runBlocking {
        assertIs<TalkReferenceException.InvalidTalk>(deleteTalk(admin, 0).exceptionOrNull())
        assertIs<TalkReferenceException.InvalidTalk>(deleteTalk(admin, Talk.MAX_NUMBER + 1).exceptionOrNull())

        assertTrue(deleteTalk(admin, 12).isSuccess)
        assertEquals(listOf(12), catalog.deletedNumbers)

        catalog.deleteResult = Result.failure(IllegalStateException("offline"))
        assertEquals("offline", deleteTalk(admin, 12).exceptionOrNull()?.message)
        catalog.saveResult = Result.failure(IllegalStateException("denied"))
        assertEquals("denied", saveTalk(admin, Talk(1, "Titel")).exceptionOrNull()?.message)
    }

    @Test
    fun `week events are saved with a trimmed name and a generated id`() = runBlocking {
        val saved = saveEvent(admin, WeekEvent("", "  Kongress  ", cancelsTalk = true)).getOrThrow()

        assertEquals(WeekEvent("ev-1", "Kongress", cancelsTalk = true), saved)
        assertEquals(listOf(saved), events.savedEvents)
    }

    @Test
    fun `week events need a name of valid length and a global admin`() = runBlocking {
        assertIs<TalkReferenceException.InvalidWeekEvent>(saveEvent(admin, WeekEvent("", " ")).exceptionOrNull())
        assertIs<TalkReferenceException.InvalidWeekEvent>(
            saveEvent(admin, WeekEvent("", "x".repeat(TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH + 1))).exceptionOrNull(),
        )
        assertIs<TalkReferenceException.NotAllowed>(
            saveEvent(congregationAdmin, WeekEvent("", "Kongress")).exceptionOrNull(),
        )
        assertIs<TalkReferenceException.NotAllowed>(deleteEvent(congregationAdmin, "ev-1").exceptionOrNull())
        assertTrue(events.savedEvents.isEmpty())
    }

    @Test
    fun `deleting a week event validates the id`() = runBlocking {
        assertIs<TalkReferenceException.InvalidWeekEvent>(deleteEvent(admin, " ").exceptionOrNull())

        assertTrue(deleteEvent(admin, "ev-1").isSuccess)
        assertEquals(listOf("ev-1"), events.deletedIds)
    }
}
