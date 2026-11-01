package de.geosphere.congregationplaner.user.domain.model

/**
 * Gleichrangige Aufgaben eines Mitglieds innerhalb seiner Versammlung (ADR-0012). Ein Mitglied kann mehrere
 * Rollen kombinieren; ohne Rolle ist es ein einfaches Mitglied.
 */
enum class CongregationRole(val label: String) {
    /** Pflegt die Versammlungsdaten und verwaltet die Mitglieder. */
    CONGREGATION_ADMIN("Versammlungsadmin"),

    /** Plant die Vorträge am Wochenende. */
    TALK_PLANNER("Vortragsplaner"),

    /** Plant die Zusammenkunft unter der Woche. */
    TASK_PLANNER("Aufgabenplaner"),
    ;

    companion object {
        /** Unbekannte Werte werden ignoriert (deny by default). */
        fun fromValues(values: Iterable<String?>): Set<CongregationRole> =
            values.mapNotNull { value -> entries.firstOrNull { it.name == value } }.toSet()
    }
}
