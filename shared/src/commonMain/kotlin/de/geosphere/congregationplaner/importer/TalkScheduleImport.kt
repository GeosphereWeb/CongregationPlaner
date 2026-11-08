package de.geosphere.congregationplaner.importer

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private const val DATE = "Datum"
private const val EVENT_ID = "ID_Wochenereignis"
private const val EVENT_NAME = "Ereignis"
private const val SPEAKER_ID = "ID_Redner"
private const val LAST_NAME = "Nachname"
private const val FIRST_NAME = "Vorname"
private const val EMAIL = "E-Mail"
private const val CONGREGATION_ID = "ID_Versammlung"
private const val CONGREGATION_NAME = "Name"
private const val TALK_NUMBER = "Nummer"
private const val MOBILE = "Telefon"
private const val LANDLINE = "Telefon2"
private const val INFORMED = "rednerIstInformiert"

/**
 * Everything the schedule import needs from the target congregation and the earlier import steps.
 *
 * @param existingSlots slots already stored in the target congregation; only empty ones are filled.
 * @param weekEvents the week events as they exist after the week event import.
 * @param weekEventIdBySourceId source event ID -> ID in [weekEvents] (see [WeekEventImport.idBySourceId]).
 * @param directory the speaker directory as it exists after the speaker list import.
 * @param timeFor meeting time for a year (the source has no time of day).
 */
data class ScheduleImportContext(
    val existingSlots: List<TalkSlot>,
    val catalog: List<Talk>,
    val weekEvents: List<WeekEvent>,
    val weekEventIdBySourceId: Map<String, String>,
    val directory: ExternalDirectory,
    val timeFor: (year: Int) -> LocalTime,
)

/**
 * The slot plan, the speakers that only occur in the schedule (created from the row), and warnings that do not stop
 * the import (e.g. a new speaker that looks like one already in the list).
 */
data class TalkScheduleImport(
    val slots: ImportPlan<TalkSlot>,
    val newSpeakers: List<VisitingSpeaker>,
    val warnings: List<ImportError>,
)

private class ScheduleRun(val context: ScheduleImportContext) {
    val errors = mutableListOf<ImportError>()
    val warnings = mutableListOf<ImportError>()
    val newSpeakers = linkedMapOf<String, VisitingSpeaker>()
    val seenDates = mutableSetOf<LocalDate>()
    val existingByDate = context.existingSlots.associateBy { it.date }
    val speakersById get() = context.directory.speakers.associateBy { it.id } + newSpeakers
}

/** Maps `Export_Vortragsplanung` to slots; an existing slot that already has a talk or a speaker stays untouched. */
fun planTalkSchedule(table: ImportTable, context: ScheduleImportContext): TalkScheduleImport {
    val run = ScheduleRun(context)
    val entries = table.rows.mapNotNull { row -> slotEntry(row, run) }
    return TalkScheduleImport(ImportPlan(entries, run.errors), run.newSpeakers.values.toList(), run.warnings)
}

private fun slotEntry(row: ImportRow, run: ScheduleRun): PlanEntry<TalkSlot>? {
    val date = row.text(DATE)?.toDoubleOrNull()?.let { runCatching { excelSerialToDate(it) }.getOrNull() }
    if (date == null) {
        run.errors += ImportError(row.number, "Ungültiges Datum: ${row.text(DATE)}")
        return null
    }
    if (!run.seenDates.add(date)) {
        run.errors += ImportError(row.number, "Datum $date kommt doppelt vor")
        return null
    }
    val existing = run.existingByDate[date]
    if (existing != null && (existing.talkNumber != null || existing.hasSpeaker)) {
        return PlanEntry(existing, ImportKind.SKIP)
    }
    val imported = buildSlot(row, date, run)
    // A slot the planner already created keeps its time, chairman and note.
    val wanted = existing?.fillWith(imported) ?: imported
    return PlanEntry(wanted, kindFor(existing, wanted))
}

private fun TalkSlot.fillWith(imported: TalkSlot): TalkSlot = imported.copy(
    time = time,
    chairmanMemberId = chairmanMemberId,
    chairmanLastName = chairmanLastName,
    chairmanFirstName = chairmanFirstName,
    note = note,
)

