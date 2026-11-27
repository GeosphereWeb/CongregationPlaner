package de.geosphere.congregationplaner.notice.pdf

import de.geosphere.congregationplaner.notice.domain.model.MAX_NOTICE_ROWS
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeRow
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TalkNoticePdfTest {
    private val font = 10.5f

    private fun row(i: Int) = NoticeRow(
        "So ${i + 1}.12.2026",
        "${100 + i}",
        "Wie kann man als Familie wirklich glücklich werden und bleiben $i",
        "Michael Hajtud · München-Harlaching",
        "Max Muster",
        if (i == 2) "Dienstwoche" else "",
    )

    private fun notice(rows: List<NoticeRow>, hint: String? = null) =
        TalkNotice("Zusammenkünfte für die Öffentlichkeit", "München-Freimann", rows, hint, "Stand: 27.11.2026")

    private fun content(pdf: ByteArray): String {
        val text = String(pdf, Charsets.ISO_8859_1)
        return Regex(">>\nstream\n(.*?)\nendstream", RegexOption.DOT_MATCHES_ALL).findAll(text).joinToString("\n") {
            val out = ByteArrayOutputStream()
            val data = it.groupValues[1].toByteArray(Charsets.ISO_8859_1)
            InflaterInputStream(data.inputStream()).use { s -> s.copyTo(out) }
            String(out.toByteArray(), Charsets.ISO_8859_1)
        }
    }

    private fun assertValidPdf(pdf: ByteArray) {
        val text = String(pdf, Charsets.ISO_8859_1)
        assertTrue(text.startsWith("%PDF-1.4"))
        val lines = text.substring(text.lastIndexOf("\nxref\n") + 1).lines()
        val count = lines[1].split(" ")[1].toInt()
        for (n in 1 until count) {
            assertTrue(text.startsWith("$n 0 obj", lines[2 + n].substring(0, 10).toInt()), "object $n")
        }
    }

    @Test
    fun `max rows end above the footer`() {
        assertTrue(150 + 26 + MAX_NOTICE_ROWS * TalkNoticePdf.ROW_HEIGHT_PT <= 842 - 28 - 12)
    }

    @Test
    fun `wrap keeps short text in one line`() {
        assertEquals(listOf("Kurz"), TalkNoticePdf.wrap("Kurz", 200f, font, true, 2))
        assertEquals(emptyList(), TalkNoticePdf.wrap("", 200f, font, true, 2))
    }

    @Test
    fun `wrap breaks at spaces into two lines`() {
        val lines = TalkNoticePdf.wrap("eins zwei drei vier fünf sechs", 70f, font, true, 3)
        assertEquals("eins zwei drei vier fünf sechs", lines.joinToString(" "))
        assertTrue(lines.size in 2..3)
    }

    @Test
    fun `wrap ellipsizes the rest and respects the width`() {
        val text = "Wie kann man als Familie wirklich glücklich werden und bleiben, auch wenn es schwierig wird"
        val lines = TalkNoticePdf.wrap(text, 120f, font, true, 2)
        assertEquals(2, lines.size)
        assertTrue(lines.last().endsWith("…"))
        lines.forEach { assertTrue(HelveticaMetrics.width(it, font, true) <= 120f, it) }
    }

    @Test
    fun `wrap breaks a word that is wider than the line`() {
        val lines = TalkNoticePdf.wrap("Donaudampfschifffahrtsgesellschaft", 60f, font, false, 5)
        assertTrue(lines.size > 1)
        lines.forEach { assertTrue(HelveticaMetrics.width(it, font, false) <= 60f) }
    }

    @Test
    fun `full notice is a valid pdf with header footer and primary color`() {
        val pdf = TalkNoticePdf.layout(notice((0 until 11).map(::row)), NoticeColorSet.GREEN, null)
        assertValidPdf(pdf)
        val c = content(pdf)
        assertContains(c, "(Nr.)")
        assertContains(c, "(Stand: 27.11.2026)")
        assertContains(c, "(Dienstwoche)")
        assertContains(c, "0.118 0.337 0.192 rg") // 1E5631
    }

    @Test
    fun `week event without talk is drawn once in the topic column`() {
        val c = content(
            TalkNoticePdf.layout(
                notice(listOf(NoticeRow("So 1.12.2026", "", "", "", "Max Muster", "Dienstwoche"))),
                NoticeColorSet.BLUE,
                null,
            ),
        )
        assertEquals(1, Regex("""\(Dienstwoche\)""").findAll(c).count())
        assertContains(c, Regex("""\n?BT /F2 10.5 Tf [\d. ]+ rg 200 [\d.]+ Td \(Dienstwoche\) Tj""").find(c)!!.value)
    }

    @Test
    fun `week event with talk is drawn small under the date`() {
        val c = content(TalkNoticePdf.layout(notice(listOf(row(2))), NoticeColorSet.BLUE, null))
        assertEquals(1, Regex("""\(Dienstwoche\)""").findAll(c).count())
        assertContains(c, "BT /F2 8.5 Tf")
        assertTrue(Regex("""200 [\d.]+ Td \(Dienstwoche\)""").find(c) == null)
    }

    @Test
    fun `empty notice shows the hint`() {
        val pdf = TalkNoticePdf.layout(notice(emptyList(), "Keine Termine ab 01.12.2026"), NoticeColorSet.BLUE, null)
        assertValidPdf(pdf)
        assertContains(content(pdf), "(Keine Termine ab 01.12.2026)")
    }

    @Test
    fun `notice with alpha image is valid`() {
        val image = RgbaImage(2, 1, ByteArray(6), byteArrayOf(0, 127))
        val pdf = TalkNoticePdf.layout(notice(listOf(row(0))), NoticeColorSet.PETROL, image)
        assertValidPdf(pdf)
        assertContains(content(pdf), "/Im1 Do")
    }

    @Test
    fun `fromArgb splits channels and drops an opaque alpha plane`() {
        val translucent = RgbaImage.fromArgb(2, 1, intArrayOf(0x80112233.toInt(), 0xFF445566.toInt()))
        assertEquals(listOf<Byte>(0x11, 0x22, 0x33, 0x44, 0x55, 0x66), translucent.rgb.toList())
        assertEquals(listOf(0x80.toByte(), 0xFF.toByte()), translucent.alpha?.toList())
        assertNull(RgbaImage.fromArgb(1, 1, intArrayOf(0xFF445566.toInt())).alpha)
        assertNotNull(translucent.alpha)
    }
}
