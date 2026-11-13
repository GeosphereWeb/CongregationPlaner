package de.geosphere.congregationplaner.auth.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class FirebaseStatusTextTest {
    @Test
    fun `the status names start and missing configuration and missing connection and availability`() {
        assertEquals("Firebase wird initialisiert...", firebaseStatusText(ready = null, online = false))
        assertEquals("Firebase nicht konfiguriert", firebaseStatusText(ready = false, online = true))
        assertEquals("Keine Internetverbindung", firebaseStatusText(ready = true, online = false))
        assertEquals("Firebase verfügbar", firebaseStatusText(ready = true, online = true))
    }
}
