package de.geosphere.congregationplaner.user.domain.model

/** Werte des Firestore-Felds `relationToHead`: die Beziehung zum Familienoberhaupt (ADR-0032). */
enum class FamilyRelation(val label: String) {
    SPOUSE("Ehepartner"),
    CHILD("Kind"),
    OTHER("Sonstige"),
    ;

    companion object {
        fun fromValue(value: String?): FamilyRelation? = entries.firstOrNull { it.name == value }
    }
}

/**
 * Die Mitglieder, die [uid] als Familienoberhaupt wählen kann, nach Namen sortiert: alle anderen ohne eigenes
 * Oberhaupt. Wer selbst Oberhaupt ist, kann keinem zugeordnet werden (keine Ketten); [uid] ist `null` für eine neue
 * Person.
 */
fun familyHeadCandidates(members: List<AppUser>, uid: String?): List<AppUser> =
    if (uid != null && familyOf(members, uid).isNotEmpty()) {
        emptyList()
    } else {
        members.filter { it.uid != uid && it.profile.familyHeadId == null }.sortedBy { it.label.lowercase() }
    }

/** Die Mitglieder, die [headUid] als Familienoberhaupt haben, nach Namen sortiert. */
fun familyOf(members: List<AppUser>, headUid: String): List<AppUser> =
    members.filter { it.profile.familyHeadId == headUid }.sortedBy { it.label.lowercase() }
