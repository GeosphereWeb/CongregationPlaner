package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkSchedulePermissionPolicyTest {
    private val policy = TalkSchedulePermissionPolicy()

    @Test
    fun `global admin may plan talks`() {
        assertTrue(policy.canPlanTalks(AppUser(uid = "1", isGlobalAdmin = true)))
    }

    @Test
    fun `congregation admin plans only as talk planner`() {
        val admin = AppUser(uid = "2", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))

        assertFalse(policy.canPlanTalks(admin))
        assertTrue(policy.canPlanTalks(admin.copy(roles = admin.roles + CongregationRole.TALK_PLANNER)))
    }

    @Test
    fun `approved talk planner may plan talks`() {
        val planner = AppUser(uid = "1", approved = true, roles = setOf(CongregationRole.TALK_PLANNER))

        assertTrue(policy.canPlanTalks(planner))
    }

    @Test
    fun `everyone else may not plan talks`() {
        val users = listOf(
            AppUser(uid = "1", approved = true),
            AppUser(uid = "2", approved = false, roles = setOf(CongregationRole.TALK_PLANNER)),
            AppUser(uid = "3", approved = false, roles = setOf(CongregationRole.CONGREGATION_ADMIN)),
            AppUser(uid = "4", approved = true, roles = setOf(CongregationRole.TASK_PLANNER)),
        )

        users.forEach { user -> assertFalse(policy.canPlanTalks(user), "canPlanTalks for $user") }
    }
}
