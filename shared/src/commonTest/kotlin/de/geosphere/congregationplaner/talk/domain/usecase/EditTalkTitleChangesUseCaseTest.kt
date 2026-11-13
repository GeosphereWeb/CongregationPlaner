package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EditTalkTitleChangesUseCaseTest {
    private val edit = EditTalkTitleChangesUseCase()
    private val change = TalkTitleChange(LocalDate(2027, 1, 1), "Zweiter")
    private val talk = Talk(12, "Erster", titleChanges = listOf(change))

    @Test
    fun `a new change starts with the title valid on its day`() {
        assertEquals(
            listOf(change, TalkTitleChange(LocalDate(2028, 1, 1), "Zweiter")),
            edit(talk, TalkTitleChangeEdit.Add(LocalDate(2028, 1, 1))),
        )
        assertEquals("Erster", edit(talk, TalkTitleChangeEdit.Add(LocalDate(2026, 1, 1))).last().title)
    }

    @Test
    fun `changes are replaced and removed by position`() {
        val replaced = change.copy(title = "Neu")
        assertEquals(listOf(replaced), edit(talk, TalkTitleChangeEdit.Change(0, replaced)))
        assertEquals(emptyList(), edit(talk, TalkTitleChangeEdit.Remove(0)))
    }

    @Test
    fun `no change beyond the maximum`() {
        val full = (1..Talk.MAX_TITLE_CHANGES).map { TalkTitleChange(LocalDate(2027, 1, it), "T$it") }
        val talkWithAll = talk.copy(titleChanges = full)

        assertFalse(edit.canAdd(full))
        assertEquals(full, edit(talkWithAll, TalkTitleChangeEdit.Add(LocalDate(2030, 1, 1))))
    }
}
