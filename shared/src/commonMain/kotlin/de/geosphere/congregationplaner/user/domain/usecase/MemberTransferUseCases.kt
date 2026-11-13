package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.SlotPerson
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveSlotsToMemberUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import dev.zacsweers.metro.Inject

/**
 * Übernehmen und Verknüpfen schreiben Mitglieder (Versammlungsadmin) und Termine (Vortragsplaner); es braucht also
 * beide Rollen.
 */
@Inject
class MemberTransferPolicy(
    private val userPolicy: UserPermissionPolicy,
    private val talkPolicy: TalkSchedulePermissionPolicy,
) {
    fun canTransfer(user: AppUser): Boolean =
        userPolicy.canCreateMemberWithoutAccount(user) && talkPolicy.canPlanTalks(user)
}

/** Die Personen ohne Konto, mit denen sich ein Konto verknüpfen lässt, nach Namen sortiert. */
@Inject
class FindLinkCandidatesUseCase {
    operator fun invoke(members: List<AppUser>): List<AppUser> =
        members.filter { !it.hasAccount }.sortedBy { it.label.lowercase() }
}

/** Ergebnis der Übernahme: die neuen Mitglieder und je Redner, der nicht übernommen wurde, der Grund. */
data class SpeakerAdoption(val adopted: List<AppUser>, val failures: List<String>)

/**
 * Übernimmt Redner aus dem Rednerverzeichnis als Personen ohne Konto in die eigene Versammlung: Bruder, getauft,
 * Redner, mit den Telefonnummern und der E-Mail-Adresse, soweit sie gültig sind. Danach zeigen seine Termine auf das
 * neue Mitglied, und der Eintrag im Verzeichnis entfällt. Die Reihenfolge verliert bei einem Fehler nichts: Der Redner
 * bleibt im Verzeichnis, bis seine Termine umgestellt sind.
 */
@Inject
class AdoptVisitingSpeakersUseCase(
    private val createMember: CreateMemberWithoutAccountUseCase,
    private val moveSlots: MoveSlotsToMemberUseCase,
    private val deleteSpeaker: DeleteVisitingSpeakerUseCase,
    private val policy: MemberTransferPolicy,
) {
    suspend operator fun invoke(user: AppUser, speakers: List<VisitingSpeaker>): Result<SpeakerAdoption> {
        if (!policy.canTransfer(user)) return Result.failure(MemberProfileException.NotAllowed())
        val adopted = mutableListOf<AppUser>()
        val failures = mutableListOf<String>()
        speakers.forEach { speaker ->
            adopt(user, speaker).fold(
                onSuccess = { adopted += it },
                onFailure = { failures += "${speaker.label()}: ${it.message ?: "nicht übernommen"}" },
            )
        }
        return Result.success(SpeakerAdoption(adopted, failures))
    }

    private suspend fun adopt(user: AppUser, speaker: VisitingSpeaker): Result<AppUser> =
        createMember(user, speaker.toMemberProfile()).mapCatching { member ->
            moveSlots(user, SlotPerson.External(speaker.id), member.toMemberRef()).getOrThrow()
            deleteSpeaker(user, speaker.id).getOrThrow()
            member
        }
}

/**
 * Verknüpft [account] (ein Mitglied mit Konto) mit [person] (ohne Konto): Die Angaben der Person gehen auf das Konto
 * über, ihre Termine zeigen danach auf das Konto, und die Person ohne Konto wird entfernt. Bei einem Fehler bleibt die
 * Person bestehen, damit sich der Vorgang wiederholen lässt.
 */
@Inject
class LinkMemberAccountUseCase(
    private val updateProfile: UpdateMemberProfileUseCase,
    private val moveSlots: MoveSlotsToMemberUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val policy: MemberTransferPolicy,
) {
    suspend operator fun invoke(user: AppUser, account: AppUser, person: AppUser): Result<AppUser> = when {
        !policy.canTransfer(user) -> Result.failure(MemberProfileException.NotAllowed())
        !account.hasAccount || person.hasAccount || account.uid == person.uid ->
            Result.failure(IllegalArgumentException("Bitte ein Mitglied mit Konto und eine Person ohne Konto wählen."))
        else -> updateProfile(user, account.copy(profile = person.profile)).mapCatching { linked ->
            moveSlots(user, SlotPerson.Member(person.uid), linked.toMemberRef()).getOrThrow()
            removeMember(user, person).getOrThrow()
            linked
        }
    }
}

private fun AppUser.toMemberRef() = MemberRef(uid, profile.lastName, profile.firstName)

private fun VisitingSpeaker.label(): String = listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ")

/**
 * Ein übernommener Redner ist ein getaufter Bruder und behält seine Vorträge; ungültige Telefonnummern und Adressen
 * bleiben leer.
 */
internal fun VisitingSpeaker.toMemberProfile(): MemberProfile = MemberProfile(
    lastName = lastName,
    firstName = firstName,
    category = MemberCategory.BROTHER,
    baptized = true,
    isSpeaker = true,
    mobilePhone = mobilePhone.takeIf(MemberProfile::isValidPhone).orEmpty(),
    landlinePhone = landlinePhone.takeIf(MemberProfile::isValidPhone).orEmpty(),
    emails = listOf(email).filter { it.isNotBlank() && MemberProfile.isValidEmail(it) },
    talkNumbers = talkNumbers,
)
