package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser

internal const val MAX_NAME = VisitingSpeaker.MAX_NAME_LENGTH

internal fun AppUser.plannerCongregation(policy: TalkSchedulePermissionPolicy): CongregationRef? =
    congregation?.takeIf { policy.canPlanTalks(this) }
