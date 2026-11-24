package de.geosphere.congregationplaner.importer.domain.export

import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.planning.dateToExcelSerial
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

// The column names are the ones the import reads (see the planning files); the round-trip tests keep them in step.
internal const val SCHEDULE_DATE = "Datum"
private const val SCHEDULE_EVENT_ID = "ID_Wochenereignis"
private const val SCHEDULE_EVENT_NAME = "Ereignis"
private const val SCHEDULE_SPEAKER_ID = "ID_Redner"
private const val SCHEDULE_LAST_NAME = "Nachname"
private const val SCHEDULE_FIRST_NAME = "Vorname"
private const val SCHEDULE_CONGREGATION = "Name"
private const val SCHEDULE_TALK_NUMBER = "Nummer"
private const val SCHEDULE_TALK_TITLE = "Titel"
private const val SCHEDULE_INFORMED = "rednerIstInformiert"
private const val SCHEDULE_REQUESTED = "rednerIstAngefragt"

private val SCHEDULE_HEADERS = listOf(
    SCHEDULE_DATE,
    SCHEDULE_EVENT_ID,
    SCHEDULE_EVENT_NAME,
    SCHEDULE_TALK_NUMBER,
    SCHEDULE_TALK_TITLE,
    SCHEDULE_SPEAKER_ID,
    SCHEDULE_LAST_NAME,
    SCHEDULE_FIRST_NAME,
    SCHEDULE_CONGREGATION,
    SCHEDULE_INFORMED,
    SCHEDULE_REQUESTED,
)

private val DIRECTORY_HEADERS = listOf(
    "ID_Kreis",
    "tblKreis_Name",
    "ID_Versammlung",
    "tblVersammlung_Name",
    "ID_Redner",
    "Nachname",
    "Vorname",
    "Telefon",
    "Telefon2",
    "E-Mail",
    "Ranking",
    "Nummer",
)

private val CATALOG_HEADERS = listOf("Nummer", "Titel", "Aktiv")

/** Rows numbered as in the file: the header is row 1. */
private fun table(headers: List<String>, rows: List<Map<String, String?>>) = ImportTable(
    headers,
    rows.mapIndexed { index, cells ->
        ImportRow(index + 2, cells.filterValues { !it.isNullOrEmpty() }.mapValues { it.value.orEmpty() })
    },
)

private fun flag(value: Boolean) = if (value) "1" else "0"

/**
 * The slots in the columns of `Export_Vortragsplanung`, sorted by date. The date is an Excel serial number, the event
 * ID is the event type of the congregation (the import matches it by name). A member or free speaker has no ID, only
 * the name.
 */
fun exportTalkSchedule(slots: List<TalkSlot>): ImportTable = table(
    SCHEDULE_HEADERS,
    slots.sortedBy { it.date }.map { slot ->
        mapOf(
            SCHEDULE_DATE to dateToExcelSerial(slot.date).toString(),
            SCHEDULE_EVENT_ID to slot.weekEventId?.takeIf { slot.weekEventName != null },
            SCHEDULE_EVENT_NAME to slot.weekEventName?.takeIf { slot.weekEventId != null },
            SCHEDULE_TALK_NUMBER to slot.talkNumber?.toString(),
            SCHEDULE_TALK_TITLE to slot.talkTitle,
            SCHEDULE_SPEAKER_ID to slot.externalSpeakerId,
            SCHEDULE_LAST_NAME to slot.speakerLastName,
            SCHEDULE_FIRST_NAME to slot.speakerFirstName,
            SCHEDULE_CONGREGATION to slot.speakerCongregation,
            SCHEDULE_INFORMED to flag(slot.confirmed),
            SCHEDULE_REQUESTED to flag(slot.isRequested),
        )
    },
)

/**
 * The directory in the columns of `Export_Kreis-Versl-Redner-Vortraege`: one row per speaker and talk, a speaker
 * without talks and a congregation without speakers get one row each. A circuit without congregations cannot be
 * written (the import needs both) and is left out.
 */
fun exportSpeakerDirectory(directory: ExternalDirectory): ImportTable {
    val circuits = directory.circuits.associateBy { it.id }
    val rows = directory.congregations.flatMap { congregation ->
        val circuit = circuits[congregation.circuitId] ?: return@flatMap emptyList()
        val place = listOf(circuit.id, circuit.name, congregation.id, congregation.name)
        val speakers = directory.speakers.filter { it.congregationId == congregation.id }
        if (speakers.isEmpty()) return@flatMap listOf(DIRECTORY_HEADERS.zip(place).toMap())
        speakers.flatMap { speaker ->
            val person = place + listOf(
                speaker.id,
                speaker.lastName,
                speaker.firstName,
                speaker.mobilePhone,
                speaker.landlinePhone,
                speaker.email,
                speaker.ranking.toString(),
            )
            speaker.talkNumbers.ifEmpty { listOf(null) }.map { talk ->
                DIRECTORY_HEADERS.zip(person + talk?.toString()).toMap()
            }
        }
    }
    return table(DIRECTORY_HEADERS, rows)
}

/** The talk list in the columns of `Export_tblVortaege`, sorted by number; the title history is not part of it. */
fun exportTalkCatalog(talks: List<Talk>): ImportTable = table(
    CATALOG_HEADERS,
    talks.sortedBy { it.number }.map { talk ->
        CATALOG_HEADERS.zip(listOf(talk.number.toString(), talk.title, flag(talk.active))).toMap()
    },
)
