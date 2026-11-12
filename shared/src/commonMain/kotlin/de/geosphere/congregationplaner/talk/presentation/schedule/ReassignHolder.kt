package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.hasAssignment
import de.geosphere.congregationplaner.talk.domain.usecase.ReassignTalkUseCase
import kotlinx.coroutines.launch

/**
 * Plant den Vortrag des geöffneten Termins auf einen anderen Tag um: [start] schließt den Editor, damit die Liste zu
 * sehen ist; der nächste angetippte Termin ist der Zieltag. Ist er belegt, fragt [ReassignState.swapTarget] erst nach.
 */
class ReassignHolder(private val session: TalkScheduleSession, private val reassignTalk: ReassignTalkUseCase) {
    private val store get() = session.store

    fun start() {
        val state = store.value
        val source = state.editor?.original?.takeIf { state.canStartReassign } ?: return
        if (session.allowedUser() == null) return
        store.update { it.copy(reassign = ReassignState(source), editor = null, errorMessage = null) }
    }

    fun cancel() = store.update { it.copy(reassign = null) }

    /** Der angetippte Termin als Zieltag; derselbe Tag bricht ab. */
    fun pickTarget(target: TalkSlot) {
        val reassign = store.value.reassign ?: return
        when {
            store.value.isSaving -> Unit
            target.date == reassign.source.date -> cancel()
            target.hasAssignment -> store.update { it.copy(reassign = reassign.copy(swapTarget = target)) }
            else -> perform(reassign.source, target)
        }
    }

    fun confirmSwap() {
        val reassign = store.value.reassign ?: return
        reassign.swapTarget?.let { perform(reassign.source, it) }
    }

    fun dismissSwap() = store.update { it.copy(reassign = it.reassign?.copy(swapTarget = null)) }

    private fun perform(from: TalkSlot, to: TalkSlot) {
        val user = session.allowedUser() ?: return
        store.update { it.copy(isSaving = true, errorMessage = null) }
        session.scope.launch {
            reassignTalk(user, from, to)
                .onSuccess { (newFrom, newTo) ->
                    store.update {
                        it.copy(isSaving = false, reassign = null, infoMessage = reassignedMessage(newFrom, newTo))
                    }
                }.onFailure {
                    session.fail(it, "Der Vortrag konnte nicht umgeplant werden.")
                    dismissSwap()
                }
        }
    }
}
