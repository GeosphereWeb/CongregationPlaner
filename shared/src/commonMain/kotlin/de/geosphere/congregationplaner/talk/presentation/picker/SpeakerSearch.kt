package de.geosphere.congregationplaner.talk.presentation.picker

import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef

/** Die Eingabe passt auf Familien- oder Vornamen, ohne Rücksicht auf Groß- und Kleinschreibung. */
internal fun List<MemberRef>.matching(query: String): List<MemberRef> {
    val text = query.trim().lowercase()
    return filter { member ->
        text.isEmpty() || member.lastName.lowercase().contains(text) || member.firstName.lowercase().contains(text)
    }
}

/** Die Eingabe passt auf Familien-, Vornamen oder die Versammlung, ohne Rücksicht auf Groß- und Kleinschreibung. */
internal fun List<ExternalSpeaker>.matchingExternal(query: String): List<ExternalSpeaker> {
    val text = query.trim().lowercase()
    return filter { speaker ->
        text.isEmpty() ||
            speaker.lastName.lowercase().contains(text) ||
            speaker.firstName.lowercase().contains(text) ||
            speaker.congregationName.lowercase().contains(text)
    }
}
