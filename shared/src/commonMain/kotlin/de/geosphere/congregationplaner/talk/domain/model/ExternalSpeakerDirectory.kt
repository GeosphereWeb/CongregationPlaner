package de.geosphere.congregationplaner.talk.domain.model

/**
 * Ein Kreis aus dem Verzeichnis externer Redner (`…/congregation/{id}/externalCircuits/{id}`). Das Verzeichnis pflegt
 * der Planer selbst; es hat nichts mit der Organisationsstruktur der App zu tun. [id] ist leer, solange der Kreis
 * noch nicht gespeichert ist.
 */
data class ExternalCircuit(val id: String = "", val name: String)

/** Eine Versammlung eines [ExternalCircuit] (`…/externalCongregations/{id}`). */
data class ExternalCongregation(val id: String = "", val circuitId: String, val name: String)

/**
 * Ein Redner einer [ExternalCongregation] (`…/visitingSpeakers/{id}`), der für die Vortragsplanung zur Verfügung steht.
 *
 * @param talkNumbers Nummern der Vorträge aus der Vortragsliste, die der Redner hält.
 * @param ranking Bewertung des Redners in Sternen, 0 (nicht bewertet) bis [MAX_RANKING].
 */
data class VisitingSpeaker(
    val id: String = "",
    val congregationId: String,
    val lastName: String,
    val firstName: String = "",
    val mobilePhone: String = "",
    val landlinePhone: String = "",
    val email: String = "",
    val talkNumbers: List<Int> = emptyList(),
    val ranking: Int = 0,
) {
    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 30
        const val MAX_EMAIL_LENGTH = 200
        const val MAX_TALKS = 200
        const val MAX_RANKING = 5
    }
}

/** Das ganze Verzeichnis, wie es die Verwaltung und der Redner-Picker anzeigen. */
data class ExternalDirectory(
    val circuits: List<ExternalCircuit> = emptyList(),
    val congregations: List<ExternalCongregation> = emptyList(),
    val speakers: List<VisitingSpeaker> = emptyList(),
)

sealed class ExternalDirectoryException(message: String) : Exception(message) {
    class NotAllowed : ExternalDirectoryException("Du darfst die externen Redner nicht bearbeiten.")

    class InvalidCircuit :
        ExternalDirectoryException(
            "Der Kreis ist ungültig: Name erforderlich, höchstens ${VisitingSpeaker.MAX_NAME_LENGTH} Zeichen.",
        )

    class InvalidCongregation :
        ExternalDirectoryException(
            "Die Versammlung ist ungültig: Name und Kreis erforderlich, Name mit höchstens " +
                "${VisitingSpeaker.MAX_NAME_LENGTH} Zeichen.",
        )

    class InvalidSpeaker :
        ExternalDirectoryException(
            "Der Redner ist ungültig: Familienname und Versammlung erforderlich, Namen mit höchstens " +
                "${VisitingSpeaker.MAX_NAME_LENGTH} Zeichen, Telefonnummern bis " +
                "${VisitingSpeaker.MAX_PHONE_LENGTH} und E-Mail bis ${VisitingSpeaker.MAX_EMAIL_LENGTH} Zeichen, " +
                "Bewertung 0 bis ${VisitingSpeaker.MAX_RANKING}.",
        )

    class CircuitNotEmpty : ExternalDirectoryException("Der Kreis hat noch Versammlungen. Entferne sie zuerst.")

    class CongregationNotEmpty : ExternalDirectoryException("Die Versammlung hat noch Redner. Entferne sie zuerst.")
}
