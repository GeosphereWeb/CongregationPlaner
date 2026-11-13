package de.geosphere.congregationplaner.importer.domain.planning

import de.geosphere.congregationplaner.importer.domain.model.ImportError
import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.importer.domain.model.kindFor
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.Talk
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
private const val CONGREGATION_NAME = "Name"
private const val TALK_NUMBER = "Nummer"
private const val TALK_TITLE = "Titel"
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
 * The slot plan and warnings that do not stop the import: a talk or speaker that is not in its list is kept as text
 * only (a free entry, see [TalkSlot]).
 */
data class TalkScheduleImport(val slots: ImportPlan<TalkSlot>, val warnings: List<ImportError>)

private class ScheduleRun(val context: ScheduleImportContext) {
    val errors = mutableListOf<ImportError>()
    val warnings = mutableListOf<ImportError>()
    val seenDates = mutableSetOf<LocalDate>()
    val existingByDate = context.existingSlots.associateBy { it.date }
    fun speaker(id: String): VisitingSpeaker? = context.directory.speakers.find { it.id == id }
}

/** Maps `Export_Vortragsplanung` to slots; an existing slot that already has a talk or a speaker stays untouched. */
fun planTalkSchedule(table: ImportTable, context: ScheduleImportContext): TalkScheduleImport {
    val run = ScheduleRun(context)
    val entries = table.rows.mapNotNull { row -> slotEntry(row, run) }
    return TalkScheduleImport(ImportPlan(entries, run.errors), run.warnings)
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
    if (existing != null && (existing.hasTalk || existing.hasSpeaker)) return PlanEntry(existing, ImportKind.SKIP)
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
        val title = row.text(TALK_TITLE)?.take(TalkSlot.MAX_TALK_TITLE_LENGTH)
        when {
            talk != null -> slot = slot.copy(talkNumber = talk.number, talkTitle = talk.title)
            title != null -> {
                slot = slot.copy(talkTitle = title)
                run.warnings += ImportError(row.number, "Vortrag $number ist nicht in der Vortragsliste; nur der Titel")
            }
            else -> run.errors += ImportError(row.number, "Vortrag $number ist nicht in der Vortragsliste")
        }
    }
    return withSpeaker(slot, row, run)
}

private fun withSpeaker(slot: TalkSlot, row: ImportRow, run: ScheduleRun): TalkSlot {
    val speakerId = row.idText(SPEAKER_ID) ?: return slot
    val speaker = run.speaker(speakerId) ?: return withFreeSpeaker(slot, speakerId, row, run)
    val congregation = run.context.directory.congregations.firstOrNull { it.id == speaker.congregationId }?.name
        ?: row.text(CONGREGATION_NAME)
    return slot.copy(
        externalSpeakerId = speaker.id,
        speakerLastName = speaker.lastName,
        speakerFirstName = speaker.firstName,
        speakerCongregation = congregation,
    )
}

// A speaker that is not in the directory is kept by name only, e.g. "Berg, Lea (Musterstadt)".
private fun withFreeSpeaker(slot: TalkSlot, id: String, row: ImportRow, run: ScheduleRun): TalkSlot {
    val name = freeSpeakerName(row)
    if (name == null) {
        run.errors += ImportError(row.number, "Redner $id ist nicht in der Rednerliste und hat keinen Namen")
        return slot
    }
    run.warnings += ImportError(row.number, "Redner $id ist nicht in der Rednerliste; nur der Name")
    return slot.copy(speakerLastName = name, speakerFirstName = "")
}

private fun freeSpeakerName(row: ImportRow): String? {
    val lastName = row.text(LAST_NAME) ?: return null
    val person = listOfNotNull(lastName, row.text(FIRST_NAME)).joinToString(", ")
    val name = row.text(CONGREGATION_NAME)?.let { "$person ($it)" } ?: person
    return name.take(TalkSlot.MAX_PERSON_NAME_LENGTH)
}
