package de.geosphere.congregationplaner.firestore

import de.geosphere.congregationplaner.auth.DesktopEnv
import de.geosphere.congregationplaner.auth.authEmulatorEnabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreBackendSwitchTest {
    @Test
    fun `the emulator url points at the emulator host`() {
        assertEquals(
            "http://127.0.0.1:8080/v1/projects/demo/databases/(default)/documents",
            firestoreDocumentsUrl("demo", useEmulator = true, emulatorHost = "127.0.0.1:8080"),
        )
    }

    @Test
    fun `the remote url points at the online firestore and ignores the emulator host`() {
        assertEquals(
            "https://firestore.googleapis.com/v1/projects/congregation-planer/databases/(default)/documents",
            firestoreDocumentsUrl("congregation-planer", useEmulator = false, emulatorHost = "127.0.0.1:8080"),
        )
    }

    @Test
    fun `the switch accepts true and false in any case and ignores everything else`() {
        assertEquals(true, DesktopEnv.parseUseEmulator("true"))
        assertEquals(true, DesktopEnv.parseUseEmulator(" TRUE "))
        assertEquals(false, DesktopEnv.parseUseEmulator("false"))
        assertEquals(false, DesktopEnv.parseUseEmulator("False"))
        assertNull(DesktopEnv.parseUseEmulator(null))
        assertNull(DesktopEnv.parseUseEmulator(""))
        assertNull(DesktopEnv.parseUseEmulator("maybe"))
    }

    @Test
    fun `the switch beats the single auth emulator variables`() {
        assertFalse(authEmulatorEnabled(useEmulator = false, explicitEnabled = "true", host = "h", port = "1"))
        assertTrue(authEmulatorEnabled(useEmulator = true, explicitEnabled = "false", host = null, port = null))
    }

    @Test
    fun `without the switch the auth emulator follows its own variables`() {
        assertTrue(authEmulatorEnabled(null, explicitEnabled = "true", host = null, port = null))
        assertFalse(authEmulatorEnabled(null, explicitEnabled = "false", host = "127.0.0.1", port = "9099"))
        assertTrue(authEmulatorEnabled(null, explicitEnabled = null, host = "127.0.0.1", port = null))
        assertTrue(authEmulatorEnabled(null, explicitEnabled = null, host = null, port = "9099"))
        assertFalse(authEmulatorEnabled(null, explicitEnabled = null, host = null, port = null))
        assertFalse(authEmulatorEnabled(null, explicitEnabled = null, host = " ", port = ""))
    }
}
