package de.geosphere.congregationplaner.user.domain.model

/**
 * Einzelberechtigungen eines Nutzers innerhalb seiner Versammlung. Die Namen entsprechen den Werten
 * im Firestore-Feld `permissions`. Vorerst gibt es nur [DUMMY]; die Liste wird später granularisiert.
 */
enum class UserPermission(val label: String) {
    DUMMY("Dummy-Berechtigung"),
    PLAN_TALKS("Vortragsplanung"),
    ;

    companion object {
        /** Neue Mitglieder erhalten vorerst die Platzhalter-Berechtigung. */
        val defaults: Set<UserPermission> = setOf(DUMMY)

        /** Unbekannte Werte werden ignoriert (deny by default). */
        fun fromValues(values: Iterable<String?>): Set<UserPermission> =
            values.mapNotNull { value -> entries.firstOrNull { it.name == value } }.toSet()
    }
}
