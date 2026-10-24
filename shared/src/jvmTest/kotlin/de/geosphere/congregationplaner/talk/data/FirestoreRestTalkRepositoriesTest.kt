package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestTalkRepositoriesTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val slotsPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.talkSchedulePath}"
    private val slot = TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30), "kongress", "Kongress", true)

    @Test
    fun `observeYear lists the collection, keeps only that year and sorts by date`() = runWithServer(
        responder = {
            FakeResponse(
                200,
                """{"documents":[${document("2027-01-03")},${document("2026-11-01")},${document("2026-10-25")}]}""",
            )
        },
    ) { server ->
        val slots = FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })
            .observeYear(congregation, 2026).first()

        assertEquals(listOf(LocalDate(2026, 10, 25), LocalDate(2026, 11, 1)), slots.map { it.date })
        assertEquals(LocalTime(17, 30), slots.first().time)
        assertEquals("Kongress", slots.first().weekEventName)
        assertTrue(slots.first().talkCancelled)
        val request = server.requests.single()
        assertEquals("GET", request.method)
        assertEquals(slotsPath, request.path)
        assertEquals("Bearer token-123", request.authorization)
    }

    @Test
    fun `observeYear follows the page token to read every page`() = runWithServer(
        responder = { request ->
            if (request.query.orEmpty().contains("pageToken=next%2F1")) {
                FakeResponse(200, """{"documents":[${document("2026-11-01")}]}""")
            } else {
                FakeResponse(200, """{"documents":[${document("2026-10-25")}],"nextPageToken":"next/1"}""")
            }
        },
    ) { server ->
        val slots = FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })
            .observeYear(congregation, 2026).first()

        assertEquals(listOf(LocalDate(2026, 10, 25), LocalDate(2026, 11, 1)), slots.map { it.date })
        assertEquals(2, server.requests.size)
        assertTrue(server.requests.all { it.query.orEmpty().contains("pageSize=300") })
    }

    @Test
    fun `observeYear skips documents with an invalid id or time`() = runWithServer(
        responder = {
            FakeResponse(
                200,
                """{"documents":[${document("2026-02-31")},${document("2026-10-25")},
                    {"name":"x/talkSchedule/2026-11-01","fields":{"talkCancelled":{"booleanValue":false}}}]}""",
            )
        },
    ) {
        val slots = FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })
            .observeYear(congregation, 2026).first()

        assertEquals(listOf(LocalDate(2026, 10, 25)), slots.map { it.date })
    }

    @Test
    fun `saveSlot replaces the document with the date as id`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        val result = FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })
            .saveSlot(congregation, slot)

        assertEquals(slot, result.getOrThrow())
        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$slotsPath/2026-10-25", request.path)
        assertTrue(request.body.contains(""""time":{"stringValue":"17:30"}"""))
        assertTrue(request.body.contains(""""talkCancelled":{"booleanValue":true}"""))
        assertTrue(request.body.contains(""""weekEventId":{"stringValue":"kongress"}"""))
    }

    @Test
    fun `slot talk is written as integer and title and read back`() = runWithServer(
        responder = { request ->
            if (request.method == "GET") {
                FakeResponse(
                    200,
                    """{"documents":[{"name":"x/talkSchedule/2026-10-25","fields":{
                        "time":{"stringValue":"17:30"},"talkCancelled":{"booleanValue":false},
                        "talkNumber":{"integerValue":"12"},"talkTitle":{"stringValue":"Der Weg zum Glück"}}}]}""",
                )
            } else {
                FakeResponse(200, "{}")
            }
        },
    ) { server ->
        val repository = FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })

        val read = repository.observeYear(congregation, 2026).first().single()
        assertEquals(12, read.talkNumber)
        assertEquals("Der Weg zum Glück", read.talkTitle)

        repository.saveSlot(congregation, read)
        val body = server.requests.last().body
        assertTrue(body.contains(""""talkNumber":{"integerValue":"12"}"""))
        assertTrue(body.contains(""""talkTitle":{"stringValue":"Der Weg zum Glück"}"""))

        repository.saveSlot(congregation, read.copy(talkNumber = null, talkTitle = null))
        assertTrue(server.requests.last().body.contains(""""talkNumber":{"nullValue":null}"""))
    }

    @Test
    fun `talk catalog reads number from the id and skips documents without a valid number`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/talkCatalog/12","fields":{"title":{"stringValue":"Der Weg zum Glück"}}},
                {"name":"x/talkCatalog/7","fields":{"title":{"stringValue":"Pausiert"},"active":{"booleanValue":false}}},
                {"name":"x/talkCatalog/abc","fields":{"title":{"stringValue":"Ungültig"}}},
                {"name":"x/talkCatalog/0","fields":{"title":{"stringValue":"Null"}}}]}""",
        )
    }) { server ->
        val talks = FirestoreRestTalkCatalogRepository(idTokenProvider = { "token-123" }).observeTalks().first()

        assertEquals(listOf(Talk(12, "Der Weg zum Glück", true), Talk(7, "Pausiert", false)), talks)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}/talkCatalog", server.requests.first().path)
    }

    @Test
    fun `saveSlot writes null for a missing week event`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })
            .saveSlot(congregation, slot.copy(weekEventId = null, weekEventName = null, talkCancelled = false))

        assertTrue(server.requests.single().body.contains(""""weekEventId":{"nullValue":null}"""))
    }

    @Test
    fun `deleteSlot deletes the document of the day and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("2026-10-25")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        val repository = FirestoreRestTalkScheduleRepository(idTokenProvider = { "token-123" })

        assertTrue(repository.deleteSlot(congregation, LocalDate(2026, 10, 25)).isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.deleteSlot(congregation, LocalDate(2026, 11, 1)).isFailure)
    }

    @Test
    fun `week events are read from the global collection`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[{"name":"x/weekEvents/kongress","fields":{
                "name":{"stringValue":"Kongress"},"cancelsTalk":{"booleanValue":true}}},
                {"name":"x/weekEvents/besuch","fields":{"name":{"stringValue":"Kreisaufseher"}}}]}""",
        )
    }) { server ->
        val events = FirestoreRestWeekEventRepository(idTokenProvider = { "token-123" })
            .observeWeekEvents().first()

        assertEquals(listOf(WeekEvent("kongress", "Kongress", true), WeekEvent("besuch", "Kreisaufseher")), events)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}/weekEvents", server.requests.single().path)
    }

    private fun document(id: String) = """{"name":"x/talkSchedule/$id","fields":{
        "time":{"stringValue":"17:30"},
        "talkCancelled":{"booleanValue":true},
        "weekEventId":{"stringValue":"kongress"},
        "weekEventName":{"stringValue":"Kongress"}}}"""

    private fun runWithServer(
        responder: (FakeFirestoreRestServer.RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) { runBlocking { block(server) } }
        }
    }
}
