package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
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
    fun `unavailable external speaker repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableExternalSpeakerRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeAll(congregation).first() }
        val save = repository.save(congregation, ExternalSpeaker("", "Extern", "Eva", "Nord")).exceptionOrNull()
        assertIs<UnsupportedOperationException>(save)
        assertEquals("Firestore is not configured for iOS.", save.message)
        assertIs<UnsupportedOperationException>(repository.delete(congregation, "e1").exceptionOrNull())
    }

    @Test
    fun `unavailable talk catalog repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableTalkCatalogRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeTalks().first() }
        assertIs<UnsupportedOperationException>(repository.save(Talk(1, "Titel")).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.delete(1).exceptionOrNull())
    }
}
