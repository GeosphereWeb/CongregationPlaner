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

/**
 * Die Nummer international mit „+“ ohne Trennzeichen („0170 1234567“ → „+491701234567“, „0043 660 1“ → „+436601“);
 * `null`, wenn sie keine Vorwahl hat oder zu kurz ist.
 */
fun internationalNumber(raw: String): String? {
    val compact = raw.filter { it.isDigit() || it == '+' }
    val digits = compact.filter(Char::isDigit)
    val number = when {
        compact.startsWith("00") -> digits.drop(2)
        compact.startsWith(INTERNATIONAL_PREFIX) || compact.startsWith("+") -> digits
        compact.startsWith("0") -> "49" + digits.drop(1)
        else -> return null
    }
    return number.takeIf { it.length >= MIN_INTERNATIONAL_DIGITS }?.let { "+$it" }
}

private const val MIN_INTERNATIONAL_DIGITS = 8

/** Art einer Telefonnummer, erkannt an der Vorwahl. */
enum class PhoneKind {
    MOBILE,
    LANDLINE,
    UNKNOWN,
}

/** Handy (015/016/017), Festnetz (bekannte Ortsnetzkennzahl) oder unbekannt; Trennzeichen und +49 sind erlaubt. */
fun phoneKind(raw: String): PhoneKind {
    val national = nationalDigits(raw) ?: return PhoneKind.UNKNOWN
    return when {
        MOBILE_PREFIXES.any(national::startsWith) -> PhoneKind.MOBILE
        areaCodeLength(national) != null -> PhoneKind.LANDLINE
        else -> PhoneKind.UNKNOWN
    }
}

/** Der Ortsnetzname zur Vorwahl einer Festnetznummer, z. B. „Essen“ für „0201 123456“; sonst `null`. */
fun areaName(raw: String): String? {
    if (phoneKind(raw) != PhoneKind.LANDLINE) return null
    val national = nationalDigits(raw) ?: return null
    val length = areaCodeLength(national) ?: return null
    return GERMAN_AREA_NAMES[national.substring(1, length)]
}

/** Die Nummer national ohne Trennzeichen („+49 201 1“ → „02011“); `null`, wenn sie nicht deutsch beginnt. */
private fun nationalDigits(raw: String): String? {
    val compact = raw.filter { it.isDigit() || it == '+' }
    val digits = compact.filter(Char::isDigit)
    return when {
        compact.startsWith(INTERNATIONAL_PREFIX) -> "0" + digits.drop(INTERNATIONAL_PREFIX.length - 1)
        compact.startsWith("0") -> digits
        else -> null
    }
}
