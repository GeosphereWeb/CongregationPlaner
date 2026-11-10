package de.geosphere.congregationplaner.presentation.navigation

/** Die Tabs der Vortragsplanung; leer, wenn sie nicht als Tabs erscheint. */
internal data class TalkPlanningTabs(val sections: List<CongregationSection>, val selected: CongregationSection?) {
    val isActive: Boolean get() = sections.isNotEmpty()

    /** Beschriftung und Auswahl je Tab. */
    val labels: List<Pair<String, Boolean>> get() = sections.map { it.tabLabel to (it == selected) }
}

/**
 * Die Vortragsplanung ist auf dem Handy ein Bereich mit Tabs, auf großen Fenstern stehen die Teile in der Spalte.
 * Tabs gibt es nur, solange einer ihrer Teile gewählt ist.
 */
internal fun talkPlanningTabs(
    visible: List<CongregationSection>,
    selected: CongregationSection?,
    isWide: Boolean,
): TalkPlanningTabs {
    val talk = visible.filter { it in CongregationSection.talkPlanning }
    val active = !isWide && selected != null && selected in talk
    return TalkPlanningTabs(if (active) talk else emptyList(), selected)
}
