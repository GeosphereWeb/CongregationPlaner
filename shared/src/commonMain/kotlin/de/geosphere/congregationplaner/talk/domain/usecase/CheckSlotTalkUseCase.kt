package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.asOf
import dev.zacsweers.metro.Inject

/**
 * Wie der Vortrag eines Termins zur Vortragsliste passt.
 *
 * @param outdatedTitle Der Titel, der am Tag des Termins gilt, wenn er vom gespeicherten abweicht; sonst `null`.
 * @param inactiveNumber Die Nummer, wenn der Vortrag in der Liste nicht mehr aktiv ist; sonst `null`.
 * @param talksOnDate Die Vortragsliste mit den Titeln, die am Tag des Termins gelten (für die Auswahl).
 */
data class SlotTalkCheck(
    val outdatedTitle: String? = null,
    val inactiveNumber: Int? = null,
    val talksOnDate: List<Talk> = emptyList(),
)

/**
 * Prüft den Vortrag eines Termins gegen die Vortragsliste (ADR-0030). Frei eingetragene Vorträge und Nummern, die nicht
 * mehr in der Liste stehen, gelten nie als veraltet.
 */
@Inject
class CheckSlotTalkUseCase {
    operator fun invoke(slot: TalkSlot, talks: List<Talk>): SlotTalkCheck {
        val talk = talks.firstOrNull { it.number == slot.talkNumber }
        return SlotTalkCheck(
            outdatedTitle = talk?.titleOn(slot.date)?.takeIf { it != slot.talkTitle },
            inactiveNumber = talk?.takeIf { !it.active }?.number,
            talksOnDate = talks.asOf(slot.date),
        )
    }
}
