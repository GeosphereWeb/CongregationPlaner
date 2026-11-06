package de.geosphere.congregationplaner.congregation.domain.model

/**
 * Der Name einer Versammlung ist pro Kreis eindeutig. Die Eindeutigkeit erzwingt das Namensdokument
 * `congregationNames/{key}` unter dem Kreis (Firestore-Regeln); [key] bildet die Regel nach.
 */
object CongregationName {
    const val MAX_LENGTH = 100

    private val blanks = Regex("\\s+")

    /** Schlüssel des Namens: getrimmt, klein geschrieben, Leerzeichen zusammengezogen. */
    fun key(name: String): String = name.trim().lowercase().replace(blanks, " ")

    /** Der Fehler eines ungültigen Namens oder `null`, wenn er passt. */
    fun validate(name: String): CongregationException? = when {
        name.isBlank() -> CongregationException.MissingName()
        name.trim().length > MAX_LENGTH -> CongregationException.NameTooLong()
        '/' in name -> CongregationException.InvalidName()
        else -> null
    }
}

/** Fachliche Fehler beim Anlegen und Umbenennen; die Nachrichten werden direkt in der UI angezeigt. */
sealed class CongregationException(message: String) : Exception(message) {
    class MissingName : CongregationException("Bitte gib einen Versammlungsnamen ein.")

    class NameTooLong :
        CongregationException("Der Versammlungsname darf höchstens ${CongregationName.MAX_LENGTH} Zeichen lang sein.")

    class InvalidName : CongregationException("Der Versammlungsname darf keinen Schrägstrich enthalten.")

    class NameTaken : CongregationException("In diesem Kreis gibt es schon eine Versammlung mit diesem Namen.")

    class EmailNotVerified :
        CongregationException("Bitte bestätige zuerst deine E-Mail-Adresse, bevor du eine Versammlung anlegst.")
}

/** Der Nutzer, der mit einer neuen Versammlung zu deren erstem, freigegebenen Admin wird. */
data class NewCongregationAdmin(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val firstName: String,
    val lastName: String,
    val emailVerified: Boolean,
)
