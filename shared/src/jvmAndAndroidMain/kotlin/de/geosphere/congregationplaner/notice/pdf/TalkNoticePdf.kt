package de.geosphere.congregationplaner.notice.pdf

import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeRow
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice

private const val PAGE_W = 595f
private const val PAGE_H = 842f
private const val MARGIN = 28f
private const val TITLE_Y = 60f
private const val SUBTITLE_Y = 80f
private const val TABLE_TOP = 150f
private const val HEADER_H = 26f
private const val HEADER_BASELINE = 18f
private const val IMAGE_MAX = 80f
private const val FOOTER_COLOR = 0xFF808080
private const val ELLIPSIS = "…"
private const val CELL_PAD = 6f
private const val DATE_W = 100f
private const val NUMBER_RIGHT = 190f
private const val TITLE_X = 200f
private const val CHAIR_X = 440f
private const val TITLE_FONT = 20f
private const val SUBTITLE_FONT = 13f
private const val HEADER_FONT = 11f
private const val BODY_FONT = 10.5f
private const val SMALL_FONT = 10f
private const val EVENT_FONT = 8.5f
private const val FOOTER_FONT = 8f
private const val FIRST_BASELINE = 17f
private const val LINE_GAP = 13f
private const val TITLE_LINES = 2
private const val TABLE_W = PAGE_W - 2 * MARGIN
private const val TEXT_COL_W = CHAIR_X - CELL_PAD - TITLE_X

/** Draws the talk notice on one A4 page; row height is fixed so the page never overflows. */
object TalkNoticePdf {
    const val ROW_HEIGHT_PT = 54f

    fun layout(notice: TalkNotice, colors: NoticeColorSet, image: RgbaImage?): ByteArray {
        val pdf = PdfWriter(PAGE_W, PAGE_H)
        val imageWidth = image?.let { drawImage(pdf, it) } ?: 0f
        val headWidth = TABLE_W - if (imageWidth > 0f) imageWidth + CELL_PAD else 0f
        pdf.line(MARGIN, TITLE_Y, notice.title, headWidth, TITLE_FONT, true, colors.primary)
        pdf.line(MARGIN, SUBTITLE_Y, notice.congregationName, headWidth, SUBTITLE_FONT, false, colors.primary)
        drawHeader(pdf, colors)
        val bodyTop = TABLE_TOP + HEADER_H
        notice.emptyHint?.takeIf { notice.rows.isEmpty() }?.let {
            pdf.line(MARGIN + CELL_PAD, bodyTop + FIRST_BASELINE, it, TABLE_W, BODY_FONT, false, NoticeColorSet.TEXT)
        }
        notice.rows.forEachIndexed { i, row -> drawRow(pdf, row, bodyTop + i * ROW_HEIGHT_PT, i % 2 == 1, colors) }
        val footerX = PAGE_W - MARGIN - HelveticaMetrics.width(notice.footer, FOOTER_FONT, false)
        pdf.text(footerX, PAGE_H - MARGIN, notice.footer, FOOTER_FONT, false, FOOTER_COLOR)
        return pdf.toByteArray()
    }

    /** Single line, shortened with an ellipsis if wider than [maxWidth]. */
    @Suppress("LongParameterList") // position, text, width and font are independent drawing inputs
    private fun PdfWriter.line(x: Float, y: Float, s: String, maxWidth: Float, size: Float, bold: Boolean, argb: Long) {
        wrap(s, maxWidth, size, bold, 1).firstOrNull()?.let { text(x, y, it, size, bold, argb) }
    }

    /** Places the graphic top-right (max 80 x 80 pt, aspect kept) and returns its width. */
    private fun drawImage(pdf: PdfWriter, image: RgbaImage): Float {
        val scale = minOf(IMAGE_MAX / image.width, IMAGE_MAX / image.height)
        val w = image.width * scale
        pdf.image(PAGE_W - MARGIN - w, MARGIN, w, image.height * scale, image)
        return w
    }

    private fun drawHeader(pdf: PdfWriter, colors: NoticeColorSet) {
        pdf.fillRect(MARGIN, TABLE_TOP, TABLE_W, HEADER_H, colors.primary)
        val y = TABLE_TOP + HEADER_BASELINE
        val nrX = NUMBER_RIGHT - HelveticaMetrics.width("Nr.", HEADER_FONT, true)
        pdf.text(MARGIN + CELL_PAD, y, "Datum", HEADER_FONT, true, colors.onPrimary)
        pdf.text(nrX, y, "Nr.", HEADER_FONT, true, colors.onPrimary)
        pdf.text(TITLE_X, y, "Thema / Redner", HEADER_FONT, true, colors.onPrimary)
        pdf.text(CHAIR_X, y, "Vorsitz", HEADER_FONT, true, colors.onPrimary)
    }

    private fun drawRow(pdf: PdfWriter, row: NoticeRow, top: Float, striped: Boolean, colors: NoticeColorSet) {
        val text = NoticeColorSet.TEXT
        if (striped) pdf.fillRect(MARGIN, top, TABLE_W, ROW_HEIGHT_PT, colors.stripe)
        val y = top + FIRST_BASELINE
        pdf.line(MARGIN + CELL_PAD, y, row.dateText, DATE_W, BODY_FONT, true, text)
        // without a talk the week event takes the topic column instead of the title
        val eventAsTitle = row.title.isEmpty()
        if (!eventAsTitle) pdf.line(MARGIN + CELL_PAD, y + LINE_GAP, row.event, DATE_W, EVENT_FONT, true, text)
        if (row.number.isNotEmpty()) {
            val x = NUMBER_RIGHT - HelveticaMetrics.width(row.number, BODY_FONT, true)
            pdf.text(x, y, row.number, BODY_FONT, true, text)
        }
        val titleLines = wrap(if (eventAsTitle) row.event else row.title, TEXT_COL_W, BODY_FONT, true, TITLE_LINES)
        titleLines.forEachIndexed { i, s -> pdf.text(TITLE_X, y + i * LINE_GAP, s, BODY_FONT, true, text) }
        pdf.line(TITLE_X, y + titleLines.size * LINE_GAP, row.speakerLine, TEXT_COL_W, SMALL_FONT, false, text)
        pdf.line(CHAIR_X, y, row.chairman, PAGE_W - MARGIN - CELL_PAD - CHAIR_X, SMALL_FONT, false, text)
    }

    /** Greedy word wrap; text beyond [maxLines] is cut and the last line ends with an ellipsis. */
    internal fun wrap(text: String, maxWidth: Float, sizePt: Float, bold: Boolean, maxLines: Int): List<String> {
        fun width(s: String) = HelveticaMetrics.width(s, sizePt, bold)
        val lines = mutableListOf<String>()
        var current = ""
        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            var rest = word
            while (width(rest) > maxWidth) { // a single word wider than the line is broken by characters
                if (current.isNotEmpty()) lines += current.also { current = "" }
                val n = (1..rest.length).last { it == 1 || width(rest.take(it)) <= maxWidth }
                lines += rest.take(n)
                rest = rest.drop(n)
            }
            val candidate = if (current.isEmpty()) rest else "$current $rest"
            if (width(candidate) <= maxWidth) {
                current = candidate
            } else {
                lines += current
                current = rest
            }
        }
        if (current.isNotEmpty()) lines += current
        if (lines.size <= maxLines) return lines
        var last = lines[maxLines - 1]
        while (last.isNotEmpty() && width(last + ELLIPSIS) > maxWidth) last = last.dropLast(1)
        return lines.take(maxLines - 1) + (last.trimEnd() + ELLIPSIS)
    }
}
