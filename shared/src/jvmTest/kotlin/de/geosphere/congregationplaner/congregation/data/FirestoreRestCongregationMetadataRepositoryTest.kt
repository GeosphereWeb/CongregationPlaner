package de.geosphere.congregationplaner.congregation.data

import com.google.gson.JsonParser
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreRestCongregationMetadataRepositoryTest {
    private val circuit = listOf("EMEA", "europe", "central-europe", "de", "c1")
    private val collectionPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregationCollectionPath(circuit)}"

    @Test
    fun `observeAll maps firestore documents to congregations`() = runWithServer(
        FakeResponse(200, documentsJson(congregationDocument("609a", "Berlin", "19:00", 3, "10:00", 7))),
    ) { server, repository ->
        val congregations = repository.observeAll(circuit).first()

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
        assertEquals(emptyList(), repository.observeAll(circuit).first())
    }

    @Test
    fun `observeAll uses defaults for missing weekday fields and rejects invalid data`() {
        val missingWeekdays = """{"documents":[{"name":"x/609a","fields":{
            "name":{"stringValue":"Berlin"},
            "midweek_meeting_time":{"stringValue":"19:00"},
            "weekend_meeting_time":{"stringValue":"10:00"}}}]}"""
        runWithServer(FakeResponse(200, missingWeekdays)) { _, repository ->
            val congregation = repository.observeAll(circuit).first().single()
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
                assertFailsWith<IllegalArgumentException> { repository.observeAll(circuit).first() }
            }
        }
    }

    @Test
    fun `observeAll fails for documents without name and for http errors`() {
        runWithServer(FakeResponse(200, """{"documents":[{"fields":{}}]}""")) { _, repository ->
            assertFailsWith<IOException> { repository.observeAll(circuit).first() }
        }
        runWithServer(FakeResponse(503)) { _, repository ->
            assertFailsWith<IOException> { repository.observeAll(circuit).first() }
        }
    }

    @Test
    fun `create writes the congregation name document and first admin in one commit`() = runWithServer(
        FakeResponse(200, "{}"),
    ) { server, repository ->
        val admin = NewCongregationAdmin("uid-1", "max@example.com", "Max Muster", "Max", "Muster", true)

        val created = repository.create(
            circuit,
            CongregationMetadata(name = " Grafing Süd".trim(), weekendMeetingTime = LocalTime(10, 0)),
            "uid-1",
            admin,
        ).getOrThrow()

        assertEquals(20, created.id.length)
        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}:commit", request.path)
        assertEquals("Bearer token-123", request.authorization)
        val writes = JsonParser.parseString(request.body).asJsonObject.getAsJsonArray("writes")
            .map { it.asJsonObject.getAsJsonObject("update") }
        assertEquals(3, writes.size)
        val prefix = "projects/${FakeFirestoreRestServer.PROJECT_ID}/databases/(default)/documents"
        val congregationPath = "$prefix/${congregationCollectionPath(circuit)}/${created.id}"
        assertEquals(congregationPath, writes[0].get("name").asString)
        assertEquals(
            "uid-1",
            writes[0].getAsJsonObject("fields").getAsJsonObject("createdBy").get("stringValue").asString,
        )
        assertEquals(
            "Grafing Süd",
            writes[0].getAsJsonObject("fields").getAsJsonObject("name").get("stringValue").asString,
        )
        assertEquals(
            "$prefix/${uniqueCongregationNamesCollectionPath(circuit)}/grafing süd",
            writes[1].get("name").asString,
        )
        assertEquals(
            created.id,
            writes[1].getAsJsonObject("fields").getAsJsonObject("congregationId").get("stringValue").asString,
        )
        assertEquals("$congregationPath/members/uid-1", writes[2].get("name").asString)
        val member = writes[2].getAsJsonObject("fields")
        assertTrue(member.toString().contains(""""approved":{"booleanValue":true}"""))
        assertTrue(member.toString().contains("CONGREGATION_ADMIN"))
        assertEquals("Max", member.getAsJsonObject("firstName").get("stringValue").asString)
        assertEquals("Muster", member.getAsJsonObject("lastName").get("stringValue").asString)
    }

    @Test
    fun `create without a first admin writes no member and reports failures`() {
        runWithServer(FakeResponse(200, "{}")) { server, repository ->
            repository.create(circuit, CongregationMetadata(name = "Nord"), "global-uid", null).getOrThrow()

            val writes = JsonParser.parseString(server.requests.single().body).asJsonObject.getAsJsonArray("writes")
            assertEquals(2, writes.size())
        }
        // Die Regeln lehnen einen vergebenen Namen ab; der Fehler kommt als Failure an.
        runWithServer(FakeResponse(403)) { _, repository ->
            assertIs<IOException>(
                repository.create(circuit, CongregationMetadata(name = "Nord"), "uid-1", null).exceptionOrNull(),
            )
        }
    }

    @Test
    fun `findNameOwner reads the name document of the circuit`() {
        runWithServer(
            FakeResponse(200, """{"name":"x/grafing süd","fields":{"congregationId":{"stringValue":"k1"}}}"""),
        ) { server, repository ->
            assertEquals("k1", repository.findNameOwner(circuit, "  Grafing   SÜD ").getOrThrow())

            val request = server.requests.single()
            assertEquals("GET", request.method)
            assertEquals(
                "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${uniqueCongregationNamesCollectionPath(
                    circuit,
                )}/grafing%20s%C3%BCd",
                request.path,
            )
        }
        runWithServer(FakeResponse(404)) { _, repository ->
            assertNull(repository.findNameOwner(circuit, "Frei").getOrThrow())
        }
        runWithServer(FakeResponse(500)) { _, repository ->
            assertIs<IOException>(repository.findNameOwner(circuit, "Nord").exceptionOrNull())
        }
    }

    @Test
    fun `save keeps the name document when the name stays`() {
        val current = congregationDocument("609a", "Berlin", "19:00", 3, "10:00", 7)
        runWithServer(
            responder = { request ->
                if (request.method ==
                    "GET"
                ) {
                    FakeResponse(200, current)
                } else {
                    FakeResponse(200, "{}")
                }
            },
        ) { server, repository ->
            val congregation = CongregationMetadata(id = "609a", name = " BERLIN ".trim())

            assertEquals(congregation, repository.save(circuit, congregation).getOrThrow())

            assertEquals(listOf("GET", "PATCH"), server.requests.map { it.method })
            val patch = server.requests.last()
            assertEquals("$collectionPath/609a", patch.path)
            assertTrue(patch.query.orEmpty().contains("updateMask.fieldPaths=name"))
            assertTrue(patch.query.orEmpty().contains("updateMask.fieldPaths=weekend_meeting_weekday"))
        }
    }

    @Test
    fun `save moves the name document along with a rename`() {
        val current = congregationDocument("609a", "Berlin", "19:00", 3, "10:00", 7)
        runWithServer(
            responder = { request ->
                if (request.method ==
                    "GET"
                ) {
                    FakeResponse(200, current)
                } else {
                    FakeResponse(200, "{}")
                }
            },
        ) { server, repository ->
            repository.save(circuit, CongregationMetadata(id = "609a", name = "Berlin-Mitte")).getOrThrow()

            assertEquals(listOf("GET", "POST"), server.requests.map { it.method })
            val commit = server.requests.last()
            assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}:commit", commit.path)
            val writes = JsonParser.parseString(commit.body).asJsonObject.getAsJsonArray("writes").map {
                it.asJsonObject
            }
            val prefix = "projects/${FakeFirestoreRestServer.PROJECT_ID}/databases/(default)/documents"
            assertEquals(
                "$prefix/${congregationCollectionPath(circuit)}/609a",
                writes[0].getAsJsonObject("update").get("name").asString,
            )
            assertEquals(
                "$prefix/${uniqueCongregationNamesCollectionPath(circuit)}/berlin",
                writes[1].get("delete").asString,
            )
            assertEquals(
                "$prefix/${uniqueCongregationNamesCollectionPath(circuit)}/berlin-mitte",
                writes[2].getAsJsonObject("update").get("name").asString,
            )
        }
    }

    @Test
    fun `save needs an id and reports http errors and a missing sign in`() {
        runWithServer(FakeResponse(200)) { server, repository ->
            assertIs<IllegalArgumentException>(
                repository.save(circuit, CongregationMetadata(name = "Berlin")).exceptionOrNull(),
            )
            assertTrue(server.requests.isEmpty())
        }
        runWithServer(FakeResponse(500)) { _, repository ->
            assertIs<IOException>(
                repository.save(circuit, CongregationMetadata(id = "609a", name = "Berlin")).exceptionOrNull(),
            )
        }
        runWithServer(FakeResponse(200), token = null) { server, repository ->
            assertIs<IllegalStateException>(
                repository.save(circuit, CongregationMetadata(id = "609a", name = "Berlin")).exceptionOrNull(),
            )
            assertTrue(server.requests.isEmpty())
        }
    }

    @Test
    fun `delete removes the congregation together with its name document`() {
        val current = congregationDocument("609a", "Grafing Süd", "19:00", 3, "10:00", 7)
        runWithServer(
            responder = { request ->
                if (request.method ==
                    "GET"
                ) {
                    FakeResponse(200, current)
                } else {
                    FakeResponse(200, "{}")
                }
            },
        ) { server, repository ->
            assertTrue(repository.delete(circuit, "609a").isSuccess)

            assertEquals(listOf("GET", "POST"), server.requests.map { it.method })
            val writes = JsonParser.parseString(server.requests.last().body).asJsonObject.getAsJsonArray("writes")
                .map { it.asJsonObject.get("delete").asString }
            val prefix = "projects/${FakeFirestoreRestServer.PROJECT_ID}/databases/(default)/documents"
            assertEquals(
                listOf(
                    "$prefix/${congregationCollectionPath(circuit)}/609a",
                    "$prefix/${uniqueCongregationNamesCollectionPath(circuit)}/grafing süd",
                ),
                writes,
            )
        }
    }

    @Test
    fun `delete rejects blank ids and reports http errors`() {
        runWithServer(FakeResponse(200)) { server, repository ->
            assertIs<IllegalArgumentException>(repository.delete(circuit, " ").exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
        runWithServer(FakeResponse(500)) { _, repository ->
            assertIs<IOException>(repository.delete(circuit, "609a").exceptionOrNull())
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
    ) = runWithServer(token = token, responder = { response }, block = block)

    private fun runWithServer(
        token: String? = "token-123",
        responder: (FakeFirestoreRestServer.RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer, FirestoreRestCongregationMetadataRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
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
