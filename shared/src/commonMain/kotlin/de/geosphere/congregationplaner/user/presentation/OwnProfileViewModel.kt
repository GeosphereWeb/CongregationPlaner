package de.geosphere.congregationplaner.user.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.usecase.ObserveOwnPrivateDetailsUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveOwnPrivateDetailsUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateOwnProfileUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die Felder von „Mein Profil“: Namen, Telefon und E-Mail aus dem Mitgliedsdokument, dazu die privaten Angaben. */
data class OwnProfileForm(
    val lastName: String = "",
    val firstName: String = "",
    val mobilePhone: String = "",
    val landlinePhone: String = "",
    val emails: List<String> = emptyList(),
    val details: MemberPrivateDetails = MemberPrivateDetails(),
) {
    internal val profilePart get() = copy(details = MemberPrivateDetails())
}

/**
 * Die eigenen Angaben unter „Einstellungen“: [form] sind die Eingaben, [saved] der gespeicherte Stand. Speichern geht
 * erst, wenn die privaten Angaben geladen sind ([detailsLoaded]); sonst würde ein leeres Formular sie überschreiben.
 */
data class OwnProfileUiState(
    val form: OwnProfileForm = OwnProfileForm(),
    val saved: OwnProfileForm = OwnProfileForm(),
    val detailsLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    val isDirty: Boolean get() = form != saved
    val canSave: Boolean
        get() = isDirty && detailsLoaded && !isSaving && form.lastName.isNotBlank() && form.firstName.isNotBlank()
    val canAddContact: Boolean get() = form.details.emergencyContacts.size < MemberPrivateDetails.MAX_EMERGENCY_CONTACTS

    /** Das erste E-Mail-Feld ist immer da, auch ohne gespeicherte Adresse; weitere kommen bei Bedarf dazu. */
    val emailFields: List<String> get() = form.emails.ifEmpty { listOf("") }
    val canAddEmail: Boolean get() = emailFields.size < MemberProfile.MAX_EMAILS
}

