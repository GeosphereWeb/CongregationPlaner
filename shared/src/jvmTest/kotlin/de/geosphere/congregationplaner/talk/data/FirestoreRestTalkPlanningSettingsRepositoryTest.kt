package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestTalkPlanningSettingsRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val settingsPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.settingsPath}"
    private val repository = FirestoreRestTalkPlanningSettingsRepository(idTokenProvider = { "token-123" })

    @Test
    fun `observe reads the talk planning document and ignores invalid dates`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[{"name":"x/settings/other","fields":{"speakerFreeSince":{"stringValue":"2020-01-01"}}},
                {"name":"x/settings/talkPlanning","fields":{"speakerFreeSince":{"stringValue":"2025-11-02"},
                "talkFreeSince":{"stringValue":"kein Datum"}}}]}""",
        )
    }) { server ->
        assertEquals(
            TalkPlanningSettings(speakerFreeSince = LocalDate(2025, 11, 2)),
            repository.observe(congregation).first(),
        )
        assertEquals(settingsPath, server.requests.single().path)
    }

    @Test
    fun `observe without a stored document has no cut-off dates`() = runWithServer({ FakeResponse(200, "{}") }) { _ ->
        assertEquals(TalkPlanningSettings(), repository.observe(congregation).first())
    }

    @Test
    fun `save replaces the talk planning document`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        val settings = TalkPlanningSettings(talkFreeSince = LocalDate(2024, 1, 14))

        assertEquals(settings, repository.save(congregation, settings).getOrThrow())

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$settingsPath/talkPlanning", request.path)
        assertTrue(request.body.contains(""""talkFreeSince":{"stringValue":"2024-01-14"}"""))
        assertTrue(request.body.contains(""""speakerFreeSince":{"nullValue":null}"""))
    }

    @Test
    fun `save reports a rejected write`() = runWithServer({ FakeResponse(403, "{}") }) { _ ->
        assertTrue(repository.save(congregation, TalkPlanningSettings()).isFailure)
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
