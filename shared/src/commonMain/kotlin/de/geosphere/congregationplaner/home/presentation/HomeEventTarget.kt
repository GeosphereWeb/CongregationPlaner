package de.geosphere.congregationplaner.home.presentation

import de.geosphere.congregationplaner.home.domain.model.HomeEvent
import de.geosphere.congregationplaner.presentation.navigation.CongregationSection

/**
 * Wohin ein Tipp auf ein Ereignis führt: ein Termin in die Planung am Wochenende, ein Ereignis des Admins in dessen
 * Pflege; `null`, wenn der Nutzer den Bereich nicht sieht.
 */
internal fun homeEventTarget(event: HomeEvent, sections: List<CongregationSection>): CongregationSection? {
    val target = if (event.fromSchedule) CongregationSection.WEEKEND_TALKS else CongregationSection.SPECIAL_EVENTS
    return target.takeIf { it in sections }
}
