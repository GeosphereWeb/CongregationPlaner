package de.geosphere.congregationplaner.notice.pdf

/** Text metrics of the standard PDF fonts Helvetica and Helvetica-Bold (Adobe AFM widths, WinAnsi codes 32..255). */
object HelveticaMetrics {
    private const val FIRST = 32
    private const val UNDEFINED = 0
    private const val BYTE_MASK = 0xFF
    private const val UNITS_PER_EM = 1000f

    private fun rep(count: Int, value: Int) = IntArray(count) { value }

    private val regular: IntArray = intArrayOf(
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
    ) + rep(10, 556) + intArrayOf(278, 278, 584, 584, 584, 556, 1015) + intArrayOf(
        667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833,
        722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611,
    ) + intArrayOf(278, 278, 278, 469, 556, 333) + intArrayOf(
        556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833,
        556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500,
    ) + intArrayOf(334, 260, 334, 584, UNDEFINED) + intArrayOf(
        556, UNDEFINED, 222, 556, 333, 1000, 556, 556, 333, 1000, 667, 333, 1000, UNDEFINED, 611, UNDEFINED,
        UNDEFINED, 222, 222, 333, 333, 350, 556, 1000, 333, 1000, 500, 333, 944, UNDEFINED, 500, 667,
        278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 556, 537, 278, 333, 333, 365, 556, 834, 834, 834, 611,
    ) + rep(6, 667) + intArrayOf(1000, 722) + rep(4, 667) + rep(4, 278) + intArrayOf(722, 722) + rep(5, 778) +
        intArrayOf(584, 778) + rep(4, 722) + intArrayOf(667, 667, 611) + rep(6, 556) + intArrayOf(889, 500) +
        rep(4, 556) + rep(4, 278) + intArrayOf(556, 556) + rep(5, 556) + intArrayOf(584, 611) + rep(4, 556) +
        intArrayOf(500, 556, 500)

    private val bold: IntArray = intArrayOf(
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
    ) + rep(10, 556) + intArrayOf(333, 333, 584, 584, 584, 611, 975) + intArrayOf(
        722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833,
        722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611,
    ) + intArrayOf(333, 278, 333, 584, 556, 333) + intArrayOf(
        556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889,
        611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500,
    ) + intArrayOf(389, 280, 389, 584, UNDEFINED) + intArrayOf(
        556, UNDEFINED, 278, 556, 500, 1000, 556, 556, 333, 1000, 667, 333, 1000, UNDEFINED, 611, UNDEFINED,
        UNDEFINED, 278, 278, 500, 500, 350, 556, 1000, 333, 1000, 556, 333, 944, UNDEFINED, 500, 667,
        278, 333, 556, 556, 556, 556, 280, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 611, 556, 278, 333, 333, 365, 556, 834, 834, 834, 611,
    ) + rep(6, 722) + intArrayOf(1000, 722) + rep(4, 667) + rep(4, 278) + intArrayOf(722, 722) + rep(5, 778) +
        intArrayOf(584, 778) + rep(4, 722) + intArrayOf(667, 667, 611) + rep(6, 556) + intArrayOf(889, 556) +
        rep(4, 556) + rep(4, 278) + intArrayOf(611, 611) + rep(5, 611) + intArrayOf(584, 611) + rep(4, 611) +
        intArrayOf(556, 611, 556)

    private fun glyphWidth(table: IntArray, code: Int): Int {
        val w = table[code - FIRST]
        // Undefined codes use the width of the question mark
        return if (w == UNDEFINED) table['?'.code - FIRST] else w
    }

    fun width(text: String, sizePt: Float, bold: Boolean): Float {
        val table = if (bold) this.bold else regular
        var sum = 0
        for (b in winAnsiBytes(text)) sum += glyphWidth(table, b.toInt() and BYTE_MASK)
        return sum * sizePt / UNITS_PER_EM
    }
}
