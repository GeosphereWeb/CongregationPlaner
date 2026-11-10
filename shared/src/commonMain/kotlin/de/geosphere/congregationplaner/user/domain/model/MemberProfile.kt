package de.geosphere.congregationplaner.user.domain.model

/**
 * Persönliche Angaben eines Mitglieds im Mitgliedsdokument (ADR-0016). Die Kontaktangaben (Namen, Telefon, E-Mail,
 * Adresse, Notfallkontakte) pflegt das Mitglied selbst, alles pflegt der Versammlungsadmin; lesen dürfen sie das
 * Mitglied, die Admins und die Planer. Die Feldnamen entsprechen den Firestore-Feldern.
 *
 * @param category `null`, solange der Admin die Angabe noch nicht gepflegt hat (z. B. nach der Selbstregistrierung).
 * @param appointment Nur getaufte Brüder können ein Dienstamt haben, siehe [canHaveAppointment].
 * @param isSpeaker `true` für Vortragsredner; solche Mitglieder können in der Vortragsplanung ausgewählt werden.
 *   Nur Brüder können Redner sein, siehe [canBeSpeaker].
 */
data class MemberProfile(
    val lastName: String = "",
    val firstName: String = "",
    val category: MemberCategory? = null,
    val baptized: Boolean = false,
    val appointment: Appointment = Appointment.NONE,
    val mobilePhone: String = "",
    val landlinePhone: String = "",
    val isSpeaker: Boolean = false,
    val emails: List<String> = emptyList(),
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
) {
    val fullName: String get() = listOf(firstName, lastName).filter(String::isNotBlank).joinToString(" ")

    val canHaveAppointment: Boolean get() = category == MemberCategory.BROTHER && baptized

    val canBeSpeaker: Boolean get() = category == MemberCategory.BROTHER

    /** Vorsitzende kommen aus den Ältesten und Dienstamtgehilfen, siehe [canHaveAppointment]. */
    val canBeChairman: Boolean get() = canHaveAppointment && appointment != Appointment.NONE

    /** Übernimmt die Felder, die das Mitglied selbst pflegt, aus [form]; die Angaben des Admins bleiben. */
    fun withOwnFieldsOf(form: MemberProfile): MemberProfile = copy(
        lastName = form.lastName,
        firstName = form.firstName,
        mobilePhone = form.mobilePhone,
        landlinePhone = form.landlinePhone,
        emails = form.emails,
        street = form.street,
        postalCode = form.postalCode,
        city = form.city,
        emergencyContacts = form.emergencyContacts,
    )

    /** Kinder sind nie getauft, und ohne passende Voraussetzungen gibt es kein Dienstamt und keinen Redner. */
    fun normalized(): MemberProfile {
        val baptized = baptized && category != MemberCategory.CHILD
        val profile = copy(baptized = baptized, isSpeaker = isSpeaker && canBeSpeaker)
        return if (profile.canHaveAppointment) profile else profile.copy(appointment = Appointment.NONE)
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 30
        const val MAX_EMAILS = 3
        const val MAX_EMAIL_LENGTH = 254
        const val MAX_STREET_LENGTH = 100
        const val MAX_POSTAL_CODE_LENGTH = 10
        const val MAX_CITY_LENGTH = 100
        const val MAX_CONTACT_NAME_LENGTH = 100
        const val MAX_RELATION_LENGTH = 50
        const val MAX_EMERGENCY_CONTACTS = 5
        const val MAX_PHONES_PER_CONTACT = 3

        /** Ziffern, Leerzeichen und die üblichen Trennzeichen; entspricht der Prüfung in den Security Rules. */
        private val PHONE_PATTERN = Regex("^[0-9+ ()/-]*$")

        fun isValidPhone(phone: String): Boolean = phone.length <= MAX_PHONE_LENGTH && PHONE_PATTERN.matches(phone)

        /** Grobe Prüfung wie in den Security Rules: etwas@etwas.etwas, ohne Leerzeichen. */
        private val EMAIL_PATTERN = Regex("^[^@ ]+@[^@ ]+[.][^@ ]+$")

        fun isValidEmail(email: String): Boolean = email.length <= MAX_EMAIL_LENGTH && EMAIL_PATTERN.matches(email)
    }
}

