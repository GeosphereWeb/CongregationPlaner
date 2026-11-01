package de.geosphere.congregationplaner.user.data.mappers

import de.geosphere.congregationplaner.user.domain.model.CongregationRole

/** Die Rollen für das Feld `roles`: stabil sortiert nach Namen. */
internal fun Set<CongregationRole>.toStoredRoles(): List<String> = map { it.name }.sorted()
