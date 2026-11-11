package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker

/** Der Eintrag im Detail; `original` ist `null`, solange der Eintrag neu und noch nicht gespeichert ist. */
sealed interface ExternalDirectoryEditor {
    val id: String?
    val isDirty: Boolean

    data class Circuit(val circuit: ExternalCircuit, val original: ExternalCircuit? = null) :
        ExternalDirectoryEditor {
        override val id: String? get() = original?.id
        override val isDirty: Boolean get() = circuit != original
    }

    data class Congregation(val congregation: ExternalCongregation, val original: ExternalCongregation? = null) :
        ExternalDirectoryEditor {
        override val id: String? get() = original?.id
        override val isDirty: Boolean get() = congregation != original
    }

    /** [talksText] ist die Eingabe der Vortragsnummern; sie wird erst beim Speichern in Zahlen umgewandelt. */
    data class Speaker(val speaker: VisitingSpeaker, val talksText: String, val original: VisitingSpeaker? = null) :
        ExternalDirectoryEditor {
        override val id: String? get() = original?.id
        override val isDirty: Boolean
            get() = speaker != original || talksText != speaker.talkNumbers.toText()
    }
}

/** Was der FAB anlegen kann; [CIRCUIT] braucht keinen Elternteil, die anderen den Kreis bzw. die Versammlung. */
enum class AddTarget(val label: String) {
    CIRCUIT("Kreis hinzufügen"),
    CONGREGATION("Versammlung hinzufügen"),
    SPEAKER("Redner hinzufügen"),
}

data class AddAction(val target: AddTarget, val parentId: String = "")

/**
 * Die Anlege-Aktion des FAB: Ist ein gespeicherter Kreis bzw. eine gespeicherte Versammlung geöffnet, legt er nur
 * die Unterebene dort an (Versammlung bzw. Redner); sonst einen Kreis.
 */
val ExternalDirectoryUiState.addAction: AddAction
    get() = when (val current = editor) {
        is ExternalDirectoryEditor.Circuit ->
            current.original?.let { AddAction(AddTarget.CONGREGATION, it.id) }

        is ExternalDirectoryEditor.Congregation ->
            current.original?.let { AddAction(AddTarget.SPEAKER, it.id) }

        else -> null
    } ?: AddAction(AddTarget.CIRCUIT)

internal fun List<Int>.toText(): String = joinToString(", ")

/** Zahlen getrennt durch Komma, Semikolon oder Leerzeichen; `null`, wenn ein Eintrag keine Zahl ist. */
internal fun parseTalkNumbers(text: String): List<Int>? {
    val numbers = text.split(',', ';', ' ').filter(String::isNotBlank).map { it.toIntOrNull() ?: return null }
    return numbers.distinct().sorted()
}

data class ExternalDirectoryUiState(
    val directory: ExternalDirectory = ExternalDirectory(),
    val editor: ExternalDirectoryEditor? = null,
    val isLoading: Boolean = true,
    val canView: Boolean = false,
    val canPlan: Boolean = false,
    override val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val filter: DirectoryFilter = DirectoryFilter(),
    val congregationQuery: String = "",
    override val errorMessage: String? = null,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null

    /** Das Verzeichnis, wie es die Liste zeigt: auf den gewählten Kreis und die Versammlung eingegrenzt. */
    val visibleDirectory: ExternalDirectory get() = directory.filtered(filter)
}

internal fun <T> List<T>.upsert(item: T, id: (T) -> String): List<T> =
    if (any { id(it) == id(item) }) map { if (id(it) == id(item)) item else it } else this + item
