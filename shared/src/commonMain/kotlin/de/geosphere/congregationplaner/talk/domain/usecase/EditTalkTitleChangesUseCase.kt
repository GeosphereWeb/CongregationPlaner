package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/** Eine Änderung an der Titelhistorie eines Vortrags; [index] ist die Stelle in der Liste. */
sealed interface TalkTitleChangeEdit {
    /** Neue Änderung ab [date], vorbelegt mit dem Titel, der an diesem Tag bisher gilt. */
    data class Add(val date: LocalDate) : TalkTitleChangeEdit

    data class Change(val index: Int, val change: TalkTitleChange) : TalkTitleChangeEdit

    data class Remove(val index: Int) : TalkTitleChangeEdit
}

/** Wendet eine [TalkTitleChangeEdit] auf [talk] an; mehr als [Talk.MAX_TITLE_CHANGES] Änderungen gibt es nicht. */
@Inject
class EditTalkTitleChangesUseCase {
    operator fun invoke(talk: Talk, edit: TalkTitleChangeEdit): List<TalkTitleChange> {
        val changes = talk.titleChanges
        return when (edit) {
            is TalkTitleChangeEdit.Add ->
                if (canAdd(changes)) changes + TalkTitleChange(edit.date, talk.titleOn(edit.date)) else changes
            is TalkTitleChangeEdit.Change ->
                changes.mapIndexed { i, current -> if (i == edit.index) edit.change else current }
            is TalkTitleChangeEdit.Remove -> changes.filterIndexed { i, _ -> i != edit.index }
        }
    }

    fun canAdd(changes: List<TalkTitleChange>): Boolean = changes.size < Talk.MAX_TITLE_CHANGES
}
