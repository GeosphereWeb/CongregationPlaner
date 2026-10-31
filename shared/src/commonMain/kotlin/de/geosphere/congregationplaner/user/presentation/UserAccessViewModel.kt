package de.geosphere.congregationplaner.user.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.GetAdminContactsUseCase
import de.geosphere.congregationplaner.user.domain.usecase.ObserveUserAccessUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class UserAccessViewModel(
    private val observeUserAccess: ObserveUserAccessUseCase,
    private val userPermissionPolicy: UserPermissionPolicy,
    private val getAdminContacts: GetAdminContactsUseCase,
) : ViewModel() {
    private val session = MutableStateFlow<AuthSession>(AuthSession.Unknown)
    private val contactsRetry = MutableStateFlow(0)

    val accessState: StateFlow<UserAccessState> = session
        .flatMapLatest { session ->
            when (session) {
                AuthSession.Unknown -> flowOf(UserAccessState.Loading)
                AuthSession.SignedOut -> flowOf(UserAccessState.Unauthenticated)
                is AuthSession.SignedIn -> observeUserAccess(session.user, session.congregation)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UserAccessState.Loading)

    /** Die Admins der Versammlung, solange der Nutzer auf seine Freigabe wartet. */
    val adminContacts: StateFlow<AdminContactsState> = combine(accessState, session, contactsRetry) {
            state,
            session,
            _,
        ->
        (session as? AuthSession.SignedIn)?.congregation.takeIf { state is UserAccessState.NeedsApproval }
    }
        .flatMapLatest { congregation ->
            if (congregation == null) {
                flowOf(AdminContactsState.Loading)
            } else {
                flow<AdminContactsState> {
                    emit(AdminContactsState.Loading)
                    emit(
                        getAdminContacts(congregation).fold(
                            onSuccess = { AdminContactsState.Loaded(it) },
                            onFailure = { AdminContactsState.Failed },
                        ),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminContactsState.Loading)

    /** [congregation] ist die im Login gewählte Versammlung; `null`, solange noch keine gewählt wurde. */
    fun onSignedIn(user: FirebaseUser, congregation: CongregationRef?) {
        session.value = AuthSession.SignedIn(user, congregation, retry = 0)
    }

    fun onSignedOut() {
        session.value = AuthSession.SignedOut
    }

    /** Liest den Nutzerstatus erneut vom Server, z. B. nachdem ein Admin die Freigabe erteilt hat. */
    fun refresh() {
        contactsRetry.update { it + 1 }
        session.update { current ->
            if (current is AuthSession.SignedIn) current.copy(retry = current.retry + 1) else current
        }
    }

    fun canManageUsers(user: AppUser): Boolean = userPermissionPolicy.canManageUsers(user)

    private sealed interface AuthSession {
        data object Unknown : AuthSession

        data object SignedOut : AuthSession

        data class SignedIn(val user: FirebaseUser, val congregation: CongregationRef?, val retry: Int) : AuthSession
    }
}
