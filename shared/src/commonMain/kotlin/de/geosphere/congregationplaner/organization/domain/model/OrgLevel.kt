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

/** Eine Einheit der Organisationsstruktur (Region, Subregion, …, Versammlung). */
data class OrgUnit(val id: String, val name: String = "") {
    val displayName: String get() = name.ifBlank { id }
}
