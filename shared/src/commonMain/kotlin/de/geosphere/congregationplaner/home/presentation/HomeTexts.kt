package de.geosphere.congregationplaner.home.presentation

import de.geosphere.congregationplaner.presentation.navigation.germanDayAndMonth
import de.geosphere.congregationplaner.presentation.navigation.germanMonthName
import de.geosphere.congregationplaner.presentation.navigation.germanWeekdayName
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.presentation.slotSpeakerLine
import kotlinx.datetime.LocalDate

private const val WEEKDAY_SHORT_LENGTH = 2

internal const val NO_NEXT_TALK_TITLE = "Kein Termin geplant"
internal const val NO_NEXT_TALK_DETAILS = "Lege in der Planung am Wochenende einen Termin an."

internal fun nextTalkTitle(slot: TalkSlot): String = germanDayAndMonth(slot.date)

/** Vortrag mit Titel und darunter der Redner mit Herkunft. */
internal fun nextTalkDetails(slot: TalkSlot): String {
    val talk = slot.talkNumber?.let { "Vortrag Nr. $it – ${slot.talkTitle.orEmpty()}" } ?: "Noch nicht geplant"
    return "$talk\n${slotSpeakerLine(slot)}"
}

internal fun openWeekendsTitle(count: Int): String = when (count) {
    0 -> "Alles geplant"
    1 -> "1 Wochenende noch offen"
    else -> "$count Wochenenden noch offen"
}

/** „Nächste Lücke: So, 15. November“; ohne Lücke gibt es keinen Zusatz. */
internal fun openWeekendsDetails(openDates: List<LocalDate>): String? = openDates.firstOrNull()?.let {
    "Nächste Lücke: ${germanWeekdayName(it).take(WEEKDAY_SHORT_LENGTH)}, ${it.day}. ${germanMonthName(it)}"
}

internal fun approvalsTitle(count: Int): String = when (count) {
    0 -> "Keine offenen Freigaben"
    1 -> "1 Freigabe offen"
    else -> "$count Freigaben offen"
}

/** Marke an der Userverwaltung; ohne offene Freigaben gibt es keine. */
internal fun approvalsBadge(count: Int?): String? = count?.takeIf { it > 0 }?.let { "$it offen" }
