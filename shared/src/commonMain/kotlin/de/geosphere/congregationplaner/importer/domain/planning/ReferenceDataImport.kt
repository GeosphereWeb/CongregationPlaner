package de.geosphere.congregationplaner.importer.domain.planning

import de.geosphere.congregationplaner.importer.domain.model.ImportError
import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent

private const val EVENT_ID_COLUMN = "ID_Wochenereignis"
private const val EVENT_NAME_COLUMN = "Ereignis"
private const val TALK_NUMBER_COLUMN = "Nummer"
private const val TALK_TITLE_COLUMN = "Titel"
private const val TALK_ACTIVE_COLUMN = "Aktiv"

// Source events after which the talk is dropped (decided in the import plan); all others keep it.
private val EVENTS_CANCELLING_TALK = setOf(
    "Kreiskongress",
    "Regionaler Kongress",
    "Besondere Zusammenkunft",
    "Zweigbesuch",
)

/** The plan for the week events plus the mapping source ID -> target ID the schedule import needs. */
data class WeekEventImport(val plan: ImportPlan<WeekEvent>, val idBySourceId: Map<String, String>)

/**
 * Maps `tblWochenereignis`. Rows without a name (the source's "no event") are skipped silently. An existing event with
 * the same name is reused instead of duplicated; an existing one with the same ID is updated if it differs.
 */
fun planWeekEvents(table: ImportTable, existing: List<WeekEvent>): WeekEventImport {
    val entries = mutableListOf<PlanEntry<WeekEvent>>()
    val errors = mutableListOf<ImportError>()
    val idBySourceId = mutableMapOf<String, String>()
    for (row in table.rows) {
        val sourceId = row.text(EVENT_ID_COLUMN)
        val name = row.text(EVENT_NAME_COLUMN) ?: continue
        when {
            sourceId == null -> errors += ImportError(row.number, "Wochenereignis ohne ID")
            name.length > TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH ->
                errors += ImportError(row.number, "Wochenereignis zu lang: $name")
            else -> {
                val sameName = existing.firstOrNull { it.name.equals(name, ignoreCase = true) }
                val wanted = WeekEvent(sourceId, name, name in EVENTS_CANCELLING_TALK)
                val sameId = existing.firstOrNull { it.id == sourceId }
                val entry = when {
                    sameName != null -> PlanEntry(sameName, ImportKind.SKIP)
                    sameId == null -> PlanEntry(wanted, ImportKind.CREATE)
                    sameId == wanted -> PlanEntry(wanted, ImportKind.SKIP)
                    else -> PlanEntry(wanted, ImportKind.UPDATE)
                }
                idBySourceId[sourceId] = entry.item.id
                entries += entry
            }
        }
    }
    return WeekEventImport(ImportPlan(entries, errors), idBySourceId)
}

/** Maps `Export_tblVortaege` (`Nummer`, `Titel`, `Aktiv`) to the global talk list. */
fun planTalkCatalog(table: ImportTable, existing: List<Talk>): ImportPlan<Talk> {
    val entries = mutableListOf<PlanEntry<Talk>>()
    val errors = mutableListOf<ImportError>()
    val seen = mutableSetOf<Int>()
    for (row in table.rows) {
        val number = row.text(TALK_NUMBER_COLUMN)?.toDoubleOrNull()?.toInt()
        val title = row.text(TALK_TITLE_COLUMN)
        val error = when {
            number == null || number !in 1..Talk.MAX_NUMBER -> "Ungültige Vortragsnummer"
            title == null || title.length > TalkSlot.MAX_TALK_TITLE_LENGTH -> "Ungültiger Titel"
            !seen.add(number) -> "Vortragsnummer $number kommt doppelt vor"
            else -> null
        }
        if (error != null || number == null || title == null) {
            errors += ImportError(row.number, error ?: "Ungültige Zeile")
            continue
        }
        val wanted = Talk(number, title, row.text(TALK_ACTIVE_COLUMN)?.toDoubleOrNull() != 0.0)
        val current = existing.firstOrNull { it.number == number }
        val kind = when {
            current == null -> ImportKind.CREATE
            current == wanted -> ImportKind.SKIP
            else -> ImportKind.UPDATE
        }
        entries += PlanEntry(wanted, kind)
    }
    return ImportPlan(entries, errors)
}
