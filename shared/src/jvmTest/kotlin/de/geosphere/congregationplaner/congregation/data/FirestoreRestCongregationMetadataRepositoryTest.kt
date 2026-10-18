package de.geosphere.congregationplaner.congregation.data

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreRestCongregationMetadataRepositoryTest {
    private val collectionPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/$CONGREGATION_COLLECTION_PATH"

    @Test
    fun `observeAll maps firestore documents to congregations`() = runWithServer(
        FakeResponse(200, documentsJson(congregationDocument("609a", "Berlin", "19:00", 3, "10:00", 7))),
    ) { server, repository ->
        val congregations = repository.observeAll().first()

        assertEquals(
            listOf(CongregationMetadata("609a", "Berlin", LocalTime(19, 0), 3, LocalTime(10, 0), 7)),
            congregations,
        )
        val request = server.requests.single()
        assertEquals("GET", request.method)
        assertEquals(collectionPath, request.path)
        assertEquals("pageSize=1000", request.query)
        assertEquals("Bearer token-123", request.authorization)
    }

    @Test
    fun `observeAll returns empty list when collection has no documents`() = runWithServer(
        FakeResponse(200, "{}"),
    ) { _, repository ->
        assertEquals(emptyList(), repository.observeAll().first())
    }

    @Test
    fun `observeAll uses defaults for missing weekday fields and rejects invalid data`() {
        val missingWeekdays = """{"documents":[{"name":"x/609a","fields":{
            "name":{"stringValue":"Berlin"},
            "midweek_meeting_time":{"stringValue":"19:00"},
            "weekend_meeting_time":{"stringValue":"10:00"}}}]}"""
        runWithServer(FakeResponse(200, missingWeekdays)) { _, repository ->
            val congregation = repository.observeAll().first().single()
            assertEquals(1, congregation.midweekMeetingWeekday)
            assertEquals(1, congregation.weekendMeetingWeekday)
        }

        listOf(
            congregationDocument("1", "Berlin", "19:00", 9, "10:00", 7),
            congregationDocument("1", "Berlin", "25:99", 3, "10:00", 7),
            """{"name":"x/1","fields":{"name":{"integerValue":"5"}}}""",
            """{"name":"x/1","fields":{"name":{"stringValue":"B"},"midweek_meeting_weekday":{"stringValue":"Mo"}}}""",
        ).forEach { document ->
            runWithServer(FakeResponse(200, documentsJson(document))) { _, repository ->
                assertFailsWith<IllegalArgumentException> { repository.observeAll().first() }
            }
        }
    }

    @Test
    fun `observeAll fails for documents without name and for http errors`() {
        runWithServer(FakeResponse(200, """{"documents":[{"fields":{}}]}""")) { _, repository ->
            assertFailsWith<IOException> { repository.observeAll().first() }
        }
        runWithServer(FakeResponse(503)) { _, repository ->
            assertFailsWith<IOException> { repository.observeAll().first() }
        }
    }

    @Test
    fun `save creates new congregation with POST and returns generated id`() = runWithServer(
        FakeResponse(200, """{"name":"projects/p/databases/(default)/documents/congregation/new-id"}"""),
    ) { server, repository ->
        val congregation = CongregationMetadata(name = "Berlin", weekendMeetingTime = LocalTime(10, 0))

        val saved = repository.save(congregation).getOrThrow()

        assertEquals(congregation.copy(id = "new-id"), saved)
        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals(collectionPath, request.path)
        assertTrue(request.body.contains(""""name":{"stringValue":"Berlin"}"""))
        assertTrue(request.body.contains(""""weekend_meeting_time":{"stringValue":"10:00"}"""))
        assertTrue(request.body.contains(""""midweek_meeting_weekday":{"integerValue":3}"""))
    }

    @Test
    fun `save updates existing congregation with PATCH and update mask`() = runWithServer(
        FakeResponse(200, "{}"),
    ) { server, repository ->
        val congregation = CongregationMetadata(id = "609a", name = "Berlin")

        assertEquals(congregation, repository.save(congregation).getOrThrow())

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$collectionPath/609a", request.path)
        assertTrue(request.query.orEmpty().contains("updateMask.fieldPaths=name"))
        assertTrue(request.query.orEmpty().contains("updateMask.fieldPaths=weekend_meeting_weekday"))
    }

    @Test
    fun `save returns failure for http errors and missing sign in`() {
        runWithServer(FakeResponse(500)) { _, repository ->
            assertIs<IOException>(repository.save(CongregationMetadata(name = "Berlin")).exceptionOrNull())
        }
        runWithServer(FakeResponse(200), token = null) { server, repository ->
            assertIs<IllegalStateException>(repository.save(CongregationMetadata(name = "Berlin")).exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
    }

    @Test
    fun `delete removes congregation document`() = runWithServer(FakeResponse(200, "{}")) { server, repository ->
        assertTrue(repository.delete("609a").isSuccess)

        val request = server.requests.single()
        assertEquals("DELETE", request.method)
        assertEquals("$collectionPath/609a", request.path)
    }

    @Test
    fun `delete rejects blank ids and reports http errors`() {
        runWithServer(FakeResponse(200)) { server, repository ->
            assertIs<IllegalArgumentException>(repository.delete(" ").exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
        runWithServer(FakeResponse(404)) { _, repository ->
            assertIs<IOException>(repository.delete("609a").exceptionOrNull())
        }
    }

    @Test
    fun `desktop factory creates rest repository`() {
        assertIs<FirestoreRestCongregationMetadataRepository>(createCongregationMetadataRepository())
    }

    private fun runWithServer(
        response: FakeResponse,
        token: String? = "token-123",
        block: suspend (FakeFirestoreRestServer, FirestoreRestCongregationMetadataRepository) -> Unit,
    ) {
        FakeFirestoreRestServer { response }.use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestCongregationMetadataRepository(idTokenProvider = { token })) }
            }
        }
    }

    @Suppress("LongParameterList")
    private fun congregationDocument(
        id: String,
        name: String,
        midweekTime: String,
        midweekWeekday: Int,
        weekendTime: String,
        weekendWeekday: Int,
    ): String = """{"name":"projects/p/databases/(default)/documents/congregation/$id","fields":{
        "name":{"stringValue":"$name"},
        "midweek_meeting_time":{"stringValue":"$midweekTime"},
        "midweek_meeting_weekday":{"integerValue":"$midweekWeekday"},
        "weekend_meeting_time":{"stringValue":"$weekendTime"},
        "weekend_meeting_weekday":{"integerValue":"$weekendWeekday"}}}"""

    private fun documentsJson(vararg documents: String): String = """{"documents":[${documents.joinToString(",")}]}"""
}
