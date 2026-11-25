package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.presentation.common.RecencyBadge
import de.geosphere.congregationplaner.talk.presentation.common.recencyBadgeOf
import kotlinx.datetime.LocalDate

/** Chip „wie lange nicht da“ eines Redners; `null`, solange die Besuche nicht geladen sind (CP-193). */
internal fun ExternalDirectoryUiState.speakerBadge(speakerId: String, today: LocalDate): RecencyBadge? =
    visits?.let { all -> all[speakerId].let { recencyBadgeOf(it?.lastVisit, it?.count ?: 0, today) } }

/**
 * Die gefilterten Redner für die Einladung: wer noch nie da war zuerst, dann der älteste Besuch; geplante Besuche
 * liegen in der Zukunft und stehen damit zuletzt. Bei Gleichstand nach Name.
 */
internal val ExternalDirectoryUiState.speakersByLastVisit: List<VisitingSpeaker>
    get() = visibleDirectory.speakers.sortedWith(
        compareBy<VisitingSpeaker, LocalDate?>(nullsFirst()) { visits?.get(it.id)?.lastVisit }
            .thenBy { it.displayName() },
    )

/** Der Name der Versammlung eines Redners, leer, wenn sie fehlt. */
internal fun ExternalDirectoryUiState.congregationName(speaker: VisitingSpeaker): String =
    directory.congregations.firstOrNull { it.id == speaker.congregationId }?.name.orEmpty()

private const val EXCLUDED_SPEAKER_ALPHA = 0.5f

/** Ein von der Planung ausgeschlossener Redner steht blass in der Liste. */
internal fun speakerRowAlpha(speaker: VisitingSpeaker): Float =
    if (speaker.excludedFromPlanning) EXCLUDED_SPEAKER_ALPHA else 1f