/** Werte des Firestore-Felds `category`. */
enum class MemberCategory(val label: String) {
    BROTHER("Bruder"),
    SISTER("Schwester"),
    CHILD("Kind"),
    ;

    companion object {
        fun fromValue(value: String?): MemberCategory? = entries.firstOrNull { it.name == value }
    }
}

/** Werte des Firestore-Felds `appointment`. */
enum class Appointment(val label: String) {
    NONE("Kein Dienstamt"),
    MINISTERIAL_SERVANT("Dienstamtgehilfe"),
    ELDER("Ältester"),
    ;

    companion object {
        /** Unbekannte Werte werden bewusst auf [NONE] abgebildet. */
        fun fromValue(value: String?): Appointment = entries.firstOrNull { it.name == value } ?: NONE
    }
}

/** Fachliche Fehler beim Pflegen der Mitgliedsangaben; die Nachrichten werden direkt in der UI angezeigt. */
sealed class MemberProfileException(message: String) : Exception(message) {
    class NotAllowed : MemberProfileException("Dafür fehlt dir die Berechtigung.")

    class MissingName : MemberProfileException("Bitte gib Vor- und Familiennamen ein.")

    class NameTooLong :
        MemberProfileException(
            "Vor- und Familienname dürfen höchstens ${MemberProfile.MAX_NAME_LENGTH} Zeichen lang sein.",
        )

    class BaptizedChild : MemberProfileException("Ein Kind kann nicht als getauft erfasst werden.")

    class AppointmentNotAllowed : MemberProfileException("Ein Dienstamt ist nur bei getauften Brüdern möglich.")

    class SpeakerNotAllowed : MemberProfileException("Ein Redner kann nur ein Bruder sein.")

    class InvalidPhone :
        MemberProfileException(
            "Telefonnummern dürfen nur Ziffern, Leerzeichen und + ( ) / - enthalten " +
                "(max. ${MemberProfile.MAX_PHONE_LENGTH} Zeichen).",
        )

    class InvalidEmail : MemberProfileException("Bitte gib gültige E-Mail-Adressen ein (z. B. name@beispiel.de).")

    class TooManyEmails :
        MemberProfileException("Es sind höchstens ${MemberProfile.MAX_EMAILS} zusätzliche E-Mail-Adressen möglich.")

    class AddressTooLong :
        MemberProfileException(
            "Straße und Ort dürfen höchstens ${MemberProfile.MAX_STREET_LENGTH} Zeichen, die Postleitzahl " +
                "höchstens ${MemberProfile.MAX_POSTAL_CODE_LENGTH} Zeichen lang sein.",
        )

    class TooManyContacts :
        MemberProfileException("Es sind höchstens ${MemberProfile.MAX_EMERGENCY_CONTACTS} Notfallkontakte möglich.")

    class IncompleteContact :
        MemberProfileException("Jeder Notfallkontakt braucht einen Namen und mindestens eine Telefonnummer.")

    class TooManyPhones :
        MemberProfileException(
            "Ein Notfallkontakt hat höchstens ${MemberProfile.MAX_PHONES_PER_CONTACT} Telefonnummern.",
        )

    class InvalidContact :
        MemberProfileException(
            "Name höchstens ${MemberProfile.MAX_CONTACT_NAME_LENGTH}, Beziehung höchstens " +
                "${MemberProfile.MAX_RELATION_LENGTH} Zeichen; die Telefonnummer nur Ziffern, Leerzeichen " +
                "und + ( ) / - (höchstens ${MemberProfile.MAX_PHONE_LENGTH} Zeichen).",
        )

    class AccessWithoutAccount :
        MemberProfileException(
            "Personen ohne Konto können nicht freigegeben werden und haben keine Rolle oder Berechtigungen.",
        )
}
