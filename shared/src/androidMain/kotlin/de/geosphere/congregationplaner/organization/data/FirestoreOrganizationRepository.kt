package de.geosphere.congregationplaner.organization.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.auth.FirebaseAndroidContextHolder
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository

actual fun createOrganizationRepository(): OrganizationRepository =
    FirestoreOrganizationRepository(AndroidFirestoreProvider.firestore)

actual fun createKeyValueStorage(): KeyValueStorage {
    val context = FirebaseAndroidContextHolder.context ?: return InMemoryKeyValueStorage()
    return SharedPreferencesKeyValueStorage(context)
}

internal class FirestoreOrganizationRepository(private val firestore: FirebaseFirestore) : OrganizationRepository {
    override suspend fun listUnits(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>> =
        runFirestoreCatching("list ${level.collection} units") {
            firestore.collection(level.collectionPath(parentIds)).get().awaitResult().documents.map { document ->
                OrgUnit(id = document.id, name = document.get("name") as? String ?: "")
            }
        }
}

private class SharedPreferencesKeyValueStorage(context: Context) : KeyValueStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

private const val PREFERENCES_NAME = "congregation_planer"
