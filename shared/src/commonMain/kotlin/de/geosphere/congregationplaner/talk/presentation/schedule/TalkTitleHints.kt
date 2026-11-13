package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

/**
 * Der Titel, der laut Vortragsliste am Tag des Termins gilt, wenn er vom gespeicherten Titel abweicht; sonst `null`
 * (auch für frei eingetragene Vorträge und Nummern, die nicht mehr in der Liste stehen).
 */
internal fun outdatedTalkTitle(slot: TalkSlot, talks: List<Talk>): String? {
    val talk = talks.firstOrNull { it.number == slot.talkNumber } ?: return null
    return talk.titleOn(slot.date).takeIf { it != slot.talkTitle }
}

/** Hinweis, wenn der Vortrag des Termins in der Vortragsliste nicht mehr aktiv ist; sonst `null`. */
internal fun inactiveTalkHint(slot: TalkSlot, talks: List<Talk>): String? =
    talks.firstOrNull { it.number == slot.talkNumber && !it.active }
        ?.let { "Nr. ${it.number} ist in der Vortragsliste nicht mehr aktiv; bitte prüfen, ob er noch gehalten wird." }
