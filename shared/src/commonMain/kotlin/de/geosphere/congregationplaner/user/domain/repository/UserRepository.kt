package de.geosphere.congregationplaner.user.domain.repository

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlinx.coroutines.flow.Flow

/** Zugriff auf die Mitglieder einer Versammlung (`…/congregation/{id}/members/{uid}`). */
interface UserRepository {
    /**
     * Liest das Mitglied vom Server oder legt es in [congregation] an, falls es noch nicht existiert.
     * Neue Mitglieder werden immer mit `approved = false` angelegt.
     */
    suspend fun getOrCreateUser(authUser: FirebaseUser, congregation: CongregationRef): Result<AppUser>

    /** Beobachtet das Mitgliedsdokument; emittiert `null`, solange es nicht existiert. */
    fun observeUser(congregation: CongregationRef, uid: String): Flow<AppUser?>

    /** Prüft, ob für [uid] ein Dokument in `admins/{uid}` existiert. */
    suspend fun isGlobalAdmin(uid: String): Result<Boolean>

    /**
     * Beobachtet die Kontaktdaten eines globalen Admins in seinem Dokument `admins/{uid}`; ein leeres Profil, solange
     * er noch keine gepflegt hat. Globale Admins haben kein Mitgliedsdokument (ADR-0013).
     */
    fun observeGlobalAdminProfile(uid: String): Flow<MemberProfile>

    /** Speichert Nachnamen, Vornamen, Handy, Festnetz und E-Mail-Adressen von [profile] im Dokument `admins/{uid}`. */
    suspend fun updateGlobalAdminProfile(uid: String, profile: MemberProfile): Result<Unit>

    fun observeMembers(congregation: CongregationRef): Flow<List<AppUser>>

    /** Legt eine Person ohne eigenes Konto mit einer von Firestore vergebenen ID an. */
    suspend fun createMemberWithoutAccount(congregation: CongregationRef, profile: MemberProfile): Result<AppUser>

    /** Speichert Freigabe und Rollen; alle anderen Felder bleiben unverändert. */
    suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser>

    /** Speichert nur die persönlichen Angaben ([AppUser.profile]); alle anderen Felder bleiben unverändert. */
    suspend fun updateMemberProfile(congregation: CongregationRef, member: AppUser): Result<AppUser>

    /**
     * Speichert nur Nachnamen, Vornamen, Handy und Festnetz von [member] ([AppUser.profile]); das darf jedes Mitglied
     * im eigenen Dokument. Alle anderen Felder bleiben unverändert.
     */
    suspend fun updateOwnProfile(congregation: CongregationRef, member: AppUser): Result<AppUser>

    suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit>
}
