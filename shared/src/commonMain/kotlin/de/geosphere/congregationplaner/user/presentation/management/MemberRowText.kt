package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

/** Wartet ein Nutzer mit Konto noch auf die Freigabe? Personen ohne Konto werden nie freigegeben. */
internal val AppUser.isPending: Boolean get() = hasAccount && !approved

/** Titel einer Zeile der Mitgliederliste; das eigene Mitglied ist mit „(du)“ markiert. */
internal fun memberTitle(member: AppUser, isCurrentUser: Boolean): String =
    if (isCurrentUser) "${member.label} (du)" else member.label

/**
 * Zweite Zeile: Bruder oder Schwester (ungetauft: männlich oder weiblich), dazu der Zugang („Ohne Konto“, „Wartet
 * auf Freigabe“ oder die Rollen), z. B. „Bruder · Vortragsplaner“. Ohne jede Angabe „Mitglied“.
 */
internal fun memberSubtitle(member: AppUser): String {
    val access = when {
        !member.hasAccount -> "Ohne Konto"
        member.isPending -> "Wartet auf Freigabe"
        else -> member.roles.sortedBy { it.ordinal }.joinToString(" · ") { it.label }.ifEmpty { null }
    }
    return listOfNotNull(member.profile.groupLabel(), access).joinToString(" · ").ifEmpty { "Mitglied" }
}

private fun MemberProfile.groupLabel(): String? = when {
    isBrother -> "Bruder"
    isSister -> "Schwester"
    gender != null -> gender.label
    else -> "getauft".takeIf { baptized }
}

/** „Anmeldung: name@beispiel.de“; `null` für Personen ohne Konto oder ohne bekannte Adresse. */
internal fun memberLoginEmail(member: AppUser): String? =
    member.email?.takeIf { member.hasAccount && it.isNotBlank() }?.let { "Anmeldung: $it" }

/** Marke am Ende der Zeile: nur bei wartender Freigabe. */
internal fun memberBadge(member: AppUser): String? = "offen".takeIf { member.isPending }
