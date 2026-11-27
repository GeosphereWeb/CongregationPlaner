package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.ui.rating.StarFill

/**
 * The new rating in half stars after tapping star [tapped] (1-based): the first tap fills up to that star, the next
 * tap on the same star halves it, the third clears the rating.
 */
internal fun nextRanking(current: Int, tapped: Int): Int {
    val full = tapped.coerceIn(1, VisitingSpeaker.STARS) * 2
    return when (current) {
        full -> full - 1
        full - 1 -> 0
        else -> full
    }
}

/** Fill of each of the [VisitingSpeaker.STARS] stars for a rating in half stars. */
internal fun rankingStars(ranking: Int): List<StarFill> = List(VisitingSpeaker.STARS) { index ->
    when {
        ranking >= (index + 1) * 2 -> StarFill.Full
        ranking == index * 2 + 1 -> StarFill.Half
        else -> StarFill.Empty
    }
}
