package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import dev.zacsweers.metro.Inject

/** Die Eingaben einer Einheit: die ID bleibt die von [original] (sie ist Pfadsegment), Namen auf die erlaubte Länge. */
@Inject
class LimitOrgUnitInputUseCase {
    operator fun invoke(unit: OrgUnit, original: OrgUnit): OrgUnit = unit.copy(
        id = original.id,
        name = unit.name.take(OrgUnit.MAX_NAME_LENGTH),
        overseerName = unit.overseerName.take(OrgUnit.MAX_NAME_LENGTH),
    )
}
