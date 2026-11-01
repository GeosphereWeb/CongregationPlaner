package de.geosphere.congregationplaner.user.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AdminContact

/** Zugriff auf die Namen der Admins einer Versammlung (`…/congregation/{id}/adminContacts/{uid}`). */
interface AdminContactRepository {
    /** Liest die Admin-Kontakte vom Server; das dürfen auch Nutzer, die noch nicht freigegeben sind. */
    suspend fun getAdminContacts(congregation: CongregationRef): Result<List<AdminContact>>

    suspend fun putAdminContact(congregation: CongregationRef, contact: AdminContact): Result<Unit>

    /** Löscht den Kontakt; ein nicht vorhandener Kontakt ist kein Fehler. */
    suspend fun deleteAdminContact(congregation: CongregationRef, uid: String): Result<Unit>
}
