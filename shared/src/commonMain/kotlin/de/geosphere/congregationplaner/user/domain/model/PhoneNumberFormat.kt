package de.geosphere.congregationplaner.user.domain.model

/**
 * Formatiert eine deutsche Telefonnummer national gruppiert: Vorwahl und Rufnummer durch ein Leerzeichen getrennt,
 * z. B. „0170 1234567“, „030 123456“, „0201 123456“ oder „+49 170 1234567“.
 *
 * Handynummern (015/016/017) werden nach vier Ziffern getrennt, Festnetznummern nach der Ortsnetzkennzahl aus dem
 * Vorwahlverzeichnis der Bundesnetzagentur ([GERMAN_AREA_CODES]); passen mehrere, gilt die längste (z. B. 02129 Haan
 * vor 0212 Solingen). Andere Nummern (Sonderrufnummern, Ausland außer +49) bleiben unverändert.
 *
 * Nur Eingaben aus Ziffern (optional mit führendem „+“) werden umgeformt; enthält die Eingabe schon Trennzeichen,
 * hat sie der Nutzer selbst gesetzt und sie bleibt, wie sie ist. Wird die Nummer dadurch länger als erlaubt, bleibt
 * sie ebenfalls unverändert.
 */
fun formatPhoneNumber(raw: String): String {
    val input = raw.trim()
    if (!PLAIN_NUMBER.matches(input)) return input
    val formatted = when {
        input.startsWith(INTERNATIONAL_PREFIX) -> formatNational("0" + input.drop(INTERNATIONAL_PREFIX.length))
            ?.let { "$INTERNATIONAL_PREFIX ${it.drop(1)}" }
        input.startsWith("0") -> formatNational(input)
        else -> null
    }
    return formatted?.takeIf { it.length <= MemberProfile.MAX_PHONE_LENGTH } ?: input
}

private const val INTERNATIONAL_PREFIX = "+49"
private val PLAIN_NUMBER = Regex("^\\+?[0-9]+$")
private val MOBILE_PREFIXES = listOf("015", "016", "017")
private const val MOBILE_PREFIX_LENGTH = 4
private const val MIN_AREA_CODE_LENGTH = 2
private const val MAX_AREA_CODE_LENGTH = 5

/** „01701234567“ → „0170 1234567“, „0201123456“ → „0201 123456“; `null` ohne erkennbare Vorwahl. */
private fun formatNational(number: String): String? {
    val prefixLength = if (MOBILE_PREFIXES.any(number::startsWith)) MOBILE_PREFIX_LENGTH else areaCodeLength(number)
    if (prefixLength == null || number.length <= prefixLength) return null
    return "${number.take(prefixLength)} ${number.drop(prefixLength)}"
}

/** Länge der längsten passenden Ortsnetzkennzahl einschließlich der führenden 0, sonst `null`. */
private fun areaCodeLength(number: String): Int? {
    val digits = number.drop(1)
    return (MAX_AREA_CODE_LENGTH downTo MIN_AREA_CODE_LENGTH)
        .firstOrNull { length -> digits.length > length && digits.take(length) in GERMAN_AREA_CODES }
        ?.plus(1)
}
