package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TalkReferenceUseCasesTest {
    private val policy = TalkReferenceDataPolicy()
    private val admin = AppUser(uid = "admin", isGlobalAdmin = true)
    private val congregationAdmin =
        AppUser(uid = "ca", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))

    private val country = CountryRef("EMEA", "europe", "central-europe", "de")

    private val catalog = FakeTalkCatalogRepository()
    private val saveTalk = SaveTalkUseCase(catalog, policy)
    private val deleteTalk = DeleteTalkUseCase(catalog, policy)

    private val events = FakeWeekEventRepository()
    private val saveEvent = SaveWeekEventUseCase(events, policy)
    private val deleteEvent = DeleteWeekEventUseCase(events, policy)

    @Test
    fun `global admin saves a talk with a trimmed title`() = runBlocking {
        val saved = saveTalk(admin, country, Talk(12, "  Der Weg zum Glück  ", active = false)).getOrThrow()

        assertEquals(Talk(12, "Der Weg zum Glück", active = false), saved)
        assertEquals(listOf(saved), catalog.savedTalks)
        assertEquals(listOf(country), catalog.countries)
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

        invalid.forEach { assertIs<TalkReferenceException.InvalidTalk>(saveTalk(admin, country, it).exceptionOrNull()) }
        assertTrue(catalog.savedTalks.isEmpty())
        assertTrue(
            saveTalk(admin, country, Talk(Talk.MAX_NUMBER, "x".repeat(TalkSlot.MAX_TALK_TITLE_LENGTH))).isSuccess,
        )
    }

    @Test
    fun `title changes are trimmed and sorted and must be valid`() = runBlocking {
        val later = TalkTitleChange(LocalDate(2028, 1, 1), " Dritter ")
        val earlier = TalkTitleChange(LocalDate(2027, 1, 1), "Zweiter")

        val saved = saveTalk(admin, country, Talk(12, "Erster", titleChanges = listOf(later, earlier))).getOrThrow()

        assertEquals(listOf(earlier, later.copy(title = "Dritter")), saved.titleChanges)
        val sameDay = listOf(earlier, earlier.copy(title = "Anders"))
        val tooMany = (1..Talk.MAX_TITLE_CHANGES + 1).map { TalkTitleChange(LocalDate(2027, 1, it), "T$it") }
        listOf(sameDay, tooMany, listOf(earlier.copy(title = " "))).forEach { changes ->
            assertIs<TalkReferenceException.InvalidTalk>(
                saveTalk(admin, country, Talk(12, "Erster", titleChanges = changes)).exceptionOrNull(),
            )
        }
    }

    @Test
    fun `only global admins may save or delete talks`() = runBlocking {
        assertIs<TalkReferenceException.NotAllowed>(
            saveTalk(congregationAdmin, country, Talk(1, "Titel")).exceptionOrNull(),
        )
        assertIs<TalkReferenceException.NotAllowed>(deleteTalk(congregationAdmin, country, 1).exceptionOrNull())
        assertTrue(catalog.savedTalks.isEmpty())
        assertTrue(catalog.deletedNumbers.isEmpty())
    }

    @Test
    fun `deleting a talk validates the number and reports repository failures`() = runBlocking {
        assertIs<TalkReferenceException.InvalidTalk>(deleteTalk(admin, country, 0).exceptionOrNull())
        assertIs<TalkReferenceException.InvalidTalk>(deleteTalk(admin, country, Talk.MAX_NUMBER + 1).exceptionOrNull())

        assertTrue(deleteTalk(admin, country, 12).isSuccess)
        assertEquals(listOf(12), catalog.deletedNumbers)

        catalog.deleteResult = Result.failure(IllegalStateException("offline"))
        assertEquals("offline", deleteTalk(admin, country, 12).exceptionOrNull()?.message)
        catalog.saveResult = Result.failure(IllegalStateException("denied"))
        assertEquals("denied", saveTalk(admin, country, Talk(1, "Titel")).exceptionOrNull()?.message)
    }

    @Test
    fun `week events are saved with a trimmed name and a generated id`() = runBlocking {
        val saved = saveEvent(admin, country, WeekEvent("", "  Kongress  ", cancelsTalk = true)).getOrThrow()

        assertEquals(WeekEvent("ev-1", "Kongress", cancelsTalk = true), saved)
        assertEquals(listOf(saved), events.savedEvents)
        assertEquals(listOf(country), events.countries)
    }

    @Test
    fun `week events need a name of valid length and a global admin`() = runBlocking {
        assertIs<TalkReferenceException.InvalidWeekEvent>(
            saveEvent(admin, country, WeekEvent("", " ")).exceptionOrNull(),
        )
        assertIs<TalkReferenceException.InvalidWeekEvent>(
            saveEvent(admin, country, WeekEvent("", "x".repeat(TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH + 1)))
                .exceptionOrNull(),
        )
        assertIs<TalkReferenceException.NotAllowed>(
            saveEvent(congregationAdmin, country, WeekEvent("", "Kongress")).exceptionOrNull(),
        )
        assertIs<TalkReferenceException.NotAllowed>(deleteEvent(congregationAdmin, country, "ev-1").exceptionOrNull())
        assertTrue(events.savedEvents.isEmpty())
    }

    @Test
    fun `deleting a week event validates the id`() = runBlocking {
        assertIs<TalkReferenceException.InvalidWeekEvent>(deleteEvent(admin, country, " ").exceptionOrNull())

        assertTrue(deleteEvent(admin, country, "ev-1").isSuccess)
        assertEquals(listOf("ev-1"), events.deletedIds)
    }
}
