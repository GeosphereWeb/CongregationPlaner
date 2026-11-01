package de.geosphere.congregationplaner.user.data.mappers

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyRoleMapperTest {

    @Test
    fun `plain member has no roles`() {
        assertEquals(emptySet(), legacyRoles(UserRole.MEMBER, setOf(UserPermission.DUMMY)))
    }

    @Test
    fun `unknown role has no roles`() {
        assertEquals(emptySet(), legacyRoles(UserRole.NONE, emptySet()))
    }

    @Test
    fun `admin role maps to congregation admin`() {
        assertEquals(setOf(CongregationRole.CONGREGATION_ADMIN), legacyRoles(UserRole.CONGREGATION_ADMIN, emptySet()))
    }

    @Test
    fun `plan talks maps to talk planner`() {
        assertEquals(
            setOf(CongregationRole.TALK_PLANNER),
            legacyRoles(UserRole.MEMBER, setOf(UserPermission.PLAN_TALKS)),
        )
    }

    @Test
    fun `view members alone grants no role`() {
        assertEquals(emptySet(), legacyRoles(UserRole.MEMBER, setOf(UserPermission.VIEW_MEMBERS)))
    }

    @Test
    fun `admin who plans talks gets both roles`() {
        assertEquals(
            setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
            legacyRoles(UserRole.CONGREGATION_ADMIN, setOf(UserPermission.PLAN_TALKS)),
        )
    }

    @Test
    fun `withLegacyRoles replaces the stored roles`() {
        val user = AppUser(role = UserRole.CONGREGATION_ADMIN).withLegacyRoles()

        assertEquals(setOf(CongregationRole.CONGREGATION_ADMIN), user.roles)
    }

    @Test
    fun `unknown role names are ignored`() {
        assertEquals(
            setOf(CongregationRole.TASK_PLANNER),
            CongregationRole.fromValues(listOf("TASK_PLANNER", "bogus", null)),
        )
    }
}
