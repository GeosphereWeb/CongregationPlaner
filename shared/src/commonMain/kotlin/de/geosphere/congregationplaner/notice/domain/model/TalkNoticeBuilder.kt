package de.geosphere.congregationplaner.notice.domain.model

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private const val CANCELLED_TEXT = "Vortrag fällt aus"
private const val SPEAKER_PENDING = "Redner folgt"

/** Builds the notice from the slots on or after [start]; [count] is clamped to 1..[MAX_NOTICE_ROWS]. */
@Suppress("LongParameterList") // fixed contract of the notice feature; independent inputs
fun buildTalkNotice(
    slots: List<TalkSlot>,
    start: LocalDate,
    count: Int,
    title: String,
    congregationName: String,
    printedOn: LocalDate,
): TalkNotice {
    val rows = slots.filter { it.date >= start }
        .sortedBy { it.date }
        .take(count.coerceIn(1, MAX_NOTICE_ROWS))
        .map { it.toRow(congregationName) }
    return TalkNotice(
        title = title,
        congregationName = congregationName,
        rows = rows,
        emptyHint = if (rows.isEmpty()) "Keine Termine ab ${numericDate(start)}" else null,
        footer = "Stand: ${numericDate(printedOn)}",
    )
}

/** E.g. `So 14.12.2025`. */
fun noticeDateText(date: LocalDate): String = "${weekdayShort(date.dayOfWeek)} ${numericDate(date)}"

private fun TalkSlot.toRow(congregationName: String): NoticeRow {
    val dateText = noticeDateText(date)
    if (talkCancelled) {
        val event = weekEventName?.takeIf { it.isNotBlank() }
        return NoticeRow(dateText, "", event?.let { "$CANCELLED_TEXT ($it)" } ?: CANCELLED_TEXT, "", "", "")
    }
    val speakerLine = when {
        hasSpeaker -> speakerText(congregationName)
        hasTalk -> SPEAKER_PENDING
        else -> ""
    }
    return NoticeRow(
        dateText = dateText,
        number = talkNumber?.toString().orEmpty(),
        title = talkTitle.orEmpty(),
        speakerLine = speakerLine,
        chairman = "${chairmanFirstName.orEmpty()} ${chairmanLastName.orEmpty()}".trim(),
        event = weekEventName.orEmpty(),
    )
}

private fun TalkSlot.speakerText(congregationName: String): String {
    val name = "${speakerFirstName.orEmpty()} ${speakerLastName.orEmpty()}".trim()
    val congregation = if (speakerMemberId != null) congregationName else speakerCongregation
    return if (congregation.isNullOrBlank()) name else "$name · $congregation"
}

private fun numericDate(d: LocalDate): String =
    "${d.day.toString().padStart(2, '0')}.${d.month.number.toString().padStart(2, '0')}.${d.year}"

private fun weekdayShort(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Di"
    DayOfWeek.WEDNESDAY -> "Mi"
    DayOfWeek.THURSDAY -> "Do"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "So"
}