@Inject
class OwnProfileViewModel(
    private val updateOwnProfile: UpdateOwnProfileUseCase,
    private val observeOwnPrivateDetails: ObserveOwnPrivateDetailsUseCase,
    private val saveOwnPrivateDetails: SaveOwnPrivateDetailsUseCase,
) : ViewModel() {
    private var user: AppUser? = null
    private var detailsJob: Job? = null
    private val mutableUiState = MutableStateFlow(OwnProfileUiState())
    val uiState = mutableUiState.asStateFlow()

    /** Übernimmt den gespeicherten Stand; ungespeicherte Eingaben gehen dabei nicht verloren. */
    fun onUserChanged(newUser: AppUser) {
        val previousUid = user?.uid
        user = newUser
        val profile = newUser.profile
        mutableUiState.update { state ->
            val saved = state.saved.copy(
                lastName = profile.lastName,
                firstName = profile.firstName,
                mobilePhone = profile.mobilePhone,
                landlinePhone = profile.landlinePhone,
                emails = profile.emails,
            )
            val form = if (state.form.profilePart == state.saved.profilePart) {
                saved.copy(details = state.form.details)
            } else {
                state.form
            }
            state.copy(form = form, saved = saved)
        }
        if (newUser.uid != previousUid) observeDetails(newUser)
    }

    fun onLastNameChange(value: String) = updateForm { it.copy(lastName = value) }

    fun onFirstNameChange(value: String) = updateForm { it.copy(firstName = value) }

    fun onMobilePhoneChange(value: String) = updateForm { it.copy(mobilePhone = value) }

    fun onLandlinePhoneChange(value: String) = updateForm { it.copy(landlinePhone = value) }

    fun onEmailChange(index: Int, value: String) = updateForm { form ->
        val fields = form.emails.ifEmpty { listOf("") }
        // Bleibt nur ein leeres erstes Feld übrig, gibt es keine Adresse; so wird das Formular nicht unnötig geändert.
        form.copy(
            emails = fields.mapIndexed { i, old ->
                if (i ==
                    index
                ) {
                    value
                } else {
                    old
                }
            }.takeUnless { it == listOf("") }.orEmpty(),
        )
    }

    fun addEmail() {
        if (!mutableUiState.value.canAddEmail) return
        updateForm { it.copy(emails = it.emails.ifEmpty { listOf("") } + "") }
    }

    /** Das erste Feld bleibt immer; entfernt werden nur weitere. */
    fun removeEmail(index: Int) {
        if (index == 0) return
        updateForm { form -> form.copy(emails = form.emails.filterIndexed { i, _ -> i != index }) }
    }

    fun onStreetChange(value: String) = updateDetails { it.copy(street = value) }

    fun onPostalCodeChange(value: String) = updateDetails { it.copy(postalCode = value) }

    fun onCityChange(value: String) = updateDetails { it.copy(city = value) }

    fun onContactChange(index: Int, contact: EmergencyContact) = updateDetails { details ->
        details.copy(
            emergencyContacts = details.emergencyContacts.mapIndexed { i, old ->
                if (i ==
                    index
                ) {
                    contact
                } else {
                    old
                }
            },
        )
    }

    fun addContact() {
        if (!mutableUiState.value.canAddContact) return
        updateDetails { it.copy(emergencyContacts = it.emergencyContacts + EmergencyContact()) }
    }

    fun removeContact(index: Int) = updateDetails { details ->
        details.copy(emergencyContacts = details.emergencyContacts.filterIndexed { i, _ -> i != index })
    }

    /** Speichert erst das Mitgliedsdokument, dann die privaten Angaben; jeweils nur, was sich geändert hat. */
    fun save() {
        val current = user ?: return
        val state = mutableUiState.value
        if (!state.canSave) return
        val form = state.form
        mutableUiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            var saved = state.saved
            if (form.profilePart != state.saved.profilePart) {
                val result =
                    updateOwnProfile(
                        current,
                        form.lastName,
                        form.firstName,
                        form.mobilePhone,
                        form.landlinePhone,
                        form.emails,
                    )
                val profile = result.getOrElse { return@launch fail(it) }.profile
                saved = saved.copy(
                    lastName = profile.lastName,
                    firstName = profile.firstName,
                    mobilePhone = profile.mobilePhone,
                    landlinePhone = profile.landlinePhone,
                    emails = profile.emails,
                )
            }
            if (form.details != state.saved.details) {
                val details = saveOwnPrivateDetails(current, form.details).getOrElse { error ->
                    // Das Mitgliedsdokument ist dann schon gespeichert; nur die privaten Angaben fehlen noch.
                    mutableUiState.update { it.copy(saved = saved) }
                    return@launch fail(error)
                }
                saved = saved.copy(details = details)
            }
            mutableUiState.update {
                it.copy(form = saved, saved = saved, isSaving = false, message = "Gespeichert.", isError = false)
            }
        }
    }

    private fun observeDetails(user: AppUser) {
        detailsJob?.cancel()
        mutableUiState.update { it.copy(detailsLoaded = false) }
        detailsJob = viewModelScope.launch {
            observeOwnPrivateDetails(user)
                .catch { error -> fail(error, "Adresse und Notfallkontakte konnten nicht geladen werden.") }
                .collect { details ->
                    mutableUiState.update { state ->
                        val untouched = state.form.details == state.saved.details
                        state.copy(
                            form = if (untouched) state.form.copy(details = details) else state.form,
                            saved = state.saved.copy(details = details),
                            detailsLoaded = true,
                        )
                    }
                }
        }
    }

    private fun updateForm(change: (OwnProfileForm) -> OwnProfileForm) =
        mutableUiState.update { it.copy(form = change(it.form), message = null) }

    private fun updateDetails(change: (MemberPrivateDetails) -> MemberPrivateDetails) =
        updateForm { it.copy(details = change(it.details)) }

    private fun fail(error: Throwable, fallback: String = "Speichern fehlgeschlagen.") {
        mutableUiState.update { it.copy(isSaving = false, message = error.message ?: fallback, isError = true) }
    }
}
