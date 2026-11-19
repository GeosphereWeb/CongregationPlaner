package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class UnavailableSpecialEventRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    @Test
    fun `unavailable special event repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableSpecialEventRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeAll(congregation).first() }
        val save = repository.save(congregation, SpecialEvent(startDate = LocalDate(2027, 1, 1))).exceptionOrNull()
        assertIs<UnsupportedOperationException>(save)
        assertEquals("Firestore is not configured for iOS.", save.message)
        assertIs<UnsupportedOperationException>(repository.delete(congregation, "1").exceptionOrNull())
    }

    @Test
    fun `events without a valid start are skipped and a missing end is the start`() {
        val start = LocalDate(2027, 1, 1)

        assertNull(specialEventOf("1", "T", null, "2027-01-02", listOf("ALL")))
        assertNull(specialEventOf("1", "T", "kein Datum", null, listOf("ALL")))
        assertEquals(SpecialEvent("1", "", start), specialEventOf("1", null, "2027-01-01", "kaputt", listOf("ALL")))
        assertEquals("kk", specialEventOf("1", "T", "2027-01-01", null, listOf("ALL"), "kk")?.eventTypeId)
        assertNull(specialEventOf("1", "T", "2027-01-01", null, listOf("ALL"), " ")?.eventTypeId)
    }
}
