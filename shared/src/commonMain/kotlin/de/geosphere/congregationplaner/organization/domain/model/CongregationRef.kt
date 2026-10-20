package de.geosphere.congregationplaner.organization.domain.model

/**
 * Vollständiger Pfad einer Versammlung in der Organisationsstruktur. Da die Nutzer einer Versammlung
 * unterhalb ihres Dokuments liegen, muss die Versammlung vor der Anmeldung feststehen.
 */
data class CongregationRef(
    val regionId: String,
    val subregionId: String,
    val branchId: String,
    val circuitId: String,
    val congregationId: String,
) {
    /** Die IDs aller Ebenen, von [OrgLevel.REGION] bis [OrgLevel.CONGREGATION]. */
    val ids: List<String> get() = listOf(regionId, subregionId, branchId, circuitId, congregationId)

    val documentPath: String get() = "${OrgLevel.CONGREGATION.collectionPath(ids.dropLast(1))}/$congregationId"

    val membersPath: String get() = "$documentPath/$MEMBERS_SUBCOLLECTION"

    /** Link, den ein Versammlungsadmin weitergeben kann, damit neue Nutzer die Versammlung nicht suchen müssen. */
    fun toInviteLink(): String = "$INVITE_LINK_PREFIX$documentPath"

    companion object {
        const val INVITE_LINK_PREFIX = "congregationplaner://join/"
        const val MEMBERS_SUBCOLLECTION = "members"

        fun fromIds(ids: List<String>): CongregationRef? {
            if (ids.size != OrgLevel.entries.size || !ids.all(::isValidSegment)) return null
            return CongregationRef(ids[0], ids[1], ids[2], ids[3], ids[4])
        }

        /**
         * Liest einen Einladungslink oder einen reinen Versammlungspfad
         * (`region/…/congregation/{id}`); liefert `null`, wenn die Eingabe keine Versammlung beschreibt.
         */
        fun parse(input: String): CongregationRef? {
            val path = input.trim().removePrefix(INVITE_LINK_PREFIX).trim('/')
            val segments = path.split('/')
            if (segments.size != OrgLevel.entries.size * 2) return null
            val pairs = segments.chunked(2)
            if (pairs.map { it[0] } != OrgLevel.entries.map { it.collection }) return null
            return fromIds(pairs.map { it[1] })
        }

        // Firestore-IDs dürfen keinen Schrägstrich enthalten und nicht `.` oder `..` sein.
        private fun isValidSegment(id: String): Boolean =
            id.isNotBlank() && id != "." && id != ".." && '/' !in id && id.none(Char::isWhitespace)
    }
}

/** Die im Login gewählte Versammlung samt Anzeigename. */
data class SelectedCongregation(val ref: CongregationRef, val name: String) {
    val displayName: String get() = name.ifBlank { ref.congregationId }
}
