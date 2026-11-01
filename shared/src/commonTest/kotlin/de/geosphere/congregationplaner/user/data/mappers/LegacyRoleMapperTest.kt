package de.geosphere.congregationplaner.user.data.mappers

import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyRoleMapperTest {

    @Test
    fun `plain member has no roles`() {
        assertEquals(emptySet(), legacyRoles("MEMBER", listOf("DUMMY")))
    }

    @Test
    fun `unknown or missing role has no roles`() {
        assertEquals(emptySet(), legacyRoles("NONE", emptyList()))
        assertEquals(emptySet(), legacyRoles(null, emptyList()))
    }

    @Test
    fun `admin role maps to congregation admin`() {
        assertEquals(setOf(CongregationRole.CONGREGATION_ADMIN), legacyRoles("CONGREGATION_ADMIN", emptyList()))
    }

    @Test
    fun `plan talks maps to talk planner`() {
        assertEquals(setOf(CongregationRole.TALK_PLANNER), legacyRoles("MEMBER", listOf("PLAN_TALKS")))
    }

    @Test
    fun `view members alone grants no role`() {
        assertEquals(emptySet(), legacyRoles("MEMBER", listOf("VIEW_MEMBERS")))
    }

    @Test
    fun `admin who plans talks gets both roles`() {
        assertEquals(
            setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
            legacyRoles("CONGREGATION_ADMIN", listOf("PLAN_TALKS", null)),
        )
    }

    @Test
    fun `stored roles win over the legacy fields`() {
        assertEquals(
            setOf(CongregationRole.TASK_PLANNER),
            memberRoles(listOf("TASK_PLANNER"), "CONGREGATION_ADMIN", listOf("PLAN_TALKS")),
        )
        assertEquals(emptySet(), memberRoles(emptyList(), "CONGREGATION_ADMIN", listOf("PLAN_TALKS")))
    }

    @Test
    fun `missing stored roles fall back to the legacy fields`() {
        assertEquals(
            setOf(CongregationRole.CONGREGATION_ADMIN),
            memberRoles(null, "CONGREGATION_ADMIN", emptyList()),
        )
    }

    @Test
    fun `unknown stored role names are ignored`() {
        assertEquals(
            setOf(CongregationRole.TASK_PLANNER),
            memberRoles(listOf("TASK_PLANNER", "bogus", null), null, emptyList()),
        )
    }

    @Test
    fun `stored roles are written sorted by name`() {
        assertEquals(
            listOf("CONGREGATION_ADMIN", "TALK_PLANNER"),
            setOf(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN).toStoredRoles(),
        )
    }
}
