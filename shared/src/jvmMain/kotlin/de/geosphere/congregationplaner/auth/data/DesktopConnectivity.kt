package de.geosphere.congregationplaner.auth.data

import de.geosphere.congregationplaner.auth.domain.repository.ConnectivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

private const val PROBE_HOST = "firestore.googleapis.com"
private const val PROBE_PORT = 443
private const val PROBE_TIMEOUT_MILLIS = 3_000
private const val PROBE_INTERVAL_MILLIS = 10_000L

actual fun createConnectivityRepository(): ConnectivityRepository = DesktopConnectivityRepository()

/** Der Desktop hat keine Netzwerk-Ereignisse; er prüft regelmäßig, ob Firestore erreichbar ist. */
internal class DesktopConnectivityRepository(
    private val host: String = PROBE_HOST,
    private val port: Int = PROBE_PORT,
    private val intervalMillis: Long = PROBE_INTERVAL_MILLIS,
) : ConnectivityRepository {
    override fun observeOnline(): Flow<Boolean> = flow {
        while (true) {
            emit(isReachable(host, port))
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)
}

internal fun isReachable(host: String, port: Int): Boolean = try {
    Socket().use { it.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MILLIS) }
    true
} catch (_: IOException) {
    false
}
