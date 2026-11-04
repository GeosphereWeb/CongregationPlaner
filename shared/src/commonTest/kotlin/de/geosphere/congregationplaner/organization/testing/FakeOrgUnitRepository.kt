package de.geosphere.congregationplaner.organization.testing

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-Memory-[OrgUnitRepository], abgelegt nach Collection-Pfad. Neue Einheiten bekommen die IDs `new-1`, `new-2`, …;
 * eine Einheit mit Einträgen unter ihrem Pfad lässt sich nicht löschen. Fehler lassen sich über [failure] einstellen.
 */
internal class FakeOrgUnitRepository(units: Map<String, List<OrgUnit>> = emptyMap()) : OrgUnitRepository {
    val units = MutableStateFlow(units)
    var failure: Throwable? = null
    private var nextId = 0

    override fun observe(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>> =
        units.map { it[level.collectionPath(parentIds)].orEmpty() }

    override suspend fun create(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> = write {
        unit.copy(id = "new-${++nextId}").also { created -> change(level, parentIds) { it + created } }
    }

    override suspend fun update(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> = write {
        change(level, parentIds) { list -> list.filterNot { it.id == unit.id } + unit }
        unit
    }

    override suspend fun delete(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<Unit> = write {
        val childPath = "${level.collectionPath(parentIds)}/${unit.id}/${checkNotNull(level.child).collection}"
        if (units.value[childPath].orEmpty().isNotEmpty()) throw OrgUnitException.HasChildren(unit.displayName)
        change(level, parentIds) { list -> list.filterNot { it.id == unit.id } }
    }

    private fun change(level: OrgLevel, parentIds: List<String>, transform: (List<OrgUnit>) -> List<OrgUnit>) {
        val path = level.collectionPath(parentIds)
        units.value = units.value + (path to transform(units.value[path].orEmpty()))
    }

    private fun <T> write(block: () -> T): Result<T> = failure?.let { Result.failure(it) } ?: runCatching(block)
}
