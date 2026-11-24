package de.geosphere.congregationplaner.firestore

import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopPollingTest {
    private val interval = 60_000L

    private fun signals(active: Boolean = true) = PollingSignals(MutableStateFlow(active), MutableStateFlow(0L))

    @Test
    fun `polls again only after the interval`() = runTest {
        val signals = signals()
        var fetches = 0
        val job = launch {
            pollingFlow(interval, signals, StandardTestDispatcher(testScheduler)) { ++fetches }.collect {}
        }

        runCurrent()
        assertEquals(1, fetches)
        advanceTimeBy(interval - 1)
        runCurrent()
        assertEquals(1, fetches)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(2, fetches)
        job.cancel()
    }

    @Test
    fun `an own write polls at once`() = runTest {
        val signals = signals()
        var fetches = 0
        val job = launch {
            pollingFlow(interval, signals, StandardTestDispatcher(testScheduler)) { ++fetches }.collect {}
        }
        runCurrent()

        signals.writes.value++
        runCurrent()

        assertEquals(2, fetches)
        job.cancel()
    }

    @Test
    fun `an inactive window pauses polling and its return polls at once`() = runTest {
        val signals = signals(active = false)
        var fetches = 0
        val job = launch {
            pollingFlow(interval, signals, StandardTestDispatcher(testScheduler)) { ++fetches }.collect {}
        }

        advanceTimeBy(interval * 5)
        runCurrent()
        assertEquals(0, fetches)

        signals.active.value = true
        runCurrent()
        assertEquals(1, fetches)
        job.cancel()
    }

    @Test
    fun `the window reports whether it is active`() {
        DesktopPolling.setWindowActive(false)
        assertEquals(false, DesktopPolling.signals.active.value)

        DesktopPolling.setWindowActive(true)
        assertEquals(true, DesktopPolling.signals.active.value)
    }

    @Test
    fun `writes signal a refresh but reads do not`() {
        FakeFirestoreRestServer { FakeResponse(200, "{}") }.use { server ->
            withFirestoreRestConfig(server) {
                val client = FirestoreRestClient(FirestoreRestClient.defaultHttpClient(), { "token" }, null)
                val before = DesktopPolling.signals.writes.value

                client.request("GET", client.collectionUrl("a"))
                client.request("POST", "${client.collectionUrl("a")}:runQuery", "{}")
                assertEquals(before, DesktopPolling.signals.writes.value)

                client.request("PATCH", client.documentUrl("a", "b"), "{}")
                client.commit(emptyList())
                assertTrue(DesktopPolling.signals.writes.value >= before + 2)
            }
        }
    }
}
