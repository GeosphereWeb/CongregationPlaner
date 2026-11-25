package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.usecase.FrequencyGroup
import de.geosphere.congregationplaner.talk.domain.usecase.SpeakerTalkDays
import de.geosphere.congregationplaner.talk.domain.usecase.TalkHistoryRow
import de.geosphere.congregationplaner.talk.presentation.common.RecencyBadge
import de.geosphere.congregationplaner.talk.presentation.common.recencyBadgeOf
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText
import kotlinx.datetime.LocalDate

/** Überschrift einer Häufigkeitsgruppe, z. B. „3× gehalten“. */
internal val FrequencyGroup.title: String get() = if (count == 0) "Noch nie gehalten" else "$count× gehalten"

/** Die Tage, an denen der Redner den Vortrag gehalten hat, z. B. `So, 25.10.2026, So, 08.11.2026`. */
internal fun SpeakerTalkDays.daysText(): String = days.joinToString(", ") { it.toDisplayText() }

private const val INACTIVE_TALK_ALPHA = 0.5f

/** Ein nicht aktiver Vortrag wird nicht mehr geplant und steht deshalb gedimmt in der Historie. */
internal fun historyRowAlpha(talk: Talk?): Float = if (isInactive(talk)) INACTIVE_TALK_ALPHA else 1f

/** True, wenn [talk] in der Vortragsliste steht, aber nicht mehr aktiv ist. */
internal fun isInactive(talk: Talk?): Boolean = talk?.active == false

/** Der Chip „wie lange her“ zur Zeile: letzter Tag und Zahl aller Tage aus der Historie (CP-191). */
internal fun TalkHistoryRow.badge(today: LocalDate): RecencyBadge =
    recencyBadgeOf(entry?.lastHeldOn, entry?.heldOn?.size ?: 0, today)
