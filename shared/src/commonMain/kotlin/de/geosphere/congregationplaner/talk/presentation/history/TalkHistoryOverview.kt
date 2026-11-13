package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.usecase.FrequencyGroup
import de.geosphere.congregationplaner.talk.domain.usecase.SpeakerTalkDays
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText

/** Überschrift einer Häufigkeitsgruppe, z. B. „3× gehalten“. */
internal val FrequencyGroup.title: String get() = if (count == 0) "Noch nie gehalten" else "$count× gehalten"

/** Die Tage, an denen der Redner den Vortrag gehalten hat, z. B. `So, 25.10.2026, So, 08.11.2026`. */
internal fun SpeakerTalkDays.daysText(): String = days.joinToString(", ") { it.toDisplayText() }
