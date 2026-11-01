package de.geosphere.congregationplaner.presentation.navigation

/** Die vier Hauptbereiche der App (Leiste unten auf dem Handy, seitlich auf Tablet und Desktop). */
enum class TopLevelDestination(val label: String) {
    HOME("Home"),
    CONGREGATION("Versammlung"),
    GLOBAL_ADMIN("Globale Verwaltung"),
    SETTINGS("Einstellungen"),
}

/** Die Gruppen der Übersicht im Hauptbereich „Versammlung“. */
enum class CongregationGroup(val label: String) {
    ADMINISTRATION("Verwaltung"),
    PLANNING("Planung"),
}

/**
 * Die Unterbereiche von „Versammlung“. [nested] markiert Einträge, die in der Übersicht eingerückt unter dem
 * vorigen Eintrag stehen (Userverwaltung unter den Versammlungsdaten, die Vortragsplanung unter der Planung).
 */
enum class CongregationSection(val label: String, val group: CongregationGroup, val nested: Boolean = false) {
    GENERAL_DATA("Allgemeine Versammlungsdaten", CongregationGroup.ADMINISTRATION),
    USER_MANAGEMENT("Userverwaltung", CongregationGroup.ADMINISTRATION, nested = true),
    MIDWEEK_PLANNING("Planung unter der Woche", CongregationGroup.PLANNING),
    WEEKEND_TALKS("Planung am Wochenende", CongregationGroup.PLANNING, nested = true),
    EXTERNAL_SPEAKERS("Externe Redner", CongregationGroup.PLANNING, nested = true),
    TALK_HISTORY("Vortragshistorie", CongregationGroup.PLANNING, nested = true),
    ;

    companion object {
        /** Die Unterbereiche der Vortragsplanung; auf dem Handy sind sie Tabs, auf großen Fenstern eingerückt. */
        val talkPlanning: List<CongregationSection> = listOf(WEEKEND_TALKS, EXTERNAL_SPEAKERS, TALK_HISTORY)
    }
}

/** Die Unterbereiche von „Globale Verwaltung“. */
enum class GlobalSection(val label: String) {
    CIRCUITS("Kreise"),
    CONGREGATIONS("Versammlungen"),
    TALK_REFERENCE("Vortragsliste & Ereignisse"),
}

/** Was der angemeldete Nutzer sehen darf. Die Hauptbereiche ergeben sich aus den sichtbaren Unterbereichen. */
data class VisibleNavigation(
    val congregationSections: List<CongregationSection> = emptyList(),
    val globalSections: List<GlobalSection> = emptyList(),
) {
    val topLevel: List<TopLevelDestination> = buildList {
        add(TopLevelDestination.HOME)
        if (congregationSections.isNotEmpty()) add(TopLevelDestination.CONGREGATION)
        if (globalSections.isNotEmpty()) add(TopLevelDestination.GLOBAL_ADMIN)
        add(TopLevelDestination.SETTINGS)
    }
}
