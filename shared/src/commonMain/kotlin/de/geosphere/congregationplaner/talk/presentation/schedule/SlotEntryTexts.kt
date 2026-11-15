package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.talkLabel

/** Der gewählte Vortrag als Text; ein frei eingetragener Vortrag nur mit Titel, `null` ohne Vortrag. */
internal fun slotTalkText(slot: TalkSlot): String? = slot.talkLabel()

/** Der gewählte Redner als Text; `null` ohne Redner. */
internal fun slotSpeakerText(slot: TalkSlot): String? = slot.speakerDisplayName

/** Hinweis unter dem Redner: fehlende Berechtigung, keine Redner überhaupt oder woher einer kommt. */
internal fun speakerSupporting(canViewMembers: Boolean, noSpeakers: Boolean, speakerMissing: Boolean): String? = when {
    !canViewMembers -> "Ohne die Berechtigung „Mitglieder einsehen“ stehen nur externe Redner zur Auswahl."
    noSpeakers -> "Noch kein Mitglied ist als Redner markiert (Userverwaltung)."
    speakerMissing -> "Aus der eigenen Versammlung oder aus „Externe Redner“"
    else -> null
}

/** Hinweis unter dem Vorsitz: fehlende Berechtigung, keine Brüder mit Dienstamt oder alle schon Redner. */
internal fun chairmanSupporting(canViewMembers: Boolean, noChairmen: Boolean, noneSelectable: Boolean): String? = when {
    !canViewMembers ->
        "Dir fehlt die Berechtigung „Mitglieder einsehen“, deshalb können keine Vorsitzenden geladen werden."
    noChairmen -> "Kein Bruder mit Dienstamt (Ältester oder Dienstamtgehilfe) mit Namen gefunden."
    noneSelectable -> "Alle Brüder mit Dienstamt sind schon als Redner eingeteilt."
    else -> null
}
