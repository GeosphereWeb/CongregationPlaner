package de.geosphere.congregationplaner.notice.data

import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.data.settingsPath
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestTalkNoticeRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val documents = FakeFirestoreRestServer.DOCUMENTS_PATH
    private val settingsPath = "$documents/${congregation.settingsPath}"
    private val imagesPath = "$documents/${congregation.noticeImagesPath}"
    private val repository = FirestoreRestTalkNoticeRepository(idTokenProvider = { "token-123" })
    private val png = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 0, 127, -128, -1)
    private val pngBase64 = Base64.getEncoder().encodeToString(png)

    @Test
    fun `settings without a document are the defaults`() = runWithServer({ FakeResponse(200, "{}") }) { _ ->
        assertEquals(TalkNoticeSettings(), repository.observeSettings(congregation).first())
    }

    @Test
    fun `settings read title color set and default image and ignore other documents`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[{"name":"x/settings/talkPlanning","fields":{"title":{"stringValue":"Falsch"}}},
                {"name":"x/settings/talkNotice","fields":{"title":{"stringValue":"Vorträge"},
                "colorSet":{"stringValue":"green"},"defaultImageId":{"stringValue":"img1"}}}]}""",
        )
    }) { _ ->
        assertEquals(
            TalkNoticeSettings("Vorträge", NoticeColorSet.GREEN, "img1"),
            repository.observeSettings(congregation).first(),
        )
    }

    @Test
    fun `an unknown color set and a null default image fall back`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[{"name":"x/settings/talkNotice","fields":{"title":{"stringValue":"T"},
                "colorSet":{"stringValue":"pink"},"defaultImageId":{"nullValue":null}}}]}""",
        )
    }) { _ ->
        assertEquals(
            TalkNoticeSettings("T", NoticeColorSet.BLUE, null),
            repository.observeSettings(congregation).first(),
        )
    }

    @Test
    fun `saveSettings replaces the talkNotice document`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        val settings = TalkNoticeSettings("Vorträge", NoticeColorSet.PETROL, "img1")

        assertEquals(settings, repository.saveSettings(congregation, settings).getOrThrow())

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$settingsPath/talkNotice", request.path)
        assertTrue(request.body.contains(""""title":{"stringValue":"Vorträge"}"""))
        assertTrue(request.body.contains(""""colorSet":{"stringValue":"petrol"}"""))
        assertTrue(request.body.contains(""""defaultImageId":{"stringValue":"img1"}"""))

        repository.saveSettings(congregation, TalkNoticeSettings())
        assertTrue(server.requests.last().body.contains(""""defaultImageId":{"nullValue":null}"""))
    }

    @Test
    fun `saveSettings reports a rejected write`() = runWithServer({ FakeResponse(403, "{}") }) { _ ->
        assertTrue(repository.saveSettings(congregation, TalkNoticeSettings()).isFailure)
    }

    @Test
    fun `loadImages reads the name and the exact bytes`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[{"name":"x/noticeImages/a1","fields":{"name":{"stringValue":"Logo"},
                "png":{"bytesValue":"$pngBase64"}}}]}""",
        )
    }) { server ->
        assertEquals(listOf(NoticeImage("a1", "Logo", png)), repository.loadImages(congregation).getOrThrow())
        assertEquals(imagesPath, server.requests.single().path)
    }

    @Test
    fun `addImage posts name and base64 bytes and returns the new id`() = runWithServer({
        FakeResponse(200, """{"name":"x/noticeImages/new42","fields":{}}""")
    }) { server ->
        assertEquals(NoticeImage("new42", "Logo", png), repository.addImage(congregation, "Logo", png).getOrThrow())

        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals(imagesPath, request.path)
        assertTrue(request.body.contains(""""png":{"bytesValue":"$pngBase64"}"""))
        assertTrue(request.body.contains(""""name":{"stringValue":"Logo"}"""))
    }

    @Test
    fun `renameImage patches only the name`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        repository.renameImage(congregation, "a1", "Neu").getOrThrow()

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$imagesPath/a1", request.path)
        assertEquals("updateMask.fieldPaths=name", request.query)
        assertTrue(!request.body.contains("png"))
    }

    @Test
    fun `deleteImage removes the document`() = runWithServer({ FakeResponse(200, "{}") }) { server ->
        repository.deleteImage(congregation, "a1").getOrThrow()

        val request = server.requests.single()
        assertEquals("DELETE", request.method)
        assertEquals("$imagesPath/a1", request.path)
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
