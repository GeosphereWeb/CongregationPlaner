package de.geosphere.congregationplaner.notice.pdf

import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfWriterTest {
    private fun ByteArray.asText() = String(this, Charsets.ISO_8859_1)

    private fun inflatedStreams(pdf: ByteArray): String {
        val text = pdf.asText()
        return Regex(">>\nstream\n(.*?)\nendstream", RegexOption.DOT_MATCHES_ALL).findAll(text).joinToString("\n") {
            val data = it.groupValues[1].toByteArray(Charsets.ISO_8859_1)
            val out = ByteArrayOutputStream()
            InflaterInputStream(data.inputStream()).use { s -> s.copyTo(out) }
            out.toByteArray().asText()
        }
    }

    private fun sample(image: RgbaImage? = null): ByteArray = PdfWriter(595f, 842f).apply {
        fillRect(10f, 10f, 100f, 20f, 0xFF1F3864)
        text(20f, 40f, "Wie kann man", 12f, false, 0xFF404040)
        image?.let { image(0f, 0f, 50f, 50f, it) }
    }.toByteArray()

    @Test
    fun `output has PDF header and EOF trailer`() {
        val pdf = sample()
        assertTrue(pdf.asText().startsWith("%PDF-1.4"))
        assertTrue(pdf.asText().trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun `every xref offset points at its object`() {
        val text = sample(RgbaImage(1, 1, byteArrayOf(1, 2, 3), byteArrayOf(9))).asText()
        val xrefPos = text.lastIndexOf("\nxref\n") + 1
        val lines = text.substring(xrefPos).lines()
        val count = lines[1].split(" ")[1].toInt()
        assertTrue(count > 6)
        for (n in 1 until count) {
            val offset = lines[2 + n].substring(0, 10).toInt()
            assertTrue(text.startsWith("$n 0 obj", offset), "object $n at $offset")
        }
    }

    @Test
    fun `content stream has escaped text and colors`() {
        val pdf = PdfWriter(595f, 842f).apply {
            text(20f, 40f, "Wie kann man", 12f, true, 0xFF1F3864)
            text(20f, 60f, "a(b)c\\d", 12f, false, 0xFF404040)
        }.toByteArray()
        val content = inflatedStreams(pdf)
        assertContains(content, "(Wie kann man) Tj")
        assertContains(content, """(a\(b\)c\\d) Tj""")
        assertContains(content, "0.122 0.220 0.392 rg")
        assertContains(content, "20 802 Td")
    }

    @Test
    fun `non ascii text is written as octal escapes`() {
        val content = inflatedStreams(
            PdfWriter(595f, 842f).apply { text(0f, 0f, "Grüße", 10f, false, 0xFF000000) }.toByteArray(),
        )
        assertContains(content, """(Gr\374\337e) Tj""")
    }

    @Test
    fun `winAnsiBytes transliterates letters outside WinAnsi`() {
        assertContentEquals("Kovacevic".toByteArray(Charsets.ISO_8859_1), winAnsiBytes("Kovačević"))
        val expected = byteArrayOf(0x8A.toByte()) + "aric".toByteArray(Charsets.ISO_8859_1)
        assertContentEquals(expected, winAnsiBytes("Šarić"))
    }

    @Test
    fun `winAnsiBytes maps an emoji to a single question mark`() {
        val emoji = String(Character.toChars(0x1F600))
        assertContentEquals("a?".toByteArray(Charsets.ISO_8859_1), winAnsiBytes("a" + emoji))
    }

    @Test
    fun `winAnsiBytes maps the remaining WinAnsi specials`() {
        val expected = intArrayOf(0x8A, 0x9A, 0x8E, 0x9E, 0x8C, 0x9C, 0x9F, 0x94, 0x95, 0x99, 0x89, 0x9B, 0x8B)
            .map { it.toByte() }.toByteArray()
        assertContentEquals(expected, winAnsiBytes("ŠšŽžŒœŸ”•™‰›‹"))
    }

    @Test
    fun `winAnsiBytes maps typographic characters`() {
        val expected = byteArrayOf(
            0x84.toByte(), 'G'.code.toByte(), 'o'.code.toByte(), 't'.code.toByte(), 't'.code.toByte(),
            0x93.toByte(), ' '.code.toByte(), 0x96.toByte(), ' '.code.toByte(), 0xDF.toByte(),
            ' '.code.toByte(), '?'.code.toByte(),
        )
        assertContentEquals(expected, winAnsiBytes("„Gott“ – ß №"))
    }

    @Test
    fun `helvetica widths match AFM`() {
        assertEquals(6.67f, HelveticaMetrics.width("A", 10f, false), 0.01f)
        assertEquals(7.22f, HelveticaMetrics.width("A", 10f, true), 0.01f)
        assertEquals(0f, HelveticaMetrics.width("", 10f, false))
    }

    @Test
    fun `image with alpha writes an SMask`() {
        val text = sample(RgbaImage(1, 1, byteArrayOf(1, 2, 3), byteArrayOf(9))).asText()
        assertContains(text, "/SMask")
        assertContains(text, "/DeviceGray")
        assertContains(text, "/Subtype /Image")
    }

    @Test
    fun `opaque image writes no SMask`() {
        val text = sample(RgbaImage(1, 1, byteArrayOf(1, 2, 3), null)).asText()
        assertContains(text, "/Subtype /Image")
        assertFalse(text.contains("/SMask"))
    }
}
