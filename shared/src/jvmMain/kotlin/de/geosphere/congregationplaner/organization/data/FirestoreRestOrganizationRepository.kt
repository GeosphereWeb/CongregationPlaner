package de.geosphere.congregationplaner.organization.data

import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import java.net.http.HttpClient
import java.util.prefs.Preferences

actual fun createOrganizationRepository(): OrganizationRepository = FirestoreRestOrganizationRepository()

actual fun createKeyValueStorage(): KeyValueStorage = object : KeyValueStorage {
    private val preferences = Preferences.userRoot().node("de/geosphere/congregationplaner")

    override fun getString(key: String): String? = preferences.get(key, null)

    override fun putString(key: String, value: String) {
        preferences.put(key, value)
    }

    override fun remove(key: String) {
        preferences.remove(key)
    }
}

internal class FirestoreRestOrganizationRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : OrganizationRepository {
    // Die Organisationsstruktur wird schon vor der Anmeldung gelesen; ein Token ist daher optional.
    private val client = FirestoreRestClient(httpClient, idTokenProvider, signInRequiredMessage = null)

    override suspend fun listUnits(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>> = runCatchingIo {
        parseDocuments(client.request("GET", "${client.collectionUrl(level.collectionPath(parentIds))}?pageSize=1000"))
            .map { OrgUnit(id = it.id, name = it.fields.optionalStringField("name").orEmpty()) }
    }
}
