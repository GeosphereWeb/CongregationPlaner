package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalDirectoryAddActionsTest {
    private val circuit = ExternalCircuit("c1", "Nord")
    private val congregation = ExternalCongregation("g1", "c1", "Musterstadt")
    private val speaker = VisitingSpeaker("s1", "g1", "Beispiel")
    private val addCircuit = AddAction(AddTarget.CIRCUIT)

    private fun actionsFor(editor: ExternalDirectoryEditor?) = ExternalDirectoryUiState(editor = editor).addActions

    @Test
    fun `without a selection only a circuit can be added`() {
        assertEquals(listOf(addCircuit), actionsFor(null))
    }

    @Test
    fun `a saved circuit offers a congregation in it first`() {
        assertEquals(
            listOf(AddAction(AddTarget.CONGREGATION, "c1"), addCircuit),
            actionsFor(ExternalDirectoryEditor.Circuit(circuit, circuit)),
        )
    }

    @Test
    fun `a saved congregation offers a speaker in it first`() {
        assertEquals(
            listOf(AddAction(AddTarget.SPEAKER, "g1"), addCircuit),
            actionsFor(ExternalDirectoryEditor.Congregation(congregation, congregation)),
        )
    }

    @Test
    fun `new entries and speakers only offer a circuit`() {
        assertEquals(listOf(addCircuit), actionsFor(ExternalDirectoryEditor.Circuit(circuit)))
        assertEquals(listOf(addCircuit), actionsFor(ExternalDirectoryEditor.Congregation(congregation)))
        assertEquals(listOf(addCircuit), actionsFor(ExternalDirectoryEditor.Speaker(speaker, "", speaker)))
    }
}
