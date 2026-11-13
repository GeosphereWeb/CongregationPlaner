package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/**
 * Gibt es am Tag von [slot] schon einen anderen Termin als den, der von [originalDate] dorthin verschoben wird?
 *
 * shortcut: geprüft wird nur gegen [slots] (das geladene Jahr); beim Verschieben über den Jahreswechsel kann ein Termin
 * im anderen Jahr überschrieben werden. Bei Bedarf vor dem Speichern das Zieldokument lesen.
 */
@Inject
class IsSlotDateTakenUseCase {
    operator fun invoke(slots: List<TalkSlot>, slot: TalkSlot, originalDate: LocalDate?): Boolean =
        slots.any { it.date == slot.date && it.date != originalDate }
}
