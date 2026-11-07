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

    private fun actionFor(editor: ExternalDirectoryEditor?) = ExternalDirectoryUiState(editor = editor).addAction

    @Test
    fun `without a selection a circuit is added`() {
        assertEquals(addCircuit, actionFor(null))
    }

    @Test
    fun `a saved circuit only adds a congregation in it`() {
        assertEquals(
            AddAction(AddTarget.CONGREGATION, "c1"),
            actionFor(ExternalDirectoryEditor.Circuit(circuit, circuit)),
        )
    }

    @Test
    fun `a saved congregation only adds a speaker in it`() {
        assertEquals(
            AddAction(AddTarget.SPEAKER, "g1"),
            actionFor(ExternalDirectoryEditor.Congregation(congregation, congregation)),
        )
    }

    @Test
    fun `new entries and speakers add a circuit`() {
        assertEquals(addCircuit, actionFor(ExternalDirectoryEditor.Circuit(circuit)))
        assertEquals(addCircuit, actionFor(ExternalDirectoryEditor.Congregation(congregation)))
        assertEquals(addCircuit, actionFor(ExternalDirectoryEditor.Speaker(speaker, "", speaker)))
    }
}
