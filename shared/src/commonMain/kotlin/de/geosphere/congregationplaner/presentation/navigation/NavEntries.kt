package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.home.presentation.approvalsBadge
import de.geosphere.congregationplaner.ui.chip.StatusTone

internal const val TALK_PLANNING_LABEL = "Vortragsplanung"
private const val TALK_PLANNING_SUPPORTING_TEXT = "Vortragsplaner · Wochenende, Redner, Historie"

/**
 * Ein Eintrag der Übersicht bzw. der zweiten Spalte. [group] gliedert die Liste, [nested] rückt den Eintrag ein,
 * [tone] färbt den Untertext, [badge] ist eine kurze Marke wie „1 offen“ und [heading] ein nicht klickbarer Eintrag.
 */
internal data class NavEntry(
    val label: String,
    val group: CongregationGroup? = null,
    val nested: Boolean = false,
    val selected: Boolean = false,
    val supportingText: String? = null,
    val tone: StatusTone? = null,
    val badge: String? = null,
    val heading: Boolean = false,
    val onClick: () -> Unit = {},
)

/** Farbe der Gruppen-Überschrift und der Kante eingerückter Einträge; „Planung“ bleibt gedämpft. */
internal val CongregationGroup.tone: StatusTone?
    get() = if (this == CongregationGroup.ADMINISTRATION) StatusTone.Gold else null

/**
 * Die Einträge von „Versammlung“. Die Vortragsplanung ist auf dem Handy ein einzelner Eintrag, dessen Teile Tabs
 * sind; auf großen Fenstern steht sie als Überschrift mit den drei Teilen darunter.
 */
internal fun congregationEntries(
    sections: List<CongregationSection>,
    selected: CongregationSection?,
    isWide: Boolean,
    pendingApprovals: Int? = null,
    onOpen: (CongregationSection) -> Unit,
): List<NavEntry> = buildList {
    val talk = sections.filter { it in CongregationSection.talkPlanning }
    sections.forEach { section ->
        if (section !in talk) {
            add(
                NavEntry(
                    label = section.label,
                    group = section.group,
                    nested = section.nested,
                    selected = section == selected,
                    supportingText = section.supportingText,
                    tone = if (section == CongregationSection.MIDWEEK_PLANNING) StatusTone.Sky else null,
                    badge = approvalsBadge(pendingApprovals).takeIf { section == CongregationSection.USER_MANAGEMENT },
                    onClick = { onOpen(section) },
                ),
            )
        } else if (section == talk.first()) {
            if (isWide) {
                add(NavEntry(TALK_PLANNING_LABEL, section.group, heading = true))
                talk.forEach { part ->
                    add(
                        NavEntry(
                            label = part.label,
                            group = part.group,
                            nested = true,
                            selected = part == selected,
                            onClick = { onOpen(part) },
                        ),
                    )
                }
            } else {
                add(
                    NavEntry(
                        label = TALK_PLANNING_LABEL,
                        group = section.group,
                        selected = selected in talk,
                        supportingText = TALK_PLANNING_SUPPORTING_TEXT,
                        tone = StatusTone.Mint,
                        onClick = { onOpen(talk.first()) },
                    ),
                )
            }
        }
    }
}

internal fun globalEntries(
    sections: List<GlobalSection>,
    selected: GlobalSection?,
    onOpen: (GlobalSection) -> Unit,
): List<NavEntry> = sections.map { section ->
    NavEntry(label = section.label, selected = section == selected, onClick = { onOpen(section) })
}
