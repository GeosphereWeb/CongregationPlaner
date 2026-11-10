package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.congregation.presentation.toInputValue
import de.geosphere.congregationplaner.talk.domain.usecase.YearSlotsResult
import kotlinx.datetime.LocalTime

/** Rückmeldung nach „Jahr anlegen“: wie viele Termine neu sind und wie viele es schon gab. */
internal fun yearSlotsMessage(year: Int, result: YearSlotsResult): String = when {
    result.created == 0 -> "Für $year waren schon alle ${termine(result.alreadyExisting)} angelegt."
    result.alreadyExisting == 0 -> "${termine(result.created)} für $year angelegt."
    else -> "${termine(result.created)} für $year angelegt, ${result.alreadyExisting} waren schon vorhanden."
}

/** Rückmeldung nach „Uhrzeit ändern“: auf wie viele Termine die neue Uhrzeit gesetzt wurde. */
internal fun yearTimeMessage(year: Int, changed: Int, time: LocalTime): String = when (changed) {
    0 -> "Alle Termine $year hatten schon ${time.toInputValue()} Uhr."
    else -> "Uhrzeit von ${terminen(changed)} in $year auf ${time.toInputValue()} Uhr geändert."
}

private fun termine(count: Int): String = if (count == 1) "1 Termin" else "$count Termine"

private fun terminen(count: Int): String = if (count == 1) "1 Termin" else "$count Terminen"
