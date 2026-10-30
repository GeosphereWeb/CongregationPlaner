package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestTalkHistoryRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val path = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.talkHistoryPath}"

    @Test
    fun `entries are listed, saved with their dates and deleted`() {
        val responder = { request: FakeFirestoreRestServer.RecordedRequest ->
            when {
                request.method == "GET" -> FakeResponse(
                    200,
                    """{"documents":[
                        {"name":"x/talkHistory/7","fields":{"heldOn":{"arrayValue":{"values":[
                            {"stringValue":"2026-10-25"},{"stringValue":"2026-03-01"}]}},
                            "plannedOn":{"arrayValue":{"values":[{"stringValue":"2026-10-25"}]}},
                            "speakers":{"mapValue":{"fields":{"2026-10-25":{"stringValue":"Muster, Max"},
                                "not a date":{"stringValue":"x"},"2026-03-01":{"integerValue":"5"}}}}}},
                        {"name":"x/talkHistory/x","fields":{"heldOn":{"arrayValue":{"values":[
                            {"stringValue":"2026-03-01"}]}}}},
                        {"name":"x/talkHistory/8","fields":{"heldOn":{"arrayValue":{"values":[
                            {"stringValue":"not a date"}]}}}},
                        {"name":"x/talkHistory/9","fields":{}}]}""",
                )
                request.method == "DELETE" && request.path.endsWith("/9") -> FakeResponse(403)
                else -> FakeResponse(200, "{}")
            }
        }
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking {
                    val repository = FirestoreRestTalkHistoryRepository(idTokenProvider = { "token-123" })

                    val listed = repository.observeAll(congregation).first()
                    val expected = TalkHistoryEntry(
                        7,
                        listOf(LocalDate(2026, 3, 1), LocalDate(2026, 10, 25)),
                        listOf(LocalDate(2026, 10, 25)),
                        mapOf(LocalDate(2026, 10, 25) to "Muster, Max"),
                    )
                    assertEquals(listOf(expected), listed)
                    assertEquals(path, server.requests.first().path)

                    assertEquals(expected, repository.save(congregation, expected).getOrThrow())
                    val patch = server.requests.last()
                    assertEquals("PATCH", patch.method)
                    assertEquals("$path/7", patch.path)
                    assertTrue(patch.body.contains(""""stringValue":"2026-10-25""""))
                    assertTrue(patch.body.contains(""""plannedOn""""))
                    assertTrue(patch.body.contains(""""2026-10-25":{"stringValue":"Muster, Max"}"""))

                    assertTrue(repository.delete(congregation, 7).isSuccess)
                    assertEquals("DELETE", server.requests.last().method)
                    assertTrue(repository.delete(congregation, 9).isFailure)
                }
            }
        }
    }
}
