package de.geosphere.congregationplaner.organization.domain.model

/**
 * Ebenen der Organisationsstruktur in Firestore, von oben nach unten. [collection] ist der Name der
 * Collection, in der die Einheiten dieser Ebene unterhalb ihrer übergeordneten Einheit liegen.
 */
enum class OrgLevel(val collection: String, val label: String) {
    REGION("region", "Region"),
    SUBREGION("subregion", "Subregion"),
    BRANCH("branch", "Zweig"),
    CIRCUIT("circuit", "Kreis"),
    CONGREGATION("congregation", "Versammlung"),
    ;

    /** Die nächsttiefere Ebene oder `null` für [CONGREGATION]. */
    val child: OrgLevel? get() = entries.getOrNull(ordinal + 1)

    /** Region bis Kreis sind [OrgUnit]s; die Versammlung hat eigene Daten (`CongregationMetadata`). */
    val isOrgUnit: Boolean get() = this != CONGREGATION

    /**
     * Collection-Pfad dieser Ebene unterhalb der übergeordneten Einheiten [parentIds]
     * (von [REGION] abwärts), z. B. `region/EMEA/subregion` für [SUBREGION].
     */
    fun collectionPath(parentIds: List<String>): String {
        require(parentIds.size == ordinal) { "$name requires exactly $ordinal parent IDs." }
        val parentPath = entries.zip(parentIds).joinToString("/") { (level, id) -> "${level.collection}/$id" }
        return if (parentPath.isEmpty()) collection else "$parentPath/$collection"
    }
}

/**
 * Eine Einheit der Organisationsstruktur (Region, Subregion, …, Versammlung).
 *
 * @param id Von Firestore beim Anlegen erzeugt und danach unveränderlich, weil sie Pfadsegment ist; leer, solange die
 *   Einheit noch nicht gespeichert ist.
 * @param overseerName Der Kreisaufseher; nur bei Kreisen.
 */
data class OrgUnit(val id: String = "", val name: String = "", val overseerName: String = "") {
    val displayName: String get() = name.ifBlank { id }

    companion object {
        const val MAX_NAME_LENGTH = 100
    }
}

/** Fachliche Fehler beim Pflegen der Organisation; die Nachrichten werden direkt in der UI angezeigt. */
sealed class OrgUnitException(message: String) : Exception(message) {
    class MissingName : OrgUnitException("Bitte gib einen Namen ein.")

    class NameTooLong :
        OrgUnitException("Name und Kreisaufseher dürfen höchstens ${OrgUnit.MAX_NAME_LENGTH} Zeichen lang sein.")

    class HasChildren(name: String) :
        OrgUnitException("„$name“ enthält noch Einträge und kann erst gelöscht werden, wenn sie leer ist.")
}
