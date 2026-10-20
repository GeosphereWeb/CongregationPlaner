package de.geosphere.congregationplaner.user.domain.model

/**
 * Anwendungsweiter Zugriffsstatus des angemeldeten Nutzers.
 */
sealed interface UserAccessState {
    /** Der Status wird gerade ermittelt. */
    data object Loading : UserAccessState

    /** Es ist kein Nutzer angemeldet. */
    data object Unauthenticated : UserAccessState

    /** Der Nutzer ist angemeldet, hat aber noch keine Versammlung gewählt. */
    data object CongregationRequired : UserAccessState

    /** Der Nutzer ist angemeldet, aber vom Versammlungsadmin noch nicht freigegeben. */
    data class NeedsApproval(val user: AppUser) : UserAccessState

    /** Der Nutzer ist angemeldet und freigegeben. */
    data class Authorized(val user: AppUser) : UserAccessState

    /** Der Nutzerstatus konnte nicht ermittelt werden (z. B. Firestore nicht erreichbar). */
    data class Error(val message: String) : UserAccessState
}
