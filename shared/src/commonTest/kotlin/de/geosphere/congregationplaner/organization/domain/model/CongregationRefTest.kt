package de.geosphere.congregationplaner.organization.domain.model

import de.geosphere.congregationplaner.organization.data.InMemoryKeyValueStorage
import de.geosphere.congregationplaner.organization.data.KeyValueCongregationSelectionStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CongregationRefTest {
    private val ref = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val path = "region/EMEA/subregion/europe/branch/central-europe/circuit/609a/congregation/berlin"

    @Test
    fun `paths follow the organization hierarchy`() {
        assertEquals(path, ref.documentPath)
        assertEquals("$path/members", ref.membersPath)
        assertEquals("region", OrgLevel.REGION.collectionPath(emptyList()))
        assertEquals("region/EMEA/subregion/europe/branch", OrgLevel.BRANCH.collectionPath(listOf("EMEA", "europe")))
        assertFailsWith<IllegalArgumentException> { OrgLevel.CIRCUIT.collectionPath(listOf("EMEA")) }
        assertEquals(OrgLevel.SUBREGION, OrgLevel.REGION.child)
        assertNull(OrgLevel.CONGREGATION.child)
    }

    @Test
    fun `invite links and plain paths round trip`() {
        assertEquals("congregationplaner://join/$path", ref.toInviteLink())
        assertEquals(ref, CongregationRef.parse(ref.toInviteLink()))
        assertEquals(ref, CongregationRef.parse("  /$path/ "))
    }

    @Test
    fun `malformed input is rejected`() {
        listOf(
            "",
            "congregationplaner://join/",
            "region/EMEA/subregion/europe/branch/central-europe/circuit/609a",
            "region/EMEA/subregion/europe/branch/central-europe/kreis/609a/congregation/berlin",
            "region/EMEA/subregion/europe/branch/central-europe/circuit/609a/congregation/..",
            "region/EMEA/subregion/europe/branch/central-europe/circuit/609a/congregation/ber lin",
            "region//subregion/europe/branch/central-europe/circuit/609a/congregation/berlin",
        ).forEach { input -> assertNull(CongregationRef.parse(input), "parse($input)") }
        assertNull(CongregationRef.fromIds(listOf("EMEA", "europe")))
    }

    @Test
    fun `display names fall back to ids`() {
        assertEquals("berlin", SelectedCongregation(ref, "").displayName)
        assertEquals("Berlin", SelectedCongregation(ref, "Berlin").displayName)
        assertEquals("EMEA", OrgUnit("EMEA").displayName)
    }

    @Test
    fun `selection store saves loads and clears the selection`() {
        val storage = InMemoryKeyValueStorage()
        val store = KeyValueCongregationSelectionStore(storage)
        assertNull(store.load())

        store.save(SelectedCongregation(ref, "Berlin"))
        assertEquals(SelectedCongregation(ref, "Berlin"), KeyValueCongregationSelectionStore(storage).load())

        store.clear()
        assertNull(store.load())

        storage.putString("selected_congregation_path", "kaputt")
        assertNull(store.load())
    }
}
