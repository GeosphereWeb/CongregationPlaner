package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.data.mappers.withLegacyRoles
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkSchedulePermissionPolicyTest {
    private val policy = TalkSchedulePermissionPolicy()

    @Test
    fun `global admin may plan talks`() {
        assertTrue(policy.canPlanTalks(AppUser(uid = "1", isGlobalAdmin = true)))
    }

    @Test
    fun `congregation admin plans only with PLAN_TALKS`() {
        val admin = AppUser(uid = "2", approved = true, role = UserRole.CONGREGATION_ADMIN).withLegacyRoles()

        assertFalse(policy.canPlanTalks(admin))
        assertTrue(policy.canPlanTalks(admin.copy(roles = setOf(CongregationRole.TALK_PLANNER))))
    }

    @Test
    fun `approved member with PLAN_TALKS may plan talks`() {
        val planner =
            AppUser(
                uid = "1",
                approved = true,
                permissions = setOf(UserPermission.PLAN_TALKS),
                roles = setOf(CongregationRole.TALK_PLANNER),
            )

        assertTrue(policy.canPlanTalks(planner))
    }

    @Test
    fun `everyone else may not plan talks`() {
        val users = listOf(
            AppUser(uid = "1", approved = true, permissions = setOf(UserPermission.DUMMY)),
            AppUser(
                uid = "2",
                approved = false,
                permissions = setOf(UserPermission.PLAN_TALKS),
                roles = setOf(CongregationRole.TALK_PLANNER),
            ),
            AppUser(uid = "3", approved = false, role = UserRole.CONGREGATION_ADMIN).withLegacyRoles(),
            AppUser(uid = "4", approved = true, role = UserRole.NONE),
        )

        users.forEach { user -> assertFalse(policy.canPlanTalks(user), "canPlanTalks for $user") }
    }

    @Test
    fun `PLAN_TALKS is a known permission value`() {
        assertEquals(setOf(UserPermission.PLAN_TALKS), UserPermission.fromValues(listOf("PLAN_TALKS", "OTHER")))
    }
}
