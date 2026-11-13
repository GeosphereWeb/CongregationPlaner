package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/**
 * Redner (Mitglieder mit `isSpeaker`) und Vorsitzende (Brüder mit Dienstamt) der Versammlung, jeweils mit
 * Familiennamen und nach Namen sortiert. Beide kommen aus einer gemeinsamen Beobachtung der Mitglieder. Das Lesen
 * der Mitglieder erfordert Adminrechte oder eine Planerrolle (ADR-0012), sonst lehnen die Rules ab.
 */
@Inject
class ObserveTalkMembersUseCase(private val observeMembers: ObserveMembersUseCase) {
    operator fun invoke(user: AppUser): Flow<TalkMembers> = user.congregation?.let { congregation ->
        observeMembers(congregation).map { members ->
            val named = members.filter { it.profile.lastName.isNotBlank() }
            TalkMembers(
                speakers = named.filter { it.profile.isSpeaker }.toRefs(),
                chairmen = named.filter { it.profile.canBeChairman }.toRefs(),
            )
        }
    } ?: emptyFlow()

    private fun List<AppUser>.toRefs(): List<MemberRef> = map {
        with(it.profile) { MemberRef(it.uid, lastName, firstName, mobilePhone, landlinePhone, talkNumbers) }
    }.sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))
}
