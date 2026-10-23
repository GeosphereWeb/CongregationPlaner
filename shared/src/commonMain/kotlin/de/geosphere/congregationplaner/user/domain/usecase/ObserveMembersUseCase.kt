package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Beobachtet die Mitglieder einer Versammlung; noch nicht freigegebene Nutzer stehen oben, danach wird nach
 * Familienname sortiert. Personen ohne Konto warten nie auf eine Freigabe.
 */
@Inject
class ObserveMembersUseCase(private val repository: UserRepository) {
    operator fun invoke(congregation: CongregationRef): Flow<List<AppUser>> = repository.observeMembers(congregation)
        .map { members -> members.sortedWith(memberOrder) }

    private companion object {
        val memberOrder = compareBy<AppUser> { !it.hasAccount || it.approved }
            .thenBy { it.profile.lastName.ifBlank { it.label }.lowercase() }
            .thenBy { it.label.lowercase() }
    }
}
