package de.geosphere.congregationplaner.notification.data

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class UnavailableNotificationRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    @Test
    fun `unavailable notification repository fails with the platform name`(): Unit = runBlocking {
        val repository = unavailableNotificationRepository("iOS")

        val send = repository.send(
            congregation,
            "uid",
            AppNotification(NotificationCategory.TALK_PLANNING, "Titel", groups = setOf(EventAudience.ALL)),
        ).exceptionOrNull()
        assertIs<UnsupportedOperationException>(send)
        assertEquals("Firestore is not configured for iOS.", send.message)
        val fetch = repository.fetchSince(congregation, setOf(EventAudience.ALL), emptySet(), Instant.DISTANT_PAST)
        assertIs<UnsupportedOperationException>(fetch.exceptionOrNull())
    }
}
