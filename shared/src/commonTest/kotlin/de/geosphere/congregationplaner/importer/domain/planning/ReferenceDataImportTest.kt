package de.geosphere.congregationplaner.importer.domain.planning

import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class ReferenceDataImportTest {
    private fun table(vararg rows: Map<String, String>) =
        ImportTable(emptyList(), rows.mapIndexed { i, cells -> ImportRow(i + 2, cells) })

    private fun event(id: String, name: String) = mapOf("ID_Wochenereignis" to id, "Ereignis" to name)

    private fun talk(number: String, title: String, active: String = "1") =
        mapOf("Nummer" to number, "Titel" to title, "Aktiv" to active)

    @Test
    fun nameLessEventIsSkippedAndCancellingFlagFollowsTheName() {
        val rows = table(event("1", ""), event("2", "Kreiskongress"), event("3", "Dienstwoche"))
        val result = planWeekEvents(rows, emptyList())
        assertEquals(
            listOf(WeekEvent("2", "Kreiskongress", true), WeekEvent("3", "Dienstwoche", false)),
            result.plan.entries.map { it.item },
        )
        assertEquals(emptyList(), result.plan.errors)
    }

    @Test
    fun existingEventWithSameNameIsReusedAndMapped() {
        val result = planWeekEvents(table(event("5", "dienstwoche")), listOf(WeekEvent("abc", "Dienstwoche")))
        assertEquals(ImportKind.SKIP, result.plan.entries.single().kind)
        assertEquals(mapOf("5" to "abc"), result.idBySourceId)
    }

    @Test
    fun eventWithSameIdIsUpdatedWhenDifferentAndSkippedWhenEqual() {
        val changed = planWeekEvents(table(event("4", "Abendmahl x")), listOf(WeekEvent("4", "Abendmahl", true)))
        assertEquals(ImportKind.UPDATE, changed.plan.entries.single().kind)
        val same = planWeekEvents(table(event("4", "Abendmahl")), listOf(WeekEvent("4", "Abendmahl", false)))
        assertEquals(ImportKind.SKIP, same.plan.entries.single().kind)
    }

    @Test
    fun eventWithoutIdOrTooLongNameIsAnError() {
        val result = planWeekEvents(table(event("", "Foo"), event("7", "x".repeat(500))), emptyList())
        assertEquals(listOf(2, 3), result.plan.errors.map { it.row })
    }

    @Test
    fun talksAreCreatedUpdatedSkippedWithActiveFlag() {
        val existing = listOf(Talk(2, "Zwei"), Talk(3, "Drei alt"))
        val plan = planTalkCatalog(table(talk("1", "Eins", "0"), talk("2", "Zwei"), talk("3", "Drei")), existing)
        assertEquals(
            listOf(
                PlanEntry(Talk(1, "Eins", false), ImportKind.CREATE),
                PlanEntry(Talk(2, "Zwei"), ImportKind.SKIP),
                PlanEntry(Talk(3, "Drei"), ImportKind.UPDATE),
            ),
            plan.entries,
        )
    }

    @Test
    fun invalidTalkRowsAreErrorsWithRowNumber() {
        val plan = planTalkCatalog(
            table(talk("0", "Null"), talk("abc", "x"), talk("5", ""), talk("6", "Ok"), talk("6", "Doppelt")),
            emptyList(),
        )
        assertEquals(listOf(2, 3, 4, 6), plan.errors.map { it.row })
        assertEquals(1, plan.count(ImportKind.CREATE))
    }
}