private fun buildSlot(row: ImportRow, date: LocalDate, run: ScheduleRun): TalkSlot {
    val context = run.context
    var slot = TalkSlot(
        date = date,
        time = context.timeFor(date.year),
        confirmed = row.text(INFORMED)?.toDoubleOrNull()?.let { it != 0.0 } ?: false,
    )
    row.idText(EVENT_ID)?.let { sourceId ->
        val event = context.weekEventIdBySourceId[sourceId]?.let { id ->
            context.weekEvents.firstOrNull { it.id == id }
        }
        if (event != null) {
            slot = slot.copy(weekEventId = event.id, weekEventName = event.name, talkCancelled = event.cancelsTalk)
        } else if (row.text(EVENT_NAME) != null) {
            run.errors += ImportError(row.number, "Unbekanntes Wochenereignis: ${row.text(EVENT_NAME)}")
        }
    }
    row.text(TALK_NUMBER)?.toDoubleOrNull()?.toInt()?.let { number ->
        val talk = context.catalog.firstOrNull { it.number == number }
        if (talk == null) {
            run.errors += ImportError(row.number, "Vortrag $number ist nicht in der Vortragsliste")
        } else {
            slot = slot.copy(talkNumber = talk.number, talkTitle = talk.title)
        }
    }
    return withSpeaker(slot, row, run)
}

private fun withSpeaker(slot: TalkSlot, row: ImportRow, run: ScheduleRun): TalkSlot {
    val speakerId = row.idText(SPEAKER_ID) ?: return slot
    val speaker = run.speakersById[speakerId] ?: createSpeaker(speakerId, row, run) ?: return slot
    val congregation = run.context.directory.congregations.firstOrNull { it.id == speaker.congregationId }?.name
        ?: row.text(CONGREGATION_NAME)
    return slot.copy(
        externalSpeakerId = speaker.id,
        speakerLastName = speaker.lastName,
        speakerFirstName = speaker.firstName,
        speakerCongregation = congregation,
    )
}

// A speaker that is only in the schedule is created from the row; the talk of the row is the one he gives.
private fun createSpeaker(id: String, row: ImportRow, run: ScheduleRun): VisitingSpeaker? {
    val congregationId = row.idText(CONGREGATION_ID)
    val lastName = row.text(LAST_NAME)
    val talk = row.text(TALK_NUMBER)?.toDoubleOrNull()?.toInt()
    if (lastName == null ||
        congregationId == null ||
        run.context.directory.congregations.none { it.id == congregationId }
    ) {
        run.errors += ImportError(row.number, "Redner $id ist nicht in der Rednerliste und kann nicht angelegt werden")
        return null
    }
    val speaker = VisitingSpeaker(
        id = id,
        congregationId = congregationId,
        lastName = lastName,
        firstName = row.text(FIRST_NAME).orEmpty(),
        mobilePhone = row.text(MOBILE).orEmpty(),
        landlinePhone = row.text(LANDLINE).orEmpty(),
        email = row.text(EMAIL).orEmpty(),
        talkNumbers = listOfNotNull(talk).filter { number -> run.context.catalog.any { it.number == number } },
    )
    val duplicate = run.context.directory.speakers.firstOrNull {
        it.congregationId == congregationId &&
            it.lastName.equals(lastName, ignoreCase = true) &&
            it.firstName.equals(speaker.firstName, ignoreCase = true)
    }
    if (duplicate != null) {
        run.warnings += ImportError(row.number, "Redner $lastName ${speaker.firstName} gleicht ${duplicate.id}")
    }
    run.newSpeakers[id] = speaker
    return speaker
}

/**
 * The history that follows from the slots that will be written (create or update): each held talk gets its day, as
 * planned day with the speaker's name, merged into the entry that already exists.
 */
fun planTalkHistory(slots: ImportPlan<TalkSlot>, existing: List<TalkHistoryEntry>): ImportPlan<TalkHistoryEntry> {
    val written = slots.entries.filter { it.kind != ImportKind.SKIP }.map { it.item }
        .filter { it.talkNumber != null && !it.talkCancelled }
    val entries = written.groupBy { it.talkNumber }.map { (number, talkSlots) ->
        val current = existing.firstOrNull { it.talkNumber == number }
        val wanted = talkSlots.fold(current ?: TalkHistoryEntry(requireNotNull(number), emptyList())) { entry, slot ->
            entry.withDay(slot)
        }
        PlanEntry(wanted, kindFor(current, wanted))
    }.sortedBy { it.item.talkNumber }
    return ImportPlan(entries, emptyList())
}

private fun TalkHistoryEntry.withDay(slot: TalkSlot): TalkHistoryEntry {
    val days = (heldOn + slot.date).distinct().sorted().takeLast(TalkHistoryEntry.MAX_DATES)
    val name = slot.externalSpeaker?.displayName
    val named = if (name == null) speakers - slot.date else speakers + (slot.date to name)
    return copy(
        heldOn = days,
        plannedOn = (plannedOn + slot.date).distinct().sorted().takeLast(TalkHistoryEntry.MAX_DATES),
        speakers = named.filterKeys { it in days },
    )
}
