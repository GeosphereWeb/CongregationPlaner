package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory

/** Hinweis an einem Redner, den der Planer nicht mehr zur Auswahl anbietet. */
internal const val EXCLUDED_FROM_PLANNING_LABEL = "Von der Planung ausgeschlossen"

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
                EXCLUDED_FROM_PLANNING_LABEL.takeIf { speaker.excludedFromPlanning },
            ),
        )
    }
}

private fun ExternalDirectory.circuitNameOf(circuitId: String): String =
    circuits.firstOrNull { it.id == circuitId }?.name.orEmpty()
