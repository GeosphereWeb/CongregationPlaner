package de.geosphere.congregationplaner.user.data.mappers

import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredRolesTest {

    @Test
    fun `roles are written sorted by name`() {
        assertEquals(
            listOf("CONGREGATION_ADMIN", "TALK_PLANNER"),
            setOf(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN).toStoredRoles(),
        )
    }

    @Test
    fun `no roles are written as an empty list`() {
        assertEquals(emptyList(), emptySet<CongregationRole>().toStoredRoles())
    }
}
