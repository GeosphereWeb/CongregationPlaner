package de.geosphere.congregationplaner.talk.presentation.picker

import de.geosphere.congregationplaner.talk.domain.model.Talk

internal fun Talk.label(): String = "$number – $title"

/** Nur aktive Vorträge; die Eingabe passt auf den Anfang der Nummer oder irgendwo im Titel. */
internal fun List<Talk>.matching(query: String): List<Talk> {
    val text = query.trim().lowercase()
    return filter { talk ->
        talk.active &&
            (text.isEmpty() || talk.number.toString().startsWith(text) || talk.title.lowercase().contains(text))
    }
}
