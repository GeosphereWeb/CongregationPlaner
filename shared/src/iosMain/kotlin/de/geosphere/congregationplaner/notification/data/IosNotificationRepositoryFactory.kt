package de.geosphere.congregationplaner.notification.data

import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository

actual fun createNotificationRepository(): NotificationRepository = unavailableNotificationRepository("iOS")
