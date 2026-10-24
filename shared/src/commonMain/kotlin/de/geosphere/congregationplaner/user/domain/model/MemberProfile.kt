package de.geosphere.congregationplaner.user.domain.model

/**
 * Persönliche Angaben eines Mitglieds, die nur Versammlungs- und globale Administratoren pflegen.
 * Die Feldnamen entsprechen den Firestore-Feldern im Mitgliedsdokument.
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
) {
    val fullName: String get() = listOf(firstName, lastName).filter(String::isNotBlank).joinToString(" ")

    val canHaveAppointment: Boolean get() = category == MemberCategory.BROTHER && baptized

    val canBeSpeaker: Boolean get() = category == MemberCategory.BROTHER

    /** Kinder sind nie getauft, und ohne passende Voraussetzungen gibt es kein Dienstamt und keinen Redner. */
    fun normalized(): MemberProfile {
        val baptized = baptized && category != MemberCategory.CHILD
        val profile = copy(baptized = baptized, isSpeaker = isSpeaker && canBeSpeaker)
        return if (profile.canHaveAppointment) profile else profile.copy(appointment = Appointment.NONE)
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 30

        /** Ziffern, Leerzeichen und die üblichen Trennzeichen; entspricht der Prüfung in den Security Rules. */
        private val PHONE_PATTERN = Regex("^[0-9+ ()/-]*$")

        fun isValidPhone(phone: String): Boolean = phone.length <= MAX_PHONE_LENGTH && PHONE_PATTERN.matches(phone)
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

    class AccessWithoutAccount :
        MemberProfileException(
            "Personen ohne Konto können nicht freigegeben werden und haben keine Rolle oder Berechtigungen.",
        )
}
