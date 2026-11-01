package de.geosphere.congregationplaner.user.data.mappers

import de.geosphere.congregationplaner.user.domain.model.CongregationRole

/**
 * Die Rollen eines Mitgliedsdokuments: das Feld `roles`, falls vorhanden, sonst abgeleitet aus den veralteten Feldern
 * `role` und `permissions` von Altdokumenten (ADR-0012). [storedRoles] ist `null`, wenn `roles` fehlt.
 */
internal fun memberRoles(
    storedRoles: Iterable<String?>?,
    legacyRole: String?,
    legacyPermissions: Iterable<String?>,
): Set<CongregationRole> = if (storedRoles != null) {
    CongregationRole.fromValues(storedRoles)
} else {
    legacyRoles(legacyRole, legacyPermissions)
}

internal fun legacyRoles(role: String?, permissions: Iterable<String?>): Set<CongregationRole> = buildSet {
    if (role == "CONGREGATION_ADMIN") add(CongregationRole.CONGREGATION_ADMIN)
    if ("PLAN_TALKS" in permissions) add(CongregationRole.TALK_PLANNER)
}

/** Die Rollen für das Feld `roles`: stabil sortiert nach Namen. */
internal fun Set<CongregationRole>.toStoredRoles(): List<String> = map { it.name }.sorted()
