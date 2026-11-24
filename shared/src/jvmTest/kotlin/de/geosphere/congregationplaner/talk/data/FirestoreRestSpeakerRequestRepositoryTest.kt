package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
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

class FirestoreRestSpeakerRequestRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    @Test
    fun `observeRequests maps requests with contact attempts and skips invalid documents`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/talkRequests/bad","fields":{"createdOn":{"stringValue":"2026-02-31"}}},
                {"name":"x/talkRequests/r1","fields":{
                    "createdOn":{"stringValue":"2026-11-10"},
                    "note":{"stringValue":"Frühjahr"},
                    "speakerLastName":{"stringValue":"Huber"},
                    "talkNumber":{"integerValue":"12"},
                    "contactAttempts":{"arrayValue":{"values":[
                        {"mapValue":{"fields":{"date":{"stringValue":"2026-11-12"},
                            "kind":{"stringValue":"VOICEMAIL"},"note":{"stringValue":"zurückrufen"}}}},
                        {"mapValue":{"fields":{"date":{"stringValue":"2026-11-20"},"kind":{"stringValue":"CALL"},
                            "note":{"nullValue":null}}}}
                    ]}}}}
            ]}""",
        )
    }) { server ->
        val read = FirestoreRestSpeakerRequestRepository(idTokenProvider = { "token-123" })
            .observeRequests(congregation).first()

        assertEquals(
            listOf(
                SpeakerRequest(
                    id = "r1",
                    createdOn = LocalDate(2026, 11, 10),
                    note = "Frühjahr",
                    contactAttempts = listOf(
                        ContactAttempt(LocalDate(2026, 11, 12), ContactKind.VOICEMAIL, "zurückrufen"),
                        ContactAttempt(LocalDate(2026, 11, 20), ContactKind.CALL),
                    ),
                    speakerLastName = "Huber",
                    talkNumber = 12,
                ),
            ),
            read,
        )
        assertEquals(
            "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.talkRequestsPath}",
            server.requests.single().path,
        )
    }

    @Test
    fun `commit sends all writes and deletes in one commit`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        val result = FirestoreRestSpeakerRequestRepository(idTokenProvider = { "token-123" }).commit(
            congregation,
            ScheduleWrite(
                saveSlots = listOf(TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0), requestId = "r1")),
                deleteSlotDates = listOf(LocalDate(2026, 11, 29)),
                saveRequests = listOf(
                    SpeakerRequest(
                        "r1",
                        LocalDate(2026, 11, 10),
                        contactAttempts = listOf(ContactAttempt(LocalDate(2026, 11, 12), ContactKind.EMAIL)),
                    ),
                ),
                deleteRequestIds = listOf("r0"),
            ),
        )

        assertTrue(result.isSuccess)
        val body = server.requests.single().body
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}:commit", server.requests.single().path)
        assertTrue(body.contains("${congregation.talkSchedulePath}/2026-12-06"))
        assertTrue(body.contains(""""requestId":{"stringValue":"r1"}"""))
        assertTrue(body.contains(""""delete":""") && body.contains("${congregation.talkSchedulePath}/2026-11-29"))
        assertTrue(body.contains("${congregation.talkRequestsPath}/r1"))
        assertTrue(body.contains("${congregation.talkRequestsPath}/r0"))
        assertTrue(body.contains(""""kind":{"stringValue":"EMAIL"}"""))
    }

    private fun runWithServer(
        responder: (FakeFirestoreRestServer.RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) { runBlocking { block(server) } }
        }
    }
}
