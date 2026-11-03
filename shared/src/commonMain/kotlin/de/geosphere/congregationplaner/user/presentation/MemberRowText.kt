package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AppUser

/** Wartet ein Nutzer mit Konto noch auf die Freigabe? Personen ohne Konto werden nie freigegeben. */
internal val AppUser.isPending: Boolean get() = hasAccount && !approved

/** Titel einer Zeile der Mitgliederliste; das eigene Mitglied ist mit „(du)“ markiert. */
internal fun memberTitle(member: AppUser, isCurrentUser: Boolean): String =
    if (isCurrentUser) "${member.label} (du)" else member.label

/**
 * Zweite Zeile: Bruder, Schwester oder Kind und ob getauft, dazu der Zugang („Ohne Konto“, „Wartet auf Freigabe“
 * oder die Rollen), z. B. „Bruder · getauft · Vortragsplaner“. Ohne jede Angabe „Mitglied“.
 */
internal fun memberSubtitle(member: AppUser): String {
    val access = when {
        !member.hasAccount -> "Ohne Konto"
        member.isPending -> "Wartet auf Freigabe"
        else -> member.roles.sortedBy { it.ordinal }.joinToString(" · ") { it.label }.ifEmpty { null }
    }
    val parts = listOfNotNull(
        member.profile.category?.label,
        "getauft".takeIf { member.profile.baptized },
        access,
    )
    return parts.joinToString(" · ").ifEmpty { "Mitglied" }
}

/** Marke am Ende der Zeile: nur bei wartender Freigabe. */
internal fun memberBadge(member: AppUser): String? = "offen".takeIf { member.isPending }
