package de.geosphere.congregationplaner.ui.list

/**
 * Wo ein losgelassener Eintrag einrastet: [Closed] liegt bündig, [Start] zeigt die Aktionen am Anfang (Wischen nach
 * rechts), [End] die am Ende (Wischen nach links).
 */
enum class SwipeAnchor { Closed, Start, End }

/** Der Versatz des Eintrags in Pixeln: positiv deckt die Aktionen am Anfang auf, negativ die am Ende. */
internal fun anchorOffset(anchor: SwipeAnchor, startWidth: Float, endWidth: Float): Float = when (anchor) {
    SwipeAnchor.Closed -> 0f
    SwipeAnchor.Start -> startWidth
    SwipeAnchor.End -> -endWidth
}

/** Begrenzt den Versatz auf den Bereich, den die Aktionen aufdecken; ohne Aktionen auf einer Seite bleibt er bei 0. */
internal fun clampOffset(offset: Float, startWidth: Float, endWidth: Float): Float =
    offset.coerceIn(if (endWidth > 0f) -endWidth else 0f, startWidth.coerceAtLeast(0f))

/** Rastet ein, sobald mindestens die halbe Aktionsbreite aufgedeckt ist; sonst springt der Eintrag zurück. */
internal fun settleAnchor(offset: Float, startWidth: Float, endWidth: Float): SwipeAnchor = when {
    startWidth > 0f && offset >= startWidth / 2f -> SwipeAnchor.Start
    endWidth > 0f && offset <= -endWidth / 2f -> SwipeAnchor.End
    else -> SwipeAnchor.Closed
}
