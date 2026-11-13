package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class LimitOrgUnitInputUseCaseTest {
    @Test
    fun `the id stays and the names are cut to their length`() {
        val original = OrgUnit(id = "c1", name = "Kreis")
        val input = OrgUnit(id = "other", name = "x".repeat(150), overseerName = "y".repeat(150))

        val limited = LimitOrgUnitInputUseCase()(input, original)

        assertEquals("c1", limited.id)
        assertEquals(OrgUnit.MAX_NAME_LENGTH, limited.name.length)
        assertEquals(OrgUnit.MAX_NAME_LENGTH, limited.overseerName.length)
    }
}
