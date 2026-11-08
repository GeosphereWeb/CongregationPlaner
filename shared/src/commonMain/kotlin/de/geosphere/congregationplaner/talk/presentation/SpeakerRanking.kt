package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker

/** The new rating after tapping star [tapped] (1-based): tapping the current rating again clears it. */
internal fun nextRanking(current: Int, tapped: Int): Int =
    if (current == tapped) 0 else tapped.coerceIn(0, VisitingSpeaker.MAX_RANKING)

/** Filled/empty star flags for a rating bar of [VisitingSpeaker.MAX_RANKING] stars. */
internal fun rankingStars(ranking: Int): List<Boolean> = List(VisitingSpeaker.MAX_RANKING) { it < ranking }
