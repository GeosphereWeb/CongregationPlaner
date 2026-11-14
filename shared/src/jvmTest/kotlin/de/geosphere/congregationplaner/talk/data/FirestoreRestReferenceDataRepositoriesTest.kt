package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestReferenceDataRepositoriesTest {
    private val documents = FakeFirestoreRestServer.DOCUMENTS_PATH
    private val country = CountryRef("EMEA", "europe", "central-europe", "de")
    private val countryPath = "region/EMEA/subregion/europe/branch/central-europe/country/de"

    @Test
    fun `saveTalk patches the document of the number with title and active flag`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server ->
        val talk = Talk(
            12,
            "Der Weg zum Glück",
            active = false,
            titleChanges = listOf(TalkTitleChange(LocalDate(2027, 1, 1), "Neuer Titel")),
        )

        assertEquals(
            talk,
            FirestoreRestTalkCatalogRepository(idTokenProvider = {
                "t"
            }).save(country, talk).getOrThrow(),
        )

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$documents/$countryPath/talkCatalog/12", request.path)
        assertTrue(request.body.contains(""""title":{"stringValue":"Der Weg zum Glück"}"""))
        assertTrue(request.body.contains(""""active":{"booleanValue":false}"""))
        assertTrue(request.body.contains(""""validFrom":{"stringValue":"2027-01-01"}"""))
        assertTrue(request.body.contains(""""title":{"stringValue":"Neuer Titel"}"""))
    }

    @Test
    fun `deleteTalk deletes the document and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("/12")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        val repository = FirestoreRestTalkCatalogRepository(idTokenProvider = { "t" })

        assertTrue(repository.delete(country, 12).isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.delete(country, 13).isFailure)
    }

    @Test
    fun `saveWeekEvent posts a new event and patches an existing one`() = runWithServer({ request ->
        if (request.method == "POST") {
            FakeResponse(
                200,
                """{"name":"x/weekEvents/new1","fields":{
                    "name":{"stringValue":"Kongress"},"cancelsTalk":{"booleanValue":true}}}""",
            )
        } else {
            FakeResponse(200, "{}")
        }
    }) { server ->
        val repository = FirestoreRestWeekEventRepository(idTokenProvider = { "t" })

        val created = repository.save(country, WeekEvent("", "Kongress", cancelsTalk = true)).getOrThrow()
        assertEquals(WeekEvent("new1", "Kongress", cancelsTalk = true), created)
        val post = server.requests.last()
        assertEquals("POST", post.method)
        assertEquals("$documents/$countryPath/weekEvents", post.path)
        assertTrue(post.body.contains(""""cancelsTalk":{"booleanValue":true}"""))

        val existing = WeekEvent("ev1", "Besuch")
        assertEquals(existing, repository.save(country, existing).getOrThrow())
        val patch = server.requests.last()
        assertEquals("PATCH", patch.method)
        assertEquals("$documents/$countryPath/weekEvents/ev1", patch.path)
    }

    @Test
    fun `deleteWeekEvent deletes the document and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("/ev1")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        val repository = FirestoreRestWeekEventRepository(idTokenProvider = { "t" })

        assertTrue(repository.delete(country, "ev1").isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.delete(country, "ev2").isFailure)
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) { runBlocking { block(server) } }
        }
    }
}
