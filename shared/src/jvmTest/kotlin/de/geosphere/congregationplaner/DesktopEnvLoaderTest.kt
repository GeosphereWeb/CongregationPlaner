package de.geosphere.congregationplaner

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopEnvLoaderTest {
    @Test
    fun `desktop env loader prefers explicit system properties and dot notation fallback`() {
        val previousWeb = System.getProperty("FIREBASE_WEB_API_KEY")
        val previousWebDot = System.getProperty("firebase.webApiKey")
        val previousProject = System.getProperty("FIREBASE_PROJECT_ID")
        val previousProjectDot = System.getProperty("firebase.projectId")

        try {
            System.setProperty("FIREBASE_WEB_API_KEY", "explicit-web-key")
            System.setProperty("firebase.webApiKey", "fallback-web-key")
            System.setProperty("FIREBASE_PROJECT_ID", "explicit-project")
            System.setProperty("firebase.projectId", "fallback-project")

            assertEquals("explicit-web-key", DesktopEnvLoader.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey"))
            assertEquals("explicit-project", DesktopEnvLoader.getValue("FIREBASE_PROJECT_ID", "firebase.projectId"))
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousWeb)
            restoreSystemProperty("firebase.webApiKey", previousWebDot)
            restoreSystemProperty("FIREBASE_PROJECT_ID", previousProject)
            restoreSystemProperty("firebase.projectId", previousProjectDot)
        }
    }

    @Test
    fun `desktop env loader reads values from env file and ignores comments and malformed lines`() {
        val rootDir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val envFile = File(rootDir, ".env")
        val previousEnv = if (envFile.exists()) envFile.readText() else null

        try {
            envFile.writeText(
                "# comment\n" +
                    "FIREBASE_WEB_API_KEY=\"quoted-web-key\"\n" +
                    "firebase.projectId='quoted-project'\n" +
                    "BROKEN\n" +
                    "EMPTY=\n" +
                    "VALID_KEY=value\n",
            )

            val loaded = DesktopEnvLoader.load()

            assertEquals("quoted-web-key", loaded["FIREBASE_WEB_API_KEY"])
            assertEquals("quoted-project", loaded["firebase.projectId"])
            assertEquals("value", loaded["VALID_KEY"])
            assertNull(loaded["BROKEN"])
            assertEquals("", loaded["EMPTY"])
        } finally {
            if (previousEnv == null) {
                envFile.delete()
            } else {
                envFile.writeText(previousEnv)
            }
        }
    }

    @Test
    fun `desktop env loader returns null when config is absent`() {
        val previousWeb = System.getProperty("FIREBASE_WEB_API_KEY")
        val previousWebDot = System.getProperty("firebase.webApiKey")
        val previousProject = System.getProperty("FIREBASE_PROJECT_ID")
        val previousProjectDot = System.getProperty("firebase.projectId")

        try {
            System.clearProperty("FIREBASE_WEB_API_KEY")
            System.clearProperty("firebase.webApiKey")
            System.clearProperty("FIREBASE_PROJECT_ID")
            System.clearProperty("firebase.projectId")

            assertNull(DesktopEnvLoader.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey"))
            assertNull(DesktopEnvLoader.getValue("FIREBASE_PROJECT_ID", "firebase.projectId"))
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousWeb)
            restoreSystemProperty("firebase.webApiKey", previousWebDot)
            restoreSystemProperty("FIREBASE_PROJECT_ID", previousProject)
            restoreSystemProperty("firebase.projectId", previousProjectDot)
        }
    }

    private fun restoreSystemProperty(key: String, previousValue: String?) {
        if (previousValue == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, previousValue)
        }
    }
}
