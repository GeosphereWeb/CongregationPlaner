package de.geosphere.congregationplaner.circuit.domain.model

/** Fachliche Fehler der Kreisverwaltung; die Nachrichten werden direkt in der UI angezeigt. */
sealed class CircuitException(message: String) : Exception(message) {
    class InvalidId :
        CircuitException(
            "Die Kreisnummer darf nur Kleinbuchstaben, Ziffern und Bindestriche enthalten (max. 32 Zeichen).",
        )

    class MissingName : CircuitException("Bitte gib einen Kreisnamen ein.")

    class AlreadyExists(id: String) : CircuitException("Ein Kreis mit der Nummer „$id“ existiert bereits.")

    class HasCongregations(id: String) :
        CircuitException(
            "Der Kreis „$id“ enthält noch Versammlungen und kann nicht gelöscht werden.",
        )
}
