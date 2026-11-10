package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory

/** Die Angaben eines Eintrags der Rednerliste für die Nur-Lesen-Ansicht. */
internal data class ReadOnlyView(val title: String, val lines: List<String>)

internal fun readOnlyView(editor: ExternalDirectoryEditor, directory: ExternalDirectory): ReadOnlyView = when (editor) {
    is ExternalDirectoryEditor.Circuit -> ReadOnlyView("Kreis", listOf(editor.circuit.name))

    is ExternalDirectoryEditor.Congregation -> ReadOnlyView(
        "Versammlung",
        listOf(editor.congregation.name, "Kreis: ${directory.circuitNameOf(editor.congregation.circuitId)}"),
    )

    is ExternalDirectoryEditor.Speaker -> {
        val speaker = editor.speaker
        val congregation = directory.congregations.firstOrNull { it.id == speaker.congregationId }
        ReadOnlyView(
            "Redner",
            listOfNotNull(
                listOf(speaker.lastName, speaker.firstName).filter(String::isNotBlank).joinToString(", "),
                congregation?.let { "Versammlung: ${it.name}" },
                speaker.mobilePhone.takeIf(String::isNotBlank)?.let { "Handy: $it" },
                speaker.landlinePhone.takeIf(String::isNotBlank)?.let { "Festnetz: $it" },
                speaker.email.takeIf(String::isNotBlank)?.let { "E-Mail: $it" },
                speaker.talkNumbers.takeIf { it.isNotEmpty() }?.let { "Vorträge: ${it.toText()}" },
                speaker.ranking.takeIf { it > 0 }?.let { "Bewertung: $it von 5" },
            ),
        )
    }
}

private fun ExternalDirectory.circuitNameOf(circuitId: String): String =
    circuits.firstOrNull { it.id == circuitId }?.name.orEmpty()
