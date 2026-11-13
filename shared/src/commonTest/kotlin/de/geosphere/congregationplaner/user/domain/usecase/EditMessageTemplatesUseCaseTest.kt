package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditMessageTemplatesUseCaseTest {
    private val edit = EditMessageTemplatesUseCase()
    private val three = DEFAULT_MESSAGE_TEMPLATES

    @Test
    fun `add appends a prefilled template until four`() {
        val four = edit(three, MessageTemplateEdit.Add)

        assertEquals(MessageTemplate("Vorlage 4", "Hallo {Vorname}, "), four.last())
        assertFalse(edit.canAdd(four))
        assertEquals(four, edit(four, MessageTemplateEdit.Add))
    }

    @Test
    fun `remove keeps at least one template`() {
        val one = edit(edit(three, MessageTemplateEdit.Remove(0)), MessageTemplateEdit.Remove(0))

        assertEquals(listOf(three[2]), one)
        assertFalse(edit.canRemove(one))
        assertEquals(one, edit(one, MessageTemplateEdit.Remove(0)))
    }

    @Test
    fun `make default moves the template to the top`() {
        assertEquals(listOf(three[2], three[0], three[1]), edit(three, MessageTemplateEdit.MakeDefault(2)))
        assertEquals(three, edit(three, MessageTemplateEdit.MakeDefault(9)))
    }

    @Test
    fun `changes are cut to the limits and validity follows the save rules`() {
        val changed = edit(three, MessageTemplateEdit.Change(0, MessageTemplate("x".repeat(60), "")))

        assertEquals(MessageTemplate.MAX_NAME_LENGTH, changed.first().name.length)
        assertFalse(edit.isValid(changed))
        assertTrue(edit.isValid(three))
    }
}
