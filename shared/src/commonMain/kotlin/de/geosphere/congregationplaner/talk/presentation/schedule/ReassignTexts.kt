package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText

/** Vortrag und Redner eines Termins in einer Zeile, z. B. „Nr. 12 – Thema · Muster, Max“. */
internal fun slotAssignmentText(slot: TalkSlot): String =
    listOfNotNull(slotTalkText(slot), slotSpeakerText(slot)).joinToString(" · ")

/** Hinweis über der Liste, solange der Zieltag fürs Umplanen gesucht wird. */
internal fun reassignBannerText(source: TalkSlot): String =
    "„${slotAssignmentText(source)}“ vom ${source.date.toDisplayText()} umplanen: Zieltag in der Liste antippen."

/** Rückfrage, wenn der Zieltag schon belegt ist. */
internal fun swapQuestion(source: TalkSlot, target: TalkSlot): String =
    "Am ${target.date.toDisplayText()} ist schon „${slotAssignmentText(target)}“ eingeplant. " +
        "Sollen die Termine ${source.date.toDisplayText()} und ${target.date.toDisplayText()} tauschen?"

/** Meldung nach dem Umplanen. */
internal fun reassignedMessage(from: TalkSlot, to: TalkSlot): String =
    "Umgeplant: ${from.date.toDisplayText()} → ${to.date.toDisplayText()}."
