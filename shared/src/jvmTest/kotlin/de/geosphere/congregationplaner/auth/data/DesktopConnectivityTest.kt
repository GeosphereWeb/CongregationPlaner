package de.geosphere.congregationplaner.auth.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopConnectivityTest {
    @Test
    fun `a listening port is reachable and a closed one is not`() = runBlocking {
        val port = ServerSocket(0).use { server ->
            assertTrue(DesktopConnectivityRepository("localhost", server.localPort).observeOnline().first())
            server.localPort
        }
        assertFalse(isReachable("localhost", port))
    }
}
