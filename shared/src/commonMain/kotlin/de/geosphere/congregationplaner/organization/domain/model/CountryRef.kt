package de.geosphere.congregationplaner.organization.domain.model

/** Pfad eines Landes in der Organisationsstruktur; unter ihm liegen Vortragsliste und Wochenereignisse (ADR-0034). */
data class CountryRef(val regionId: String, val subregionId: String, val branchId: String, val countryId: String) {
    val ids: List<String> get() = listOf(regionId, subregionId, branchId, countryId)

    val documentPath: String get() = "${OrgLevel.COUNTRY.collectionPath(ids.dropLast(1))}/$countryId"

    companion object {
        fun fromIds(ids: List<String>): CountryRef? {
            if (ids.size != OrgLevel.COUNTRY.ordinal + 1) return null
            val id = ids.iterator()
            return CountryRef(id.next(), id.next(), id.next(), id.next())
        }
    }
}

/** Ein Land zur Auswahl, etwa beim Pflegen der Vortragsliste durch den globalen Admin. */
data class CountryOption(val ref: CountryRef, val name: String) {
    val displayName: String get() = name.ifBlank { ref.countryId }
}

/**
 * Das Land, das zuerst ausgewählt ist: das der eigenen Versammlung, sonst das einzige vorhandene; bei mehreren ohne
 * eigene Versammlung muss der Nutzer wählen.
 */
fun List<CountryOption>.preferredFor(congregation: CongregationRef?): CountryRef? =
    firstOrNull { it.ref == congregation?.country }?.ref ?: singleOrNull()?.ref
