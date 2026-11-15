package de.geosphere.congregationplaner.notification.di

import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.notification.data.createNotificationRepository
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.notification.domain.usecase.CheckNewNotificationsUseCase
import de.geosphere.congregationplaner.organization.data.createCongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraph

/** Für die Abfrage im Hintergrund, ohne geöffnete Oberfläche (ADR-0035). */
@DependencyGraph
interface NotificationGraph {
    val checkNewNotifications: CheckNewNotificationsUseCase

    @Provides
    fun provideAuthRepository(): FirebaseAuthRepository = FirebaseAuthManager

    @Provides
    fun provideCongregationSelectionStore(): CongregationSelectionStore = createCongregationSelectionStore()

    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()

    @Provides
    fun provideNotificationRepository(): NotificationRepository = createNotificationRepository()
}

/** Für Plattformmodule ohne Metro-Compiler-Plugin (Android-Worker). */
fun createCheckNewNotificationsUseCase(): CheckNewNotificationsUseCase =
    createGraph<NotificationGraph>().checkNewNotifications
