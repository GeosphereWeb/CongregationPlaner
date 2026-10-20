package de.geosphere.congregationplaner.circuit.domain.model

/**
 * Ein Kreis (Circuit), dem mehrere Versammlungen zugeordnet sind.
 *
 * @param id Die Kreisnummer (z. B. `609a`); dient als Firestore-Dokument-ID und als Pfadsegment
 *   der Versammlungs-Collection und ist daher nach dem Anlegen nicht mehr änderbar.
 * @param overseerName Name des Kreisaufsehers; optional.
 */
data class Circuit(val id: String = "", val name: String = "", val overseerName: String = "") {
    companion object {
        /** Kleinbuchstaben, Ziffern und Bindestriche, damit die ID als Firestore-Pfadsegment taugt. */
        private val ID_PATTERN = Regex("^[a-z0-9](?:[a-z0-9-]{0,30}[a-z0-9])?$")

        fun normalizeId(id: String): String = id.trim().lowercase()

        fun isValidId(id: String): Boolean = ID_PATTERN.matches(id)
    }
}
