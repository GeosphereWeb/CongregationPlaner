package de.geosphere.congregationplaner.user.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveUserAccessUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class UserAccessViewModel(
    private val observeUserAccess: ObserveUserAccessUseCase,
    private val userPermissionPolicy: UserPermissionPolicy,
) : ViewModel() {
    private val session = MutableStateFlow<AuthSession>(AuthSession.Unknown)

    val accessState: StateFlow<UserAccessState> = session
        .flatMapLatest { session ->
            when (session) {
                AuthSession.Unknown -> flowOf(UserAccessState.Loading)
                AuthSession.SignedOut -> flowOf(UserAccessState.Unauthenticated)
                is AuthSession.SignedIn -> observeUserAccess(session.user)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UserAccessState.Loading)

    fun onSignedIn(user: FirebaseUser) {
        session.value = AuthSession.SignedIn(user)
    }

    fun onSignedOut() {
        session.value = AuthSession.SignedOut
    }

    /** Liest den Nutzerstatus erneut vom Server, z. B. nachdem ein Admin die Freigabe erteilt hat. */
    fun refresh() {
        session.update { current ->
            if (current is AuthSession.SignedIn) current.copy(attempt = current.attempt + 1) else current
        }
    }

    fun canManageUsers(user: AppUser): Boolean = userPermissionPolicy.canManageUsers(user)

    private sealed interface AuthSession {
        data object Unknown : AuthSession

        data object SignedOut : AuthSession

        data class SignedIn(val user: FirebaseUser, val attempt: Int = 0) : AuthSession
    }
}
