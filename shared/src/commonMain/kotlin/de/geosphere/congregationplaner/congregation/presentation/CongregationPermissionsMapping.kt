package de.geosphere.congregationplaner.congregation.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser

/** Die UI-Rechte von [user] für die Liste und den gewählten [editor]; ohne Nutzer ist alles verboten. */
internal fun congregationPermissions(
    policy: CongregationPermissionPolicy,
    user: AppUser?,
    editor: CongregationMetadata?,
): CongregationPermissions {
    if (user == null) return CongregationPermissions()
    // Eine neue Versammlung legt der globale Admin samt Zeiten an; danach ändert nur der Versammlungsadmin die Zeiten.
    val canEditSelection = editor?.let { if (it.isNew) policy.canCreate(user) else policy.canEdit(user, it) }
    val canEditTimes = editor?.let { if (it.isNew) policy.canCreate(user) else policy.canEditMeetingTimes(user, it) }
    return CongregationPermissions(
        canCreate = policy.canCreate(user),
        canManage = policy.canManageGeneral(user),
        canEditSelection = canEditSelection ?: false,
        canEditMeetingTimes = canEditTimes ?: false,
    )
}

/** Mit [ownOnly] nur die eigene Versammlung des Nutzers (Bereich „Versammlung“), sonst alle des Kreises. */
internal fun visibleCongregations(
    all: List<CongregationMetadata>,
    ownOnly: Boolean,
    user: AppUser?,
): List<CongregationMetadata> {
    if (!ownOnly) return all
    val ownId = user?.congregation?.congregationId
    return all.filter { it.id == ownId }
}
