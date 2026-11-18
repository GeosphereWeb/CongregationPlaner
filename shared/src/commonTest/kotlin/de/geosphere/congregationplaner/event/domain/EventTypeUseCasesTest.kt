package de.geosphere.congregationplaner.event.domain

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.model.EventTypeException
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.usecase.AdoptEventTypeBlueprintUseCase
import de.geosphere.congregationplaner.event.domain.usecase.DeleteEventTypeUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveEventTypesUseCase
import de.geosphere.congregationplaner.event.domain.usecase.SaveEventTypeUseCase
import de.geosphere.congregationplaner.event.domain.usecase.missingFromBlueprint
import de.geosphere.congregationplaner.event.testing.FakeEventTypeRepository
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EventTypeUseCasesTest {
    private val policy = EventTypePolicy()
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )
    private val planner = admin.copy(uid = "planner", roles = setOf(CongregationRole.TALK_PLANNER))
    private val member = admin.copy(uid = "member", roles = emptySet())
    private val congress = EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo")
    private val week = EventType("dw", "Dienstwoche")
    private val repository = FakeEventTypeRepository(listOf(week, congress))

    @Test
    fun `members see the types sorted by name and others see nothing`() = runBlocking {
        val observe = ObserveEventTypesUseCase(repository, policy)

        assertEquals(listOf(week, congress), observe(member).first())
        assertEquals(emptyList(), observe(member.copy(approved = false)).first())
        assertEquals(emptyList(), observe(member.copy(congregation = null)).first())
        assertEquals(emptyList(), observe(member.copy(isMember = false)).first())
    }

    @Test
    fun `saving trims the name and checks name icon and permission`() = runBlocking {
        val save = SaveEventTypeUseCase(repository, policy)

        val saved = save(admin, EventType(name = "  Gedächtnismahl ", icon = "ic_cp_wine_bread_duo")).getOrThrow()
        assertEquals(EventType("type-1", "Gedächtnismahl", icon = "ic_cp_wine_bread_duo"), saved)

        assertIs<EventTypeException.InvalidName>(save(admin, EventType(name = " ")).exceptionOrNull())
        assertIs<EventTypeException.InvalidName>(save(admin, EventType(name = "x".repeat(101))).exceptionOrNull())
        assertIs<EventTypeException.InvalidIcon>(
            save(admin, EventType(name = "A", icon = "ic_launcher")).exceptionOrNull(),
        )
        assertIs<EventTypeException.NotAllowed>(save(member, EventType(name = "A")).exceptionOrNull())
        assertEquals(1, repository.savedTypes.size)
    }

    @Test
    fun `the talk planner may create but not change a type`() = runBlocking {
        val save = SaveEventTypeUseCase(repository, policy)

        assertTrue(save(planner, EventType(name = "Zweigbesuch")).isSuccess)
        assertIs<EventTypeException.NotAllowed>(save(planner, congress.copy(name = "Anders")).exceptionOrNull())
        assertTrue(save(admin, congress.copy(name = "Anders")).isSuccess)
    }

    @Test
    fun `only the admin deletes`() = runBlocking {
        val delete = DeleteEventTypeUseCase(repository, policy)

        assertIs<EventTypeException.NotAllowed>(delete(planner, "kk").exceptionOrNull())
        assertTrue(delete(admin, "").isFailure)
        assertTrue(delete(admin, "kk").isSuccess)
        assertEquals(listOf("kk"), repository.deletedIds)
    }

    @Test
    fun `the blueprint adds only missing types and can be adopted again`() = runBlocking {
        val blueprint = FakeWeekEventRepository(
            listOf(WeekEvent("c1", "kreiskongress", cancelsTalk = true), WeekEvent("c2", "Zweigbesuch", true)),
        )
        val adopt = AdoptEventTypeBlueprintUseCase(repository, blueprint, policy)

        assertIs<EventTypeException.NotAllowed>(adopt(planner).exceptionOrNull())
        assertEquals(1, adopt(admin).getOrThrow())
        assertEquals(listOf(EventType("type-1", "Zweigbesuch", cancelsTalk = true)), repository.savedTypes)
        assertEquals(listOf(testCongregation.country), blueprint.countries)
        assertEquals(0, adopt(admin).getOrThrow())
    }

    @Test
    fun `a failed save is reported`() = runBlocking {
        repository.saveResult = Result.failure(IllegalStateException("offline"))
        val adopt =
            AdoptEventTypeBlueprintUseCase(repository, FakeWeekEventRepository(listOf(WeekEvent("c", "Neu"))), policy)

        assertEquals("offline", adopt(admin).exceptionOrNull()?.message)
    }

    @Test
    fun `missing blueprint entries ignore case blanks and duplicates`() {
        val blueprint = listOf(
            WeekEvent("1", " Dienstwoche "),
            WeekEvent("2", "Regionaler Kongress", cancelsTalk = true),
            WeekEvent("3", "regionaler kongress"),
            WeekEvent("4", " "),
        )

        assertEquals(
            listOf(EventType(name = "Regionaler Kongress", cancelsTalk = true)),
            missingFromBlueprint(blueprint, listOf(week)),
        )
    }
}
