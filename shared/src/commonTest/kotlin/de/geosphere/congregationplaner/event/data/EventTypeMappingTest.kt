package de.geosphere.congregationplaner.event.data

import congregationplaner.uilib.generated.resources.Res
import congregationplaner.uilib.generated.resources.ic_cp_arena_duo
import congregationplaner.uilib.generated.resources.ic_cp_square
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.presentation.eventTypeIcon
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class EventTypeMappingTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    @Test
    fun `missing fields get defaults`() {
        assertEquals(
            EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo"),
            eventTypeOf("kk", "Kreiskongress", true, "ic_cp_arena_duo"),
        )
        assertEquals(EventType("x", "", false, EventType.DEFAULT_ICON), eventTypeOf("x", null, null, " "))
        assertEquals("${congregation.documentPath}/eventTypes", congregation.eventTypesPath)
    }

    @Test
    fun `the icon name resolves to its drawable and unknown names to the square`() {
        assertEquals(Res.drawable.ic_cp_arena_duo, eventTypeIcon("ic_cp_arena_duo"))
        assertEquals(Res.drawable.ic_cp_square, eventTypeIcon("ic_cp_does_not_exist"))
        assertEquals(Res.drawable.ic_cp_square, eventTypeIcon(null))
    }

    @Test
    fun `unavailable repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableEventTypeRepository("iOS")

        assertFailsWith<UnsupportedOperationException> { repository.observeEventTypes(congregation).first() }
        val save = repository.save(congregation, EventType(name = "A")).exceptionOrNull()
        assertIs<UnsupportedOperationException>(save)
        assertEquals("Firestore is not configured for iOS.", save.message)
        assertIs<UnsupportedOperationException>(repository.delete(congregation, "a").exceptionOrNull())
    }
}
