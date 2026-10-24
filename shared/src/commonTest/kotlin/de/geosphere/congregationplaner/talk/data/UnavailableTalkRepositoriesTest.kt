package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UnavailableTalkRepositoriesTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val slot = TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30))

    @Test
    fun `unavailable talk schedule repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableTalkScheduleRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeYear(congregation, 2026).first() }
        val save = repository.saveSlot(congregation, slot).exceptionOrNull()
        assertIs<UnsupportedOperationException>(save)
        assertEquals("Firestore is not configured for iOS.", save.message)
        assertIs<UnsupportedOperationException>(repository.deleteSlot(congregation, slot.date).exceptionOrNull())
    }

    @Test
    fun `unavailable week event repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableWeekEventRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeWeekEvents().first() }
    }

    @Test
    fun `unavailable talk catalog repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableTalkCatalogRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeTalks().first() }
    }
}
