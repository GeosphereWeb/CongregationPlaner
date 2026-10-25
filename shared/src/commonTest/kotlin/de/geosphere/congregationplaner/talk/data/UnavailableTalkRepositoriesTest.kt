package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
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
        assertIs<UnsupportedOperationException>(repository.save(WeekEvent("", "Kongress")).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.delete("ev-1").exceptionOrNull())
    }

    @Test
    fun `unavailable external speaker directory repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableExternalSpeakerDirectoryRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeCircuits(congregation).first() }
        assertFailsWith<UnsupportedOperationException> { repository.observeCongregations(congregation).first() }
        assertFailsWith<UnsupportedOperationException> { repository.observeSpeakers(congregation).first() }
        val save = repository.saveSpeaker(congregation, VisitingSpeaker("", "g1", "Extern")).exceptionOrNull()
        assertIs<UnsupportedOperationException>(save)
        assertEquals("Firestore is not configured for iOS.", save.message)
        assertIs<UnsupportedOperationException>(
            repository.saveCircuit(congregation, ExternalCircuit("", "Nord")).exceptionOrNull(),
        )
        assertIs<UnsupportedOperationException>(
            repository.saveCongregation(congregation, ExternalCongregation("", "c1", "Süd")).exceptionOrNull(),
        )
        assertIs<UnsupportedOperationException>(repository.deleteCircuit(congregation, "c1").exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.deleteCongregation(congregation, "g1").exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.deleteSpeaker(congregation, "s1").exceptionOrNull())
    }

    @Test
    fun `unavailable talk history repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableTalkHistoryRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeAll(congregation).first() }
        val entry = TalkHistoryEntry(1, listOf(LocalDate(2026, 3, 1)))
        assertIs<UnsupportedOperationException>(repository.save(congregation, entry).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.delete(congregation, 1).exceptionOrNull())
    }

    @Test
    fun `unavailable talk catalog repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableTalkCatalogRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeTalks().first() }
        assertIs<UnsupportedOperationException>(repository.save(Talk(1, "Titel")).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.delete(1).exceptionOrNull())
    }
}
