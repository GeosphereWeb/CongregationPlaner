package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkPlanningSettingsRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TalkPlanningSettingsUseCasesTest {
    private val repository = FakeTalkPlanningSettingsRepository()
    private val policy = TalkSchedulePermissionPolicy()
    private val observe = ObserveTalkPlanningSettingsUseCase(repository, policy)
    private val save = SaveTalkPlanningSettingsUseCase(repository, policy)

    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val helper = planner.copy(uid = "h", roles = setOf(CongregationRole.TALK_HELPER))
    private val member = planner.copy(uid = "m", roles = emptySet())
    private val settings = TalkPlanningSettings(LocalDate(2025, 11, 2), LocalDate(2024, 1, 14))

    @Test
    fun `planner saves the cut-off dates of his congregation`() = runBlocking {
        assertEquals(settings, save(planner, settings).getOrThrow())
        assertEquals(listOf(settings), repository.saved)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `only planners with a congregation save and failures are passed on`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(save(helper, settings).exceptionOrNull())
        assertIs<TalkScheduleException.NotAllowed>(save(planner.copy(congregation = null), settings).exceptionOrNull())
        assertTrue(repository.saved.isEmpty())

        repository.saveResult = Result.failure(IllegalStateException("denied"))
        assertEquals("denied", save(planner, settings).exceptionOrNull()?.message)
    }

    @Test
    fun `planners and helpers see the cut-off dates and other members see nothing`() = runBlocking {
        repository.settings.value = settings

        assertEquals(settings, observe(planner).first())
        assertEquals(settings, observe(helper).first())
        assertTrue(observe(member).toList().isEmpty())
        assertTrue(observe(planner.copy(congregation = null)).toList().isEmpty())
    }
}
