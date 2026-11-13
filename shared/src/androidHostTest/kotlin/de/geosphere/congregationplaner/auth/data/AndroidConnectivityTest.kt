package de.geosphere.congregationplaner.auth.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidConnectivityTest {
    private val manager = mockk<ConnectivityManager>()
    private val network = mockk<Network>()
    private val callback = slot<ConnectivityManager.NetworkCallback>()

    @AfterTest
    fun tearDown() {
        FirebaseAndroidContextHolder.context = null
    }

    private fun capabilities(internet: Boolean, validated: Boolean) = mockk<NetworkCapabilities> {
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
    }

    @Test
    fun `without a context the app counts as online`() = runBlocking {
        assertTrue(createConnectivityRepository().observeOnline().first())
    }

    @Test
    fun `the current network and its changes are reported until the flow ends`() = runBlocking {
        FirebaseAndroidContextHolder.context = mockk<Context> {
            every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns manager
        }
        every { manager.activeNetwork } returns network
        every { manager.getNetworkCapabilities(network) } returns capabilities(internet = true, validated = false)
        every { manager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just runs
        every { manager.registerDefaultNetworkCallback(capture(callback)) } answers {
            callback.captured.onCapabilitiesChanged(network, capabilities(internet = true, validated = true))
            callback.captured.onLost(network)
        }

        val states = createConnectivityRepository().observeOnline().take(3).toList()

        assertEquals(listOf(false, true, false), states)
        verify { manager.unregisterNetworkCallback(callback.captured) }
    }
}
