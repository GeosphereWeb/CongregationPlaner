package de.geosphere.congregationplaner.notice.pdf

private const val DEFAULT_MAX_EDGE = 600

/** Power-of-two subsampling factor that brings the longer edge of a [width] x [height] image to at most [maxEdge]. */
internal fun noticeImageSampleSize(width: Int, height: Int, maxEdge: Int = DEFAULT_MAX_EDGE): Int {
    val longest = maxOf(width, height)
    var sample = 1
    while ((longest + sample - 1) / sample > maxEdge) sample *= 2
    return sample
}
