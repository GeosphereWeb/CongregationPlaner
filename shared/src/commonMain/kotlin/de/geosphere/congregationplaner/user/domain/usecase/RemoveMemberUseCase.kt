package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.model.familyOf
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Der Versammlungsadmin [user] entfernt [member] aus der Versammlung. War [member] Familienoberhaupt, bekommt seine
 * Familie [newFamilyHead] als Oberhaupt (beim Verknüpfen mit einem Konto); ohne neues Oberhaupt löst sie sich auf.
 * Die Familie wird vor dem Entfernen umgestellt, damit kein Mitglied auf ein entferntes Oberhaupt zeigt.
 */
@Inject
class RemoveMemberUseCase(private val repository: UserRepository, private val policy: UserPermissionPolicy) {
    suspend operator fun invoke(user: AppUser, member: AppUser, newFamilyHead: String? = null): Result<Unit> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        val congregation = user.congregation
        if (congregation == null || !policy.canEditMember(user, member)) {
            return Result.failure(MemberProfileException.NotAllowed())
        }
        return runCatching {
            familyOf(repository.observeMembers(congregation).first(), member.uid).forEach { relative ->
                val profile = relative.profile.copy(familyHeadId = newFamilyHead).normalized()
                repository.updateMemberProfile(congregation, relative.copy(profile = profile)).getOrThrow()
            }
        }.mapCatching { repository.removeMember(congregation, member.uid).getOrThrow() }
    }
}
