package de.geosphere.congregationplaner.user.presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import de.geosphere.congregationplaner.user.domain.model.formatPhoneNumber

/**
 * Zeigt eine Telefonnummer beim Tippen so an, wie sie gespeichert wird ([formatPhoneNumber]); der eingegebene Text
 * selbst bleibt unverändert, damit der Cursor nicht springt.
 */
internal object PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val shown = phoneDisplay(raw)
        if (shown == raw) return TransformedText(text, OffsetMapping.Identity)
        val rawToShown = rawToShownOffsets(raw, shown)
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = rawToShown[offset.coerceIn(0, raw.length)]

                override fun transformedToOriginal(offset: Int): Int =
                    rawToShown.indexOfLast { it <= offset.coerceIn(0, shown.length) }.coerceAtLeast(0)
            },
        )
    }
}

/** Die Anzeige zu [raw]: die formatierte Nummer, wenn sie [raw] nur um Leerzeichen ergänzt, sonst [raw] selbst. */
internal fun phoneDisplay(raw: String): String {
    val formatted = formatPhoneNumber(raw)
    return formatted.takeIf { it.length >= raw.length && it.replace(" ", "") == raw.replace(" ", "") } ?: raw
}

/** Für jede Position in [raw] (0..Länge) die Position in [shown], das [raw] mit eingefügten Leerzeichen ist. */
internal fun rawToShownOffsets(raw: String, shown: String): IntArray {
    val offsets = IntArray(raw.length + 1)
    var j = 0
    for (i in raw.indices) {
        while (j < shown.length && shown[j] != raw[i]) j++
        offsets[i] = j
        j++
    }
    offsets[raw.length] = shown.length
    return offsets
}
