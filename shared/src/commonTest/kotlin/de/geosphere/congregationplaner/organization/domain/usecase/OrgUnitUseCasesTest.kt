package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.organization.domain.permission.OrganizationPermissionPolicy
import de.geosphere.congregationplaner.organization.testing.FakeOrgUnitRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrgUnitUseCasesTest {
    private val admin = AppUser(uid = "a", isGlobalAdmin = true)
    private val member = AppUser(uid = "m", approved = true)
    private val branch = listOf("r1", "s1", "b1")
    private val circuitsPath = OrgLevel.CIRCUIT.collectionPath(branch)

    private val repository = FakeOrgUnitRepository()
    private val save = SaveOrgUnitUseCase(repository, OrganizationPermissionPolicy())
    private val delete = DeleteOrgUnitUseCase(repository, OrganizationPermissionPolicy())

    @Test
    fun `units are sorted by name and congregations are no units`(): Unit = runBlocking {
        repository.units.value = mapOf("region" to listOf(OrgUnit("2", "Europa"), OrgUnit("1", "afrika")))
        val observe = ObserveOrgUnitsUseCase(repository)

        assertEquals(listOf("afrika", "Europa"), observe(OrgLevel.REGION, emptyList()).first().map { it.name })
        assertFailsWith<IllegalArgumentException> { observe(OrgLevel.CONGREGATION, List(4) { "x" }) }
    }

    @Test
    fun `a global admin creates and renames a unit with trimmed input`(): Unit = runBlocking {
        val created = save(
            admin,
            OrgLevel.CIRCUIT,
            branch,
            OrgUnit(name = " Kreis Nord ", overseerName = " Max "),
        ).getOrThrow()

        assertEquals(OrgUnit("new-1", "Kreis Nord", "Max"), created)
        save(admin, OrgLevel.CIRCUIT, branch, created.copy(name = "Kreis Süd")).getOrThrow()
        assertEquals(listOf(OrgUnit("new-1", "Kreis Süd", "Max")), repository.units.value[circuitsPath])
    }

    @Test
    fun `only circuits keep an overseer`(): Unit = runBlocking {
        val region = save(
            admin,
            OrgLevel.REGION,
            emptyList(),
            OrgUnit(name = "EMEA", overseerName = "Max"),
        ).getOrThrow()

        assertEquals("", region.overseerName)
    }

    @Test
    fun `invalid names and missing permissions are rejected`(): Unit = runBlocking {
        assertIs<OrgUnitException.MissingName>(
            save(admin, OrgLevel.REGION, emptyList(), OrgUnit(name = " ")).exceptionOrNull(),
        )
        val tooLong = "x".repeat(OrgUnit.MAX_NAME_LENGTH + 1)
        assertIs<OrgUnitException.NameTooLong>(
            save(admin, OrgLevel.REGION, emptyList(), OrgUnit(name = tooLong)).exceptionOrNull(),
        )
        assertIs<OrgUnitException.NameTooLong>(
            save(admin, OrgLevel.CIRCUIT, branch, OrgUnit(name = "Kreis", overseerName = tooLong)).exceptionOrNull(),
        )
        assertIs<IllegalStateException>(
            save(member, OrgLevel.REGION, emptyList(), OrgUnit(name = "EMEA")).exceptionOrNull(),
        )
        assertTrue(repository.units.value.isEmpty())
    }

    @Test
    fun `only empty units can be deleted and only by a global admin`(): Unit = runBlocking {
        val circuit = OrgUnit("c1", "Kreis Nord")
        repository.units.value = mapOf(
            circuitsPath to listOf(circuit, OrgUnit("c2", "Kreis Süd")),
            "$circuitsPath/c1/congregation" to listOf(OrgUnit("k1", "Berlin")),
        )

        assertIs<OrgUnitException.HasChildren>(delete(admin, OrgLevel.CIRCUIT, branch, circuit).exceptionOrNull())
        assertIs<IllegalStateException>(
            delete(member, OrgLevel.CIRCUIT, branch, OrgUnit("c2", "Kreis Süd")).exceptionOrNull(),
        )
        assertIs<IllegalArgumentException>(
            delete(admin, OrgLevel.CIRCUIT, branch, OrgUnit(name = "Neu")).exceptionOrNull(),
        )

        delete(admin, OrgLevel.CIRCUIT, branch, OrgUnit("c2", "Kreis Süd")).getOrThrow()
        assertEquals(listOf(circuit), repository.units.value[circuitsPath])
    }
}
