package de.geosphere.congregationplaner.user.domain.model

/**
 * Rollen eines Nutzers innerhalb seiner Versammlung. Die Namen entsprechen den Werten im
 * Firestore-Feld `role` der `members`-Collection.
 */
enum class UserRole(val label: String) {
    /** Verwaltet die Nutzer seiner Versammlung. */
    CONGREGATION_ADMIN("Versammlungsadmin"),
    MEMBER("Mitglied"),
    NONE("Keine"),
    ;

    companion object {
        /** Unbekannte oder fehlende Werte werden bewusst auf [NONE] abgebildet (deny by default). */
        fun fromValue(value: String?): UserRole = entries.firstOrNull { it.name == value } ?: NONE

        /** Rollen, die ein Versammlungsadmin vergeben kann. */
        val assignable: List<UserRole> = listOf(CONGREGATION_ADMIN, MEMBER)
    }
}
