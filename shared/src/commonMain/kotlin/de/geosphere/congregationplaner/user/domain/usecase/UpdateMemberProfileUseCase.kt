package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateMemberProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(congregation: CongregationRef, member: AppUser): Result<AppUser> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        // Bei Personen ohne Konto ist der Name die einzige Möglichkeit, sie zu erkennen.
        return member.profile.validated(requireName = !member.hasAccount).fold(
            onSuccess = { profile -> repository.updateMemberProfile(congregation, member.copy(profile = profile)) },
            onFailure = { Result.failure(it) },
        )
    }
}

/** Trimmt die Eingaben und prüft sie gegen dieselben Regeln wie die Firestore Security Rules. */
internal fun MemberProfile.validated(requireName: Boolean): Result<MemberProfile> {
    val trimmed = copy(
        lastName = lastName.trim(),
        firstName = firstName.trim(),
        mobilePhone = mobilePhone.trim(),
        landlinePhone = landlinePhone.trim(),
    )
    val error = when {
        requireName && (trimmed.lastName.isEmpty() || trimmed.firstName.isEmpty()) ->
            MemberProfileException.MissingName()
        maxOf(trimmed.lastName.length, trimmed.firstName.length) > MemberProfile.MAX_NAME_LENGTH ->
            MemberProfileException.NameTooLong()
        trimmed.category == MemberCategory.CHILD && trimmed.baptized -> MemberProfileException.BaptizedChild()
        trimmed.appointment != Appointment.NONE && !trimmed.canHaveAppointment ->
            MemberProfileException.AppointmentNotAllowed()
        !MemberProfile.isValidPhone(trimmed.mobilePhone) || !MemberProfile.isValidPhone(trimmed.landlinePhone) ->
            MemberProfileException.InvalidPhone()
        else -> null
    }
    return error?.let { Result.failure(it) } ?: Result.success(trimmed)
}
