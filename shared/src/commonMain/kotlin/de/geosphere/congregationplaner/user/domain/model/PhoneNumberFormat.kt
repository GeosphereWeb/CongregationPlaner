package de.geosphere.congregationplaner.user.domain.model

/**
 * Formatiert eine deutsche Telefonnummer national gruppiert: Vorwahl und Rufnummer durch ein Leerzeichen getrennt,
 * z. B. „0170 1234567“, „030 123456“ oder „+49 170 1234567“.
 *
 * shortcut: Getrennt wird nur, wo die Vorwahl sicher erkennbar ist (Handy 015/016/017 und die zweistelligen
 * Ortsnetze 030, 040, 069, 089); andere Nummern bleiben unverändert. Für alle Ortsnetze braucht es eine
 * Vorwahl-Liste (z. B. libphonenumber), sobald es eine KMP-taugliche gibt.
 *
 * Nur Eingaben aus Ziffern (optional mit führendem „+“) werden umgeformt; enthält die Eingabe schon Trennzeichen,
 * hat sie der Nutzer selbst gesetzt und sie bleibt, wie sie ist. Wird die Nummer dadurch länger als erlaubt, bleibt
 * sie ebenfalls unverändert.
 */
fun formatPhoneNumber(raw: String): String {
    val input = raw.trim()
    if (!PLAIN_NUMBER.matches(input)) return input
    val formatted = when {
        input.startsWith("+49") -> formatNational("0" + input.drop(INTERNATIONAL_PREFIX.length))
            ?.let { "$INTERNATIONAL_PREFIX ${it.drop(1)}" }
        input.startsWith("0") -> formatNational(input)
        else -> null
    }
    return formatted?.takeIf { it.length <= MemberProfile.MAX_PHONE_LENGTH } ?: input
}

private const val INTERNATIONAL_PREFIX = "+49"
private val PLAIN_NUMBER = Regex("^\\+?[0-9]+$")
private val MOBILE_PREFIXES = listOf("015", "016", "017")
private val SHORT_AREA_CODES = listOf("030", "040", "069", "089")
private const val MOBILE_PREFIX_LENGTH = 4
private const val SHORT_AREA_CODE_LENGTH = 3

/** „01701234567“ → „0170 1234567“; `null`, wenn die Vorwahl nicht sicher erkennbar ist. */
private fun formatNational(number: String): String? {
    val prefixLength = when {
        MOBILE_PREFIXES.any(number::startsWith) -> MOBILE_PREFIX_LENGTH
        SHORT_AREA_CODES.any(number::startsWith) -> SHORT_AREA_CODE_LENGTH
        else -> return null
    }
    if (number.length <= prefixLength) return null
    return "${number.take(prefixLength)} ${number.drop(prefixLength)}"
}
