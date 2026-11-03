package de.geosphere.congregationplaner.user.domain.model

/** Ein Notfallkontakt; [relation] (z. B. „Ehefrau“) ist optional. */
data class EmergencyContact(val name: String = "", val relation: String = "", val phone: String = "")

/**
 * Private Angaben eines Mitglieds, getrennt vom Mitgliedsdokument (ADR-0015): lesen nur das Mitglied selbst und die
 * Versammlungsadmins, ändern nur das Mitglied.
 */
data class MemberPrivateDetails(
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
) {
    companion object {
        const val MAX_STREET_LENGTH = 100
        const val MAX_POSTAL_CODE_LENGTH = 10
        const val MAX_CITY_LENGTH = 100
        const val MAX_CONTACT_NAME_LENGTH = 100
        const val MAX_RELATION_LENGTH = 50
        const val MAX_EMERGENCY_CONTACTS = 5
    }
}

/** Fachliche Fehler bei den privaten Angaben; die Nachrichten werden direkt in der UI angezeigt. */
sealed class MemberPrivateException(message: String) : Exception(message) {
    class AddressTooLong :
        MemberPrivateException(
            "Straße und Ort dürfen höchstens ${MemberPrivateDetails.MAX_STREET_LENGTH} Zeichen, die Postleitzahl " +
                "höchstens ${MemberPrivateDetails.MAX_POSTAL_CODE_LENGTH} Zeichen lang sein.",
        )

    class TooManyContacts :
        MemberPrivateException(
            "Es sind höchstens ${MemberPrivateDetails.MAX_EMERGENCY_CONTACTS} Notfallkontakte möglich.",
        )

    class IncompleteContact : MemberPrivateException("Jeder Notfallkontakt braucht einen Namen und eine Telefonnummer.")

    class InvalidContact :
        MemberPrivateException(
            "Name höchstens ${MemberPrivateDetails.MAX_CONTACT_NAME_LENGTH}, Beziehung höchstens " +
                "${MemberPrivateDetails.MAX_RELATION_LENGTH} Zeichen; die Telefonnummer nur Ziffern, Leerzeichen " +
                "und + ( ) / - (höchstens ${MemberProfile.MAX_PHONE_LENGTH} Zeichen).",
        )
}
