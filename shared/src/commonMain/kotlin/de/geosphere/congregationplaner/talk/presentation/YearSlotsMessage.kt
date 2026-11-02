package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.usecase.YearSlotsResult

/** Rückmeldung nach „Jahr anlegen“: wie viele Termine neu sind und wie viele es schon gab. */
internal fun yearSlotsMessage(year: Int, result: YearSlotsResult): String = when {
    result.created == 0 -> "Für $year waren schon alle ${termine(result.alreadyExisting)} angelegt."
    result.alreadyExisting == 0 -> "${termine(result.created)} für $year angelegt."
    else -> "${termine(result.created)} für $year angelegt, ${result.alreadyExisting} waren schon vorhanden."
}

private fun termine(count: Int): String = if (count == 1) "1 Termin" else "$count Termine"
