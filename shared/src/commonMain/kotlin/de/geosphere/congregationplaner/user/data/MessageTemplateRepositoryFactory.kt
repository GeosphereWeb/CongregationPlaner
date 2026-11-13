package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository

expect fun createMessageTemplateRepository(): MessageTemplateRepository

/** Das Einstellungs-Dokument mit den Vorlagen; die Rules erlauben unter `settings` nur dieses. */
internal const val MESSAGE_TEMPLATES_DOCUMENT_ID = "messageTemplates"

internal fun CongregationRef.memberSettingsPath(uid: String): String = "$membersPath/$uid/settings"
