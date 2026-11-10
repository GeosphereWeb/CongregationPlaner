package de.geosphere.congregationplaner.user.domain.model

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef

/**
 * Ein Mitglied einer Versammlung aus `…/congregation/{id}/members/{uid}`.
 *
 * @param uid Die ID aus Firebase Authentication; dient als Dokument-ID. Bei Personen ohne Konto ist es
 *   eine von Firestore vergebene ID.
 * @param approved Gibt an, ob der Versammlungsadmin den Nutzer freigeschaltet hat.
 * @param roles Die Rollen des Nutzers in der Versammlung (ADR-0012); ohne Rolle ist er ein einfaches Mitglied.
 * @param congregation Die Versammlung, zu der das Mitglied gehört.
 * @param isGlobalAdmin Globale Administratoren (Dokument in `admins/{uid}`) verwalten Kreise und
 *   Versammlungen und haben in jeder Versammlung Zugriff.
 * @param hasAccount `false` für Personen (z. B. Kinder), die der Admin ohne eigenes Login erfasst hat;
 *   sie sind nie freigegeben und haben keine Rolle.
 * @param profile Persönliche Angaben, die der Admin pflegt.
 * @param isMember `false` für einen globalen Admin, für den es kein Mitgliedsdokument gibt: Seine Angaben sind aus dem
 *   Login zusammengestellt, und es gibt nichts, das er als Mitglied speichern könnte.
 */
data class AppUser(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val approved: Boolean = false,
    val roles: Set<CongregationRole> = emptySet(),
    val congregation: CongregationRef? = null,
    val isGlobalAdmin: Boolean = false,
    val hasAccount: Boolean = true,
    val profile: MemberProfile = MemberProfile(),
    val isMember: Boolean = true,
) {
    val label: String
        get() = profile.fullName.takeIf(String::isNotBlank)
            ?: displayName?.takeIf(String::isNotBlank)
            ?: email?.takeIf(String::isNotBlank)
            ?: uid
}
