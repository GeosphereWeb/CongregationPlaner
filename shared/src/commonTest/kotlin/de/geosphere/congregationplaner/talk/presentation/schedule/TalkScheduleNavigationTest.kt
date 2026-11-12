package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkScheduleNavigationTest {
    @Test
    fun `the navigation never opens the detail pane but keeps saving and errors`() {
        val slot = TalkSlot(LocalDate(2026, 11, 8), LocalTime(17, 30))
        val state = TalkScheduleUiState(
            year = 2026,
            editor = TalkSlotEditorState(slot, slot),
            isSaving = true,
            errorMessage = "Fehler",
        )

        val navigation = state.navigationState

        assertTrue(state.hasDetail)
        assertFalse(navigation.hasDetail)
        assertTrue(navigation.isSaving)
        assertEquals("Fehler", navigation.errorMessage)
    }
}
