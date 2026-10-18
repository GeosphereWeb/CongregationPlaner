package de.geosphere.congregationplaner.testing

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Minimaler lokaler HTTP-Server, der die Firestore-REST-API für Repository-Tests simuliert.
 * Über [withFirestoreRestConfig] werden die Repositories per System-Properties auf ihn umgeleitet.
 */
internal class FakeFirestoreRestServer(
    private val responder: (RecordedRequest) -> FakeResponse = { FakeResponse(200, "{}") },
) : AutoCloseable {
    data class RecordedRequest(
        val method: String,
        val path: String,
        val query: String?,
        val body: String,
        val authorization: String?,
    )

    data class FakeResponse(val status: Int, val body: String = "")

    val requests = CopyOnWriteArrayList<RecordedRequest>()

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            val request = RecordedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.rawPath,
                query = exchange.requestURI.rawQuery,
                body = exchange.requestBody.readBytes().decodeToString(),
                authorization = exchange.requestHeaders.getFirst("Authorization"),
            )
            requests += request
            val response = responder(request)
            val bytes = response.body.toByteArray()
            exchange.sendResponseHeaders(response.status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        start()
    }

    val host: String
        get() = "127.0.0.1:${server.address.port}"

    override fun close() {
        server.stop(0)
    }

    companion object {
        const val PROJECT_ID = "test-project"
        const val DOCUMENTS_PATH = "/v1/projects/$PROJECT_ID/databases/(default)/documents"
    }
}

internal fun <T> withFirestoreRestConfig(server: FakeFirestoreRestServer, block: () -> T): T {
    val previousProjectId = System.getProperty("FIREBASE_PROJECT_ID")
    val previousHost = System.getProperty("FIRESTORE_EMULATOR_HOST")
    System.setProperty("FIREBASE_PROJECT_ID", FakeFirestoreRestServer.PROJECT_ID)
    System.setProperty("FIRESTORE_EMULATOR_HOST", "http://${server.host}/")
    return try {
        block()
    } finally {
        restoreProperty("FIREBASE_PROJECT_ID", previousProjectId)
        restoreProperty("FIRESTORE_EMULATOR_HOST", previousHost)
    }
}

private fun restoreProperty(key: String, value: String?) {
    if (value == null) System.clearProperty(key) else System.setProperty(key, value)
}
