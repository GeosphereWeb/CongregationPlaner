package de.geosphere.congregationplaner.organization.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository

expect fun createOrganizationRepository(): OrganizationRepository

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
        val ref = storage.getString(KEY_PATH)?.let(CongregationRef::parse) ?: return null
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
