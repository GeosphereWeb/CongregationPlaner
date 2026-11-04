package de.geosphere.congregationplaner.organization.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createOrganizationRepository(): OrganizationRepository

expect fun createOrgUnitRepository(): OrgUnitRepository

/** Feldnamen der Organisationsdokumente in Firestore; den Kreisaufseher haben nur Kreise. */
internal const val ORG_UNIT_NAME_FIELD = "name"
internal const val ORG_UNIT_OVERSEER_FIELD = "overseer_name"

/** Die Felder, die für [level] gespeichert werden. */
internal fun OrgUnit.firestoreFields(level: OrgLevel): Map<String, String> = buildMap {
    put(ORG_UNIT_NAME_FIELD, name)
    if (level == OrgLevel.CIRCUIT) put(ORG_UNIT_OVERSEER_FIELD, overseerName)
}

/** Einfacher Schlüssel-Wert-Speicher der jeweiligen Plattform (SharedPreferences, Java Preferences, NSUserDefaults). */
interface KeyValueStorage {
    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun remove(key: String)
}

expect fun createKeyValueStorage(): KeyValueStorage

fun createCongregationSelectionStore(): CongregationSelectionStore =
    KeyValueCongregationSelectionStore(createKeyValueStorage())

internal class KeyValueCongregationSelectionStore(private val storage: KeyValueStorage) : CongregationSelectionStore {
    override fun load(): SelectedCongregation? {
        val ref = storage.getString(KEY_PATH)?.let(CongregationRef::fromDocumentPath) ?: return null
        return SelectedCongregation(ref, storage.getString(KEY_NAME).orEmpty())
    }

    override fun save(selection: SelectedCongregation) {
        storage.putString(KEY_PATH, selection.ref.documentPath)
        storage.putString(KEY_NAME, selection.name)
    }

    override fun clear() {
        storage.remove(KEY_PATH)
        storage.remove(KEY_NAME)
    }

    private companion object {
        const val KEY_PATH = "selected_congregation_path"
        const val KEY_NAME = "selected_congregation_name"
    }
}

/** Fallback, falls die Plattform keinen dauerhaften Speicher bereitstellt; die Auswahl gilt dann nur bis zum Neustart. */
internal class InMemoryKeyValueStorage : KeyValueStorage {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

fun unavailableOrganizationRepository(platform: String): OrganizationRepository = object : OrganizationRepository {
    override suspend fun listUnits(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))
}

fun unavailableOrgUnitRepository(platform: String): OrgUnitRepository = object : OrgUnitRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observe(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun create(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> =
        unavailable()

    override suspend fun update(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> =
        unavailable()

    override suspend fun delete(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<Unit> = unavailable()
}
