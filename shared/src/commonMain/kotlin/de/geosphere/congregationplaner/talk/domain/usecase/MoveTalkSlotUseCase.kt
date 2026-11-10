package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/**
 * Verschiebt einen Termin auf ein anderes Datum. Da das Datum der Schlüssel ist, wird der neue Termin zuerst
 * gespeichert und danach der alte gelöscht: Schlägt das Löschen fehl, bleibt ein Duplikat statt eines Verlusts.
 * Der Aufrufer muss sicherstellen, dass am Zieltag kein anderer Termin liegt, denn Speichern überschreibt.
 */
@Inject
class MoveTalkSlotUseCase(private val save: SaveTalkSlotUseCase, private val delete: DeleteTalkSlotUseCase) {
    suspend operator fun invoke(user: AppUser, from: LocalDate, slot: TalkSlot): Result<TalkSlot> =
        save(user, slot).mapCatching { saved ->
            if (from != saved.date) delete(user, from).getOrThrow()
            saved
        }
}
