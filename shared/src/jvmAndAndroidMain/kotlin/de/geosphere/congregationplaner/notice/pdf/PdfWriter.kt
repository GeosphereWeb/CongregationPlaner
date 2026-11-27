package de.geosphere.congregationplaner.notice.pdf

import java.io.ByteArrayOutputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.DeflaterOutputStream

/** Raster image: [rgb] has 3 bytes per pixel, [alpha] 1 byte per pixel or null if opaque. */
class RgbaImage(val width: Int, val height: Int, val rgb: ByteArray, val alpha: ByteArray?) {
    companion object {
        /** From non-premultiplied ARGB pixels; the alpha plane is dropped if every pixel is opaque. */
        @Suppress("MagicNumber") // channel shifts of the ARGB packing
        fun fromArgb(width: Int, height: Int, argb: IntArray): RgbaImage {
            val rgb = ByteArray(argb.size * 3)
            val alpha = ByteArray(argb.size)
            argb.forEachIndexed { i, p ->
                rgb[i * 3] = (p shr 16).toByte()
                rgb[i * 3 + 1] = (p shr 8).toByte()
                rgb[i * 3 + 2] = p.toByte()
                alpha[i] = (p ushr 24).toByte()
            }
            return RgbaImage(width, height, rgb, alpha.takeUnless { a -> a.all { it == 0xFF.toByte() } })
        }
    }
}

private const val BITS_PER_BYTE = 8
private const val BYTE_MASK = 0xFF
private const val BYTE_MAX = 255f
private const val ASCII_LIMIT = 0x80
private const val OCTAL_RADIX = 8
private const val OCTAL_DIGITS = 3
private const val UNKNOWN = 0x3F
private val ASCII_PRINTABLE = 0x20..0x7E
private val LATIN1_HIGH = 0xA0..0xFF

// WinAnsi code points (0x80..0x9F block) of typographic characters
@Suppress("MagicNumber")
private val WIN_ANSI_SPECIALS = mapOf(
    '€' to 0x80, '‚' to 0x82, '„' to 0x84, '…' to 0x85, '‘' to 0x91,
    '’' to 0x92, '“' to 0x93, '–' to 0x96, '—' to 0x97,
    'Š' to 0x8A, 'š' to 0x9A, 'Ž' to 0x8E, 'ž' to 0x9E, 'Œ' to 0x8C, 'œ' to 0x9C, 'Ÿ' to 0x9F,
    '”' to 0x94, '•' to 0x95, '™' to 0x99, '‰' to 0x89, '›' to 0x9B, '‹' to 0x8B,
)

private fun winAnsiCode(c: Char): Int? =
    WIN_ANSI_SPECIALS[c] ?: c.code.takeIf { it in ASCII_PRINTABLE || it in LATIN1_HIGH }

/**
 * Maps text to WinAnsi bytes; Latin-1 as is, listed typographic characters mapped, other letters by their base letter
 * (`č` -> `c`), everything else (including a surrogate pair such as an emoji) becomes a single `?`.
 */
internal fun winAnsiBytes(text: String): ByteArray {
    val out = ByteArrayOutputStream()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val code = winAnsiCode(c)
            ?: Normalizer.normalize(c.toString(), Normalizer.Form.NFD).let { winAnsiCode(it[0]) }
            ?: UNKNOWN
        out.write(code)
        i += if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) 2 else 1
    }
    return out.toByteArray()
}

/**
 * Minimal single-page PDF 1.4 writer. Coordinates have a top-left origin; [text] y is the baseline.
 * Colors are `0xAARRGGBB` values, but the alpha is ignored: every color is drawn opaque.
 */
class PdfWriter(private val widthPt: Float, private val heightPt: Float) {
    private val content = StringBuilder()
    private val images = mutableListOf<RgbaImage>()

    private fun n(v: Float): String = String.format(Locale.ROOT, "%.2f", v).trimEnd('0').trimEnd('.')

