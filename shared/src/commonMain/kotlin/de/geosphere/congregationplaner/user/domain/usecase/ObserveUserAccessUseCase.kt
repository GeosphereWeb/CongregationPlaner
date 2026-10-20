package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Ermittelt den [UserAccessState] für den angemeldeten Firebase-Nutzer in der gewählten Versammlung und
 * hält ihn aktuell, sodass Freigaben und Rollenänderungen ohne erneute Anmeldung wirksam werden.
 */
@Inject
class ObserveUserAccessUseCase(private val userRepository: UserRepository) {
    operator fun invoke(authUser: FirebaseUser?, congregation: CongregationRef?): Flow<UserAccessState> {
        if (authUser == null) return flowOf(UserAccessState.Unauthenticated)
        if (congregation == null) return flowOf(UserAccessState.CongregationRequired)

        return flow {
            emit(UserAccessState.Loading)
            val isGlobalAdmin = userRepository.isGlobalAdmin(authUser.uid).getOrElse { exception ->
                emit(UserAccessState.Error(exception.message ?: DEFAULT_ERROR_MESSAGE))
                return@flow
            }
            val user = userRepository.getOrCreateUser(authUser, congregation).getOrElse { exception ->
                emit(UserAccessState.Error(exception.message ?: DEFAULT_ERROR_MESSAGE))
                return@flow
            }
            emit(user.copy(isGlobalAdmin = isGlobalAdmin).toAccessState())
            emitAll(
                userRepository.observeUser(congregation, authUser.uid).map { observed ->
                    observed?.copy(isGlobalAdmin = isGlobalAdmin)?.toAccessState()
                        ?: UserAccessState.Error(MISSING_USER_MESSAGE)
                },
            )
        }.catch { exception ->
            if (exception is CancellationException) throw exception
            emit(UserAccessState.Error(exception.message ?: DEFAULT_ERROR_MESSAGE))
        }
    }

    // Globale Admins brauchen keine Freigabe durch den Versammlungsadmin.
    private fun AppUser.toAccessState(): UserAccessState = if (approved || isGlobalAdmin) {
        UserAccessState.Authorized(this)
    } else {
        UserAccessState.NeedsApproval(this)
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "Nutzerstatus konnte nicht ermittelt werden."
        const val MISSING_USER_MESSAGE = "Nutzerprofil wurde nicht gefunden."
    }
}
