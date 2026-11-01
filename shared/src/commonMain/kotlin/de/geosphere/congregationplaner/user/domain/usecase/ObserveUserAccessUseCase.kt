package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Ermittelt den [UserAccessState] für den angemeldeten Firebase-Nutzer in der gewählten Versammlung und
 * hält ihn aktuell, sodass Freigaben und Rollenänderungen ohne erneute Anmeldung wirksam werden.
 *
 * Globale Admins brauchen weder Versammlung noch Freigabe; die App legt für sie kein Mitgliedsdokument an und sie
 * sehen keine Versammlungsdaten (ADR-0013). Nur wenn sie dort ausdrücklich Mitglied sind, gelten ihre Rollen.
 */
@Inject
class ObserveUserAccessUseCase(private val userRepository: UserRepository) {
    operator fun invoke(authUser: FirebaseUser?, congregation: CongregationRef?): Flow<UserAccessState> {
        if (authUser == null) return flowOf(UserAccessState.Unauthenticated)

        return flow {
            emit(UserAccessState.Loading)
            val isGlobalAdmin = userRepository.isGlobalAdmin(authUser.uid).getOrElse { exception ->
                emit(UserAccessState.Error(exception.message ?: DEFAULT_ERROR_MESSAGE))
                return@flow
            }
            when {
                isGlobalAdmin -> emitAll(globalAdminAccess(authUser, congregation))
                congregation == null -> emit(UserAccessState.CongregationRequired)
                else -> memberAccess(authUser, congregation)
            }
        }.catch { exception ->
            if (exception is CancellationException) throw exception
            emit(UserAccessState.Error(exception.message ?: DEFAULT_ERROR_MESSAGE))
        }
    }

    private suspend fun FlowCollector<UserAccessState>.memberAccess(
        authUser: FirebaseUser,
        congregation: CongregationRef,
    ) {
        val user = userRepository.getOrCreateUser(authUser, congregation).getOrElse { exception ->
            emit(UserAccessState.Error(exception.message ?: DEFAULT_ERROR_MESSAGE))
            return
        }
        emit(user.toAccessState())
        emitAll(
            userRepository.observeUser(congregation, authUser.uid).map { observed ->
                observed?.toAccessState() ?: UserAccessState.Error(MISSING_USER_MESSAGE)
            },
        )
    }

    private fun globalAdminAccess(authUser: FirebaseUser, congregation: CongregationRef?): Flow<UserAccessState> {
        val globalOnly = AppUser(
            uid = authUser.uid,
            email = authUser.email,
            displayName = authUser.displayName,
            approved = true,
            congregation = congregation,
            isGlobalAdmin = true,
        )
        if (congregation == null) return flowOf(UserAccessState.Authorized(globalOnly))
        // Ein Mitgliedsdokument wird nur beobachtet, nie angelegt: Rollen gelten nur dort, wo er ausdrücklich
        // Mitglied ist.
        return userRepository.observeUser(congregation, authUser.uid).map { member ->
            UserAccessState.Authorized((member ?: globalOnly).copy(isGlobalAdmin = true))
        }
    }

    private fun AppUser.toAccessState(): UserAccessState = if (approved) {
        UserAccessState.Authorized(this)
    } else {
        UserAccessState.NeedsApproval(this)
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "Nutzerstatus konnte nicht ermittelt werden."
        const val MISSING_USER_MESSAGE = "Nutzerprofil wurde nicht gefunden."
    }
}
