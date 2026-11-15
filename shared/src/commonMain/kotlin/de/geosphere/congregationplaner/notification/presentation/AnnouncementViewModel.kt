package de.geosphere.congregationplaner.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.permission.NotificationPermissionPolicy
import de.geosphere.congregationplaner.notification.domain.usecase.SendNotificationUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Verfassen und Senden einer Mitteilung durch Mitglieder mit der Rolle „Benachrichtigungen senden“ (ADR-0035). */
@Inject
class AnnouncementViewModel(
    private val sendNotification: SendNotificationUseCase,
    private val policy: NotificationPermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AnnouncementUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        mutableUiState.update { it.copy(canSend = policy.canSend(user, NotificationCategory.ANNOUNCEMENT)) }
    }

    fun updateDraft(draft: AnnouncementDraft) {
        mutableUiState.update { if (it.isSending) it else it.copy(draft = draft.limited(), sentMessage = null) }
    }

    fun send() {
        val state = mutableUiState.value
        val user = currentUser
        if (!state.canSubmit || user == null) return
        mutableUiState.update { it.copy(isSending = true, errorMessage = null, sentMessage = null) }
        viewModelScope.launch {
            sendNotification(user, state.draft.toNotification())
                .onSuccess {
                    mutableUiState.update {
                        it.copy(draft = AnnouncementDraft(), isSending = false, sentMessage = "Mitteilung gesendet.")
                    }
                }.onFailure { exception ->
                    mutableUiState.update {
                        it.copy(isSending = false, errorMessage = exception.message ?: "Senden fehlgeschlagen.")
                    }
                }
        }
    }

    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
    }
}
