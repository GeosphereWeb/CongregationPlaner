package de.geosphere.congregationplaner.importer.domain.planning

import de.geosphere.congregationplaner.importer.domain.model.ImportError
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.importer.domain.model.kindFor
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker

private const val CIRCUIT_ID = "ID_Kreis"
private const val CIRCUIT_NAME = "tblKreis_Name"
private const val CONGREGATION_ID = "ID_Versammlung"
private const val CONGREGATION_NAME = "tblVersammlung_Name"
private const val SPEAKER_ID = "ID_Redner"
private const val LAST_NAME = "Nachname"
private const val FIRST_NAME = "Vorname"
private const val MOBILE = "Telefon"
private const val LANDLINE = "Telefon2"
private const val EMAIL = "E-Mail"
private const val RANKING = "Ranking"
private const val TALK_NUMBER = "Nummer"

/** The three plans a speaker list import produces; write them in this order (circuits, congregations, speakers). */
data class SpeakerDirectoryImport(
    val circuits: ImportPlan<ExternalCircuit>,
    val congregations: ImportPlan<ExternalCongregation>,
    val speakers: ImportPlan<VisitingSpeaker>,
)

/** One row of the source is one speaker x talk combination, so rows are grouped by speaker ID. */
private class SpeakerDraft(val id: String, val rowNumber: Int, val congregationId: String, val row: ImportRow) {
    val talkNumbers = mutableSetOf<Int>()
}

/**
 * Maps `Export_Kreis-Versl-Redner-Vortraege` to circuits, congregations and speakers with their talk numbers. IDs are
 * the source IDs, so a second run updates instead of duplicating. A talk number that is not in [knownTalkNumbers] is
 * reported as an error and left out; the speaker is still imported.
 */
fun planSpeakerDirectory(
    table: ImportTable,
    existing: ExternalDirectory,
    knownTalkNumbers: Set<Int>,
): SpeakerDirectoryImport {
    val errors = mutableListOf<ImportError>()
    val circuits = linkedMapOf<String, ExternalCircuit>()
    val congregations = linkedMapOf<String, ExternalCongregation>()
    val drafts = linkedMapOf<String, SpeakerDraft>()
    for (row in table.rows) {
        val circuitId = row.idText(CIRCUIT_ID)
        val circuitName = row.text(CIRCUIT_NAME)
        val congregationId = row.idText(CONGREGATION_ID)
        val congregationName = row.text(CONGREGATION_NAME)
        if (circuitId == null || circuitName == null || congregationId == null || congregationName == null) {
            errors += ImportError(row.number, "Kreis oder Versammlung unvollständig")
            continue
        }
        circuits.getOrPut(circuitId) { ExternalCircuit(circuitId, circuitName) }
        congregations.getOrPut(congregationId) { ExternalCongregation(congregationId, circuitId, congregationName) }
        val speakerId = row.idText(SPEAKER_ID) ?: continue
        val draft = drafts.getOrPut(speakerId) { SpeakerDraft(speakerId, row.number, congregationId, row) }
        val talk = row.text(TALK_NUMBER)?.toDoubleOrNull()?.toInt()
        when {
            talk == null -> Unit
            talk in knownTalkNumbers -> draft.talkNumbers += talk
            else -> errors += ImportError(row.number, "Vortrag $talk ist nicht in der Vortragsliste")
        }
    }
    val speakers = drafts.values.mapNotNull { toSpeaker(it, errors) }
    return SpeakerDirectoryImport(
        circuits = ImportPlan(
            circuits.values.map { PlanEntry(it, kindFor(existing.circuits.firstOrNull { c -> c.id == it.id }, it)) },
            emptyList(),
        ),
        congregations = ImportPlan(
            congregations.values.map {
                PlanEntry(it, kindFor(existing.congregations.firstOrNull { c -> c.id == it.id }, it))
            },
            emptyList(),
        ),
        speakers = ImportPlan(
            speakers.map { PlanEntry(it, kindFor(existing.speakers.firstOrNull { s -> s.id == it.id }, it)) },
            errors,
        ),
    )
}

private fun toSpeaker(draft: SpeakerDraft, errors: MutableList<ImportError>): VisitingSpeaker? {
    val row = draft.row
    val lastName = row.text(LAST_NAME)
    val ranking = row.text(RANKING)?.toDoubleOrNull()?.toInt() ?: 0
    val problem = when {
        lastName == null -> "Redner ohne Nachnamen"
        ranking !in 0..VisitingSpeaker.MAX_RANKING -> "Ungültige Bewertung $ranking"
        listOf(lastName, row.text(FIRST_NAME).orEmpty()).any { it.length > VisitingSpeaker.MAX_NAME_LENGTH } ->
            "Name zu lang"
        listOf(row.text(MOBILE), row.text(LANDLINE)).any { (it?.length ?: 0) > VisitingSpeaker.MAX_PHONE_LENGTH } ->
            "Telefonnummer zu lang"
        (row.text(EMAIL)?.length ?: 0) > VisitingSpeaker.MAX_EMAIL_LENGTH -> "E-Mail zu lang"
        else -> null
    }
    if (problem != null || lastName == null) {
        errors += ImportError(draft.rowNumber, problem ?: "Ungültiger Redner")
        return null
    }
    return VisitingSpeaker(
        id = draft.id,
        congregationId = draft.congregationId,
        lastName = lastName,
        firstName = row.text(FIRST_NAME).orEmpty(),
        mobilePhone = row.text(MOBILE).orEmpty(),
        landlinePhone = row.text(LANDLINE).orEmpty(),
        email = row.text(EMAIL).orEmpty(),
        talkNumbers = draft.talkNumbers.sorted(),
        ranking = ranking,
    )
}
