package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.adminContact
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository

/**
 * Hält die öffentlich lesbaren Admin-Kontakte mit den Mitgliedern in Einklang. Firestore kennt keine
 * Lesesicht auf einzelne Felder; deshalb steht der Name jedes freigegebenen Admins zusätzlich in
 * `adminContacts`, damit Nutzer ohne Freigabe wissen, an wen sie sich wenden müssen.
 *
 * Die Reihenfolge verhindert, dass ein Fehler einen ehemaligen Admin weiter auflistet: Wer den Admin-Status
 * verliert, verliert zuerst den Kontakt; wer ihn bekommt, wird zuerst gespeichert und dann veröffentlicht.
 */
internal class AdminContactSyncingUserRepository(
    private val users: UserRepository,
    private val contacts: AdminContactRepository,
) : UserRepository by users {
    override suspend fun getOrCreateUser(authUser: FirebaseUser, congregation: CongregationRef): Result<AppUser> =
        users.getOrCreateUser(authUser, congregation).onSuccess { user ->
            // Best effort: füllt oder berichtigt den Kontakt bestehender Admins bei der Anmeldung.
            user.adminContact()?.let { contacts.putAdminContact(congregation, it) }
        }

    override suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser> {
        val contact = member.adminContact()
        if (contact == null) {
            contacts.deleteAdminContact(congregation, member.uid).getOrElse { return Result.failure(it) }
        }
        val updated = users.updateMember(congregation, member).getOrElse { return Result.failure(it) }
        if (contact != null) contacts.putAdminContact(congregation, contact).getOrElse { return Result.failure(it) }
        return Result.success(updated)
    }

    override suspend fun updateMemberProfile(congregation: CongregationRef, member: AppUser): Result<AppUser> {
        val updated = users.updateMemberProfile(congregation, member).getOrElse { return Result.failure(it) }
        val contact = member.adminContact()
        if (contact != null) contacts.putAdminContact(congregation, contact).getOrElse { return Result.failure(it) }
        return Result.success(updated)
    }

    override suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit> {
        contacts.deleteAdminContact(congregation, uid).getOrElse { return Result.failure(it) }
        return users.removeMember(congregation, uid)
    }
}

internal fun UserRepository.withAdminContacts(contacts: AdminContactRepository): UserRepository =
    AdminContactSyncingUserRepository(this, contacts)
