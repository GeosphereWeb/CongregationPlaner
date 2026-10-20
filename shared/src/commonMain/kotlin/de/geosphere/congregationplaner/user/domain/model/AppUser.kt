package de.geosphere.congregationplaner.user.domain.model

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef

/**
 * Ein Mitglied einer Versammlung aus `…/congregation/{id}/members/{uid}`.
 *
 * @param uid Die ID aus Firebase Authentication; dient als Dokument-ID.
 * @param approved Gibt an, ob der Versammlungsadmin den Nutzer freigeschaltet hat.
 * @param role Die Rolle innerhalb der Versammlung.
 * @param permissions Einzelne Berechtigungen des Nutzers innerhalb der Versammlung.
 * @param congregation Die Versammlung, zu der das Mitglied gehört.
 * @param isGlobalAdmin Globale Administratoren (Dokument in `admins/{uid}`) verwalten Kreise und
 *   Versammlungen und haben in jeder Versammlung Zugriff.
 */
data class AppUser(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val approved: Boolean = false,
    val role: UserRole = UserRole.MEMBER,
    val permissions: Set<UserPermission> = emptySet(),
    val congregation: CongregationRef? = null,
    val isGlobalAdmin: Boolean = false,
) {
    val label: String get() = displayName?.takeIf(String::isNotBlank) ?: email?.takeIf(String::isNotBlank) ?: uid
}