    private fun color(argb: Long): String {
        val parts = (2 downTo 0).map { ((argb shr it * BITS_PER_BYTE).toInt() and BYTE_MASK) / BYTE_MAX }
        return parts.joinToString(" ", postfix = " rg") { String.format(Locale.ROOT, "%.3f", it) }
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float, argb: Long) {
        content.append("${color(argb)} ${n(x)} ${n(heightPt - y - h)} ${n(w)} ${n(h)} re f\n")
    }

    @Suppress("LongParameterList") // signature fixed by the notice layout API
    fun text(x: Float, y: Float, text: String, sizePt: Float, bold: Boolean, argb: Long) {
        val escaped = StringBuilder()
        for (b in winAnsiBytes(text)) {
            val v = b.toInt() and BYTE_MASK
            when {
                v == '('.code || v == ')'.code || v == '\\'.code -> escaped.append('\\').append(v.toChar())
                v < ASCII_LIMIT -> escaped.append(v.toChar())
                else -> escaped.append('\\').append(v.toString(OCTAL_RADIX).padStart(OCTAL_DIGITS, '0'))
            }
        }
        content.append("BT /${if (bold) "F2" else "F1"} ${n(sizePt)} Tf ${color(argb)} ${n(x)} ${n(heightPt - y)} Td ")
            .append("($escaped) Tj ET\n")
    }

    fun image(x: Float, y: Float, w: Float, h: Float, image: RgbaImage) {
        images += image
        content.append("q ${n(w)} 0 0 ${n(h)} ${n(x)} ${n(heightPt - y - h)} cm /Im${images.size} Do Q\n")
    }

    private fun deflate(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    fun toByteArray(): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        fun obj(dict: String, stream: ByteArray? = null) {
            offsets += out.size()
            out.write("${offsets.size} 0 obj\n$dict".toByteArray(Charsets.ISO_8859_1))
            if (stream != null) {
                out.write("\nstream\n".toByteArray())
                out.write(stream)
                out.write("\nendstream".toByteArray())
            }
            out.write("\nendobj\n".toByteArray())
        }
        out.write("%PDF-1.4\n".toByteArray())
        // Image objects follow the six fixed ones: each image takes 1 object, plus 1 more with alpha.
        val imageRefs = mutableListOf<Int>()
        var next = FIXED_OBJECTS + 1
        for (img in images) {
            imageRefs += next
            next += if (img.alpha != null) 2 else 1
        }
        val xObjects = imageRefs.withIndex().joinToString(" ") { (i, ref) -> "/Im${i + 1} $ref 0 R" }
        obj("<< /Type /Catalog /Pages 2 0 R >>")
        obj("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        obj(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${n(widthPt)} ${n(heightPt)}] /Contents 4 0 R " +
                "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> /XObject << $xObjects >> >> >>",
        )
        val body = deflate(content.toString().toByteArray(Charsets.ISO_8859_1))
        obj("<< /Length ${body.size} /Filter /FlateDecode >>", body)
        listOf("Helvetica", "Helvetica-Bold").forEach {
            obj("<< /Type /Font /Subtype /Type1 /BaseFont /$it /Encoding /WinAnsiEncoding >>")
        }
        images.forEachIndexed { i, img ->
            val alpha = img.alpha
            val smask = if (alpha != null) " /SMask ${imageRefs[i] + 1} 0 R" else ""
            val dims = "/Width ${img.width} /Height ${img.height} /BitsPerComponent 8 /Filter /FlateDecode"
            val rgb = deflate(img.rgb)
            obj("<< /Type /XObject /Subtype /Image $dims /ColorSpace /DeviceRGB$smask /Length ${rgb.size} >>", rgb)
            if (alpha != null) {
                val a = deflate(alpha)
                obj("<< /Type /XObject /Subtype /Image $dims /ColorSpace /DeviceGray /Length ${a.size} >>", a)
            }
        }
        val xref = out.size()
        val table = StringBuilder("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { table.append(String.format(Locale.ROOT, "%010d 00000 n \n", it)) }
        table.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        out.write(table.toString().toByteArray())
        return out.toByteArray()
    }

    private companion object {
        const val FIXED_OBJECTS = 6
    }
}
