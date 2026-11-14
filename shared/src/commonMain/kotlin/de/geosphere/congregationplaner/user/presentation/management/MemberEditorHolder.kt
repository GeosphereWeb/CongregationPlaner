package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.presentation.directory.parseTalkNumbers
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.usecase.CreateMemberWithoutAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberProfileUseCase
import de.geosphere.congregationplaner.user.presentation.contact.withinInputLimits
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Die UseCases des Profil-Editors. */
@Inject
class MemberEditorUseCases(
    val createMemberWithoutAccount: CreateMemberWithoutAccountUseCase,
    val updateMemberProfile: UpdateMemberProfileUseCase,
)

/** Bearbeitet die persönlichen Angaben eines Mitglieds oder erfasst eine Person ohne Konto. */
class MemberEditorHolder(
    private val store: UiStateStore<UserManagementUiState>,
    private val scope: CoroutineScope,
    private val useCases: MemberEditorUseCases,
    private val currentUser: () -> AppUser?,
) {
    /** Öffnet einen leeren Editor, sofern der Nutzer Mitglieder verwalten darf. */
    fun add() = open(store.value.canManage, MemberEditorState())

    fun edit(member: AppUser) =
        open(store.value.permissionsOf(member).canEditProfile, MemberEditorState(member.profile, member))

    /** Hält die Angaben stimmig, z. B. verliert ein Kind die Taufe und ein Nicht-Bruder das Dienstamt. */
    fun update(profile: MemberProfile) = store.update { state ->
        state.copy(editor = state.editor?.copy(profile = profile.normalized().withinInputLimits()))
    }

    fun updateTalks(text: String) = store.update { state ->
        state.copy(editor = state.editor?.copy(talksText = text))
    }

    fun cancel() {
        if (!store.value.isSaving) store.update { it.copy(editor = null, errorMessage = null) }
    }

    /** Verwirft Änderungen, lässt aber das Mitglied offen; eine neue, ungespeicherte Person wird geschlossen. */
    fun discard() {
        if (store.value.isSaving) return
        store.update { state ->
            val reverted = state.editor?.takeUnless { it.isNew }?.let { MemberEditorState(it.original, it.member) }
            state.copy(editor = reverted, errorMessage = null)
        }
    }

    fun save() {
        val state = store.value
        val editor = state.editor
        val user = currentUser()
        val uid = editor?.member?.uid
        val busy = state.isSaving || uid in state.busyMemberIds
        if (editor == null || user == null || busy) return
        // Wer kein Redner ist, hat keine Vorträge; die Eingabe zählt dann nicht.
        val talks = if (editor.profile.isSpeaker) parseTalkNumbers(editor.talksText) else emptyList()
        if (talks == null) {
            store.update { it.copy(errorMessage = INVALID_TALKS_MESSAGE) }
            return
        }
        val profile = editor.profile.copy(talkNumbers = talks)

        store.update {
            it.copy(isSaving = true, busyMemberIds = it.busyMemberIds + setOfNotNull(uid), errorMessage = null)
        }
        scope.launch {
            val result = if (editor.member == null) {
                useCases.createMemberWithoutAccount(user, profile)
            } else {
                useCases.updateMemberProfile(user, editor.member.copy(profile = profile))
            }
            store.update { current ->
                result.fold(
                    onSuccess = { saved ->
                        val reopened = MemberEditorState(saved.profile, saved)
                        current.withSavedMember(saved).copy(editor = reopened, isSaving = false)
                    },
                    onFailure = { exception ->
                        current.copy(
                            isSaving = false,
                            busyMemberIds = current.busyMemberIds - setOfNotNull(uid),
                            errorMessage = exception.message ?: "Angaben konnten nicht gespeichert werden.",
                        )
                    },
                )
            }
        }
    }

    private companion object {
        const val INVALID_TALKS_MESSAGE = "Vorträge: bitte nur Nummern, getrennt durch Komma oder Leerzeichen."
    }

    private fun open(allowed: Boolean, editor: MemberEditorState) = store.update {
        if (allowed) {
            it.copy(editor = editor, errorMessage = null)
        } else {
            it.copy(errorMessage = MISSING_PERMISSION_MESSAGE)
        }
    }
}
