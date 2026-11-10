package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException

internal class Written<T>(val report: ImportReport, val saved: List<PlanEntry<T>>)

internal fun Throwable.isDenied(): Boolean = this is TalkReferenceException.NotAllowed ||
    this is ExternalDirectoryException.NotAllowed ||
    this is TalkScheduleException.NotAllowed

/**
 * Writes the entries of [plan] that are to be created or updated through the regular save use case, so permission,
 * validation and rules apply as in the app. A failing entry is reported and the run continues; a missing permission
 * stops the step at once, because every further entry would fail the same way.
 */
internal suspend fun <T> write(plan: ImportPlan<T>, label: (T) -> String, save: suspend (T) -> Result<*>): Written<T> {
    var report = ImportReport(skipped = plan.count(ImportKind.SKIP))
    val saved = mutableListOf<PlanEntry<T>>()
    for (entry in plan.entries.filter { it.kind != ImportKind.SKIP }) {
        val failure = save(entry.item).exceptionOrNull()
        when {
            failure == null -> {
                saved += entry
                report += if (entry.kind == ImportKind.CREATE) ImportReport(created = 1) else ImportReport(updated = 1)
            }
            failure.isDenied() -> return Written(report + failed(failure.message, label(entry.item)), saved)
            else -> report += failed(failure.message, label(entry.item))
        }
    }
    return Written(report, saved)
}

internal fun failed(reason: String?, subject: String) = ImportReport(failures = listOf("$subject: $reason"))
