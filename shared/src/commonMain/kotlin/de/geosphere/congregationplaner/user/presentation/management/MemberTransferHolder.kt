package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.usecase.AdoptVisitingSpeakersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.LinkMemberAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.MemberTransferPolicy
import de.geosphere.congregationplaner.user.domain.usecase.SpeakerAdoption
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Ein Redner aus dem Verzeichnis zur Auswahl: „Berg, Lea“ und seine Versammlung. */
data class SpeakerChoice(val id: String, val name: String, val congregation: String)

/** Auswahl der Redner, die Mitglieder werden; [speakers] ist leer, solange das Verzeichnis lädt. */
data class AdoptSpeakersDialogState(
    val speakers: List<SpeakerChoice> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
) {
    val canConfirm: Boolean get() = selected.isNotEmpty() && !isWorking
}

/** Verknüpfen von [account] mit einer Person ohne Konto; [selectedPersonId] ist die gewählte Person. */
data class LinkAccountDialogState(
    val account: AppUser,
    val selectedPersonId: String? = null,
    val isWorking: Boolean = false,
) {
    val canConfirm: Boolean get() = selectedPersonId != null && !isWorking
}

/** Die UseCases für Übernehmen und Verknüpfen. */
@Inject
class MemberTransferUseCases(
    val observeDirectory: ObserveExternalDirectoryUseCase,
    val adoptSpeakers: AdoptVisitingSpeakersUseCase,
    val linkAccount: LinkMemberAccountUseCase,
    val policy: MemberTransferPolicy,
)

/** Redner aus dem Verzeichnis als Mitglieder übernehmen und Konten mit Personen ohne Konto verknüpfen. */
class MemberTransferHolder(
    private val store: UiStateStore<UserManagementUiState>,
    private val scope: CoroutineScope,
    private val useCases: MemberTransferUseCases,
    private val currentUser: () -> AppUser?,
) {
    private var speakers: List<VisitingSpeaker> = emptyList()

    fun canTransfer(user: AppUser): Boolean = useCases.policy.canTransfer(user)

    fun openAdopt() {
        val user = currentUser()?.takeIf(::canTransfer) ?: return
        store.update { it.copy(adoptDialog = AdoptSpeakersDialogState(), infoMessage = null) }
        scope.launch {
            runCatching { useCases.observeDirectory(user).first() }.fold(
                onSuccess = { directory ->
                    speakers = directory.speakers
                    store.update { state ->
                        state.copy(
                            adoptDialog = state.adoptDialog?.copy(
                                speakers = speakerChoices(directory),
                                isLoading = false,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    store.update {
                        it.copy(
                            adoptDialog = null,
                            errorMessage =
                            error.message ?: "Redner konnten nicht geladen werden.",
                        )
                    }
                },
            )
        }
    }

    fun toggleSpeaker(id: String) = store.update { state ->
        val dialog = state.adoptDialog ?: return@update state
        val selected = if (id in dialog.selected) dialog.selected - id else dialog.selected + id
        state.copy(adoptDialog = dialog.copy(selected = selected))
    }

    fun closeAdopt() = store.update { it.copy(adoptDialog = it.adoptDialog?.takeIf { dialog -> dialog.isWorking }) }

    fun adopt() {
        val user = currentUser() ?: return
        val dialog = store.value.adoptDialog?.takeIf { it.canConfirm } ?: return
        val chosen = speakers.filter { it.id in dialog.selected }
        store.update { it.copy(adoptDialog = dialog.copy(isWorking = true)) }
        scope.launch {
            val result = useCases.adoptSpeakers(user, chosen)
            store.update { state ->
                result.fold(
                    onSuccess = { adoption ->
                        state.copy(
                            adoptDialog = null,
                            infoMessage = adoptionMessage(adoption),
                            errorMessage = adoption.failures.joinToString("\n").ifEmpty { null },
                        )
                    },
                    onFailure = { error ->
                        state.copy(adoptDialog = null, errorMessage = error.message ?: "Übernehmen fehlgeschlagen.")
                    },
                )
            }
        }
    }

    fun openLink(account: AppUser) {
        if (currentUser()?.let(::canTransfer) != true || !account.hasAccount) return
        store.update { it.copy(linkDialog = LinkAccountDialogState(account), infoMessage = null) }
    }

    fun selectPerson(id: String) = store.update { state ->
        state.copy(linkDialog = state.linkDialog?.copy(selectedPersonId = id))
    }

    fun closeLink() = store.update { it.copy(linkDialog = it.linkDialog?.takeIf { dialog -> dialog.isWorking }) }

    fun link() {
        val user = currentUser() ?: return
        val dialog = store.value.linkDialog?.takeIf { it.canConfirm } ?: return
        val person = store.value.members.firstOrNull { it.uid == dialog.selectedPersonId } ?: return
        store.update { it.copy(linkDialog = dialog.copy(isWorking = true)) }
        scope.launch {
            val result = useCases.linkAccount(user, dialog.account, person)
            store.update { state ->
                result.fold(
                    onSuccess = { linked -> state.afterLink(linked, person) },
                    onFailure = { error ->
                        state.copy(linkDialog = null, errorMessage = error.message ?: "Verknüpfen fehlgeschlagen.")
                    },
                )
            }
        }
    }
}

/** Nach dem Verknüpfen: das Konto mit den neuen Angaben, ohne die Person ohne Konto; ein offener Editor zeigt sie. */
internal fun UserManagementUiState.afterLink(linked: AppUser, person: AppUser): UserManagementUiState {
    val saved = withSavedMember(linked)
    return saved.copy(
        members = saved.members.filterNot { it.uid == person.uid },
        editor = editor?.let { if (it.member?.uid == linked.uid) MemberEditorState(linked.profile, linked) else it },
        linkDialog = null,
        infoMessage = "${linked.label} ist jetzt mit dem Konto verknüpft.",
    )
}

/** Die Redner des Verzeichnisses mit dem Namen ihrer Versammlung, nach Namen sortiert. */
internal fun speakerChoices(directory: ExternalDirectory): List<SpeakerChoice> {
    val congregations = directory.congregations.associate { it.id to it.name }
    return directory.speakers.map { speaker ->
        SpeakerChoice(
            id = speaker.id,
            name = listOf(speaker.lastName, speaker.firstName).filter(String::isNotBlank).joinToString(", "),
            congregation = congregations[speaker.congregationId].orEmpty(),
        )
    }.sortedBy { it.name.lowercase() }
}

/** Personen ohne Konto, mit denen sich ein Konto verknüpfen lässt, nach Namen sortiert. */
internal fun linkCandidates(members: List<AppUser>): List<AppUser> =
    members.filter { !it.hasAccount }.sortedBy { it.label.lowercase() }

internal fun adoptionMessage(adoption: SpeakerAdoption): String? = when (adoption.adopted.size) {
    0 -> null
    1 -> "1 Redner ist jetzt Mitglied der Versammlung."
    else -> "${adoption.adopted.size} Redner sind jetzt Mitglieder der Versammlung."
}
