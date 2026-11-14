package de.geosphere.congregationplaner.event.domain

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.model.SpecialEventException
import de.geosphere.congregationplaner.event.domain.model.eventGroups
import de.geosphere.congregationplaner.event.domain.permission.SpecialEventPolicy
import de.geosphere.congregationplaner.event.domain.usecase.DeleteSpecialEventUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveManagedEventsUseCase
import de.geosphere.congregationplaner.event.domain.usecase.SaveSpecialEventUseCase
import de.geosphere.congregationplaner.event.testing.FakeSpecialEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.Gender
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpecialEventUseCasesTest {
    private val policy = SpecialEventPolicy()
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )
    private val planner = admin.copy(roles = setOf(CongregationRole.TALK_PLANNER))
    private val congress = SpecialEvent("1", "Kongress", LocalDate(2027, 6, 11), LocalDate(2027, 6, 13))
    private val visit = SpecialEvent("2", "Besuch", LocalDate(2027, 3, 2))

    @Test
    fun `the groups of a member follow from gender baptism and appointment`() {
        val brother = MemberProfile(gender = Gender.MALE, baptized = true)

        assertEquals(setOf(EventAudience.ALL), MemberProfile().eventGroups())
        assertEquals(setOf(EventAudience.ALL), MemberProfile(gender = Gender.MALE).eventGroups())
        assertEquals(setOf(EventAudience.ALL, EventAudience.BROTHERS), brother.eventGroups())
        assertEquals(
            setOf(EventAudience.ALL, EventAudience.SISTERS),
            MemberProfile(gender = Gender.FEMALE, baptized = true).eventGroups(),
        )
        assertEquals(
            setOf(EventAudience.ALL, EventAudience.BROTHERS, EventAudience.ELDERS),
            brother.copy(appointment = Appointment.ELDER).eventGroups(),
        )
        assertEquals(
            setOf(EventAudience.ALL, EventAudience.BROTHERS, EventAudience.MINISTERIAL_SERVANTS),
            brother.copy(appointment = Appointment.MINISTERIAL_SERVANT).eventGroups(),
        )
    }

    @Test
    fun `the admin sees all events sorted by start and others see none`() = runBlocking {
        val repository = FakeSpecialEventRepository(listOf(congress, visit))
        val observe = ObserveManagedEventsUseCase(repository, policy)

        assertEquals(listOf(visit, congress), observe(admin).first())
        assertTrue(observe(planner).first().isEmpty())
        assertTrue(observe(admin.copy(congregation = null)).first().isEmpty())
    }

    @Test
    fun `saving trims the title and keeps only all when all is chosen`() = runBlocking {
        val repository = FakeSpecialEventRepository()
        val save = SaveSpecialEventUseCase(repository, policy)
        val event = visit.copy(id = "", title = " Besuch ", audience = setOf(EventAudience.ALL, EventAudience.ELDERS))

        val saved = save(admin, event).getOrThrow()

        assertEquals("Besuch", saved.title)
        assertEquals(setOf(EventAudience.ALL), saved.audience)
        assertEquals(listOf(saved), repository.savedEvents)
    }

    @Test
    fun `saving rejects invalid events and users without the admin role`(): Unit = runBlocking {
        val repository = FakeSpecialEventRepository()
        val save = SaveSpecialEventUseCase(repository, policy)

        assertIs<SpecialEventException.NotAllowed>(save(planner, congress).exceptionOrNull())
        assertIs<SpecialEventException.NotAllowed>(save(admin.copy(congregation = null), congress).exceptionOrNull())
        assertIs<SpecialEventException.InvalidTitle>(save(admin, congress.copy(title = " ")).exceptionOrNull())
        assertIs<SpecialEventException.InvalidTitle>(
            save(admin, congress.copy(title = "x".repeat(SpecialEvent.MAX_TITLE_LENGTH + 1))).exceptionOrNull(),
        )
        assertIs<SpecialEventException.InvalidDates>(
            save(admin, congress.copy(endDate = LocalDate(2027, 6, 10))).exceptionOrNull(),
        )
        assertIs<SpecialEventException.MissingAudience>(
            save(admin, congress.copy(audience = emptySet())).exceptionOrNull(),
        )
        assertTrue(repository.savedEvents.isEmpty())
    }

    @Test
    fun `only the admin deletes and the id is required`(): Unit = runBlocking {
        val repository = FakeSpecialEventRepository(listOf(congress))
        val delete = DeleteSpecialEventUseCase(repository, policy)

        assertIs<SpecialEventException.NotAllowed>(delete(planner, "1").exceptionOrNull())
        assertIs<IllegalArgumentException>(delete(admin, " ").exceptionOrNull())
        delete(admin, "1").getOrThrow()
        assertEquals(listOf("1"), repository.deletedIds)
    }
}
