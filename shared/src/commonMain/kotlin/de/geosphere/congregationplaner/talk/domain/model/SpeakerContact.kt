package de.geosphere.congregationplaner.talk.domain.model

import de.geosphere.congregationplaner.user.domain.model.internationalNumber

/**
 * Wie sich ein Redner erreichen lässt: Handy und Festnetz lassen sich je einzeln anrufen, WhatsApp gibt es nur mit
 * einer Handynummer. Eine Nummer, die sich nicht wählen lässt (keine Vorwahl, zu kurz), zählt als nicht vorhanden.
 */
data class SpeakerContact(val mobilePhone: String = "", val landlinePhone: String = "") {
    /** `tel:`-Link der Handynummer; `null` ohne wählbare Handynummer. */
    val mobileCallUri: String? get() = callUri(mobilePhone)

    /** `tel:`-Link der Festnetznummer; `null` ohne wählbare Festnetznummer. */
    val landlineCallUri: String? get() = callUri(landlinePhone)

    /** `wa.me`-Link der Handynummer; `null` ohne wählbare Handynummer. */
    val whatsAppUri: String? get() = internationalNumber(mobilePhone)?.let { "https://wa.me/${it.removePrefix("+")}" }

    /** `true`, wenn sich der Redner anrufen oder per WhatsApp erreichen lässt. */
    val isReachable: Boolean get() = mobileCallUri != null || landlineCallUri != null

    /** `wa.me`-Link mit vorbelegtem [text]; `null` ohne wählbare Handynummer. */
    fun whatsAppUri(text: String): String? = whatsAppUri?.let { "$it?text=${percentEncode(text)}" }

    private fun callUri(raw: String): String? = internationalNumber(raw)?.let { "tel:$it" }
}

private const val BYTE_MASK = 0xFF
private const val ASCII_LIMIT = 0x80
private const val HEX_RADIX = 16

// RFC 3986: unreservierte Zeichen bleiben, alles andere wird als UTF-8 kodiert (Leerzeichen als %20).
private fun percentEncode(text: String): String = buildString {
    text.encodeToByteArray().forEach { byte ->
        val code = byte.toInt() and BYTE_MASK
        val char = code.toChar()
        if (code < ASCII_LIMIT && (char.isLetterOrDigit() || char in "-._~")) {
            append(char)
        } else {
            append('%').append(code.toString(HEX_RADIX).uppercase().padStart(2, '0'))
        }
    }
}
