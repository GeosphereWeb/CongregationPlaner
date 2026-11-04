package de.geosphere.congregationplaner.user.domain.model

/** Ein Notfallkontakt mit bis zu [MemberProfile.MAX_PHONES_PER_CONTACT] Telefonnummern; [relation] ist optional. */
data class EmergencyContact(val name: String = "", val relation: String = "", val phones: List<String> = listOf("")) {
    /** Der Ort zur ersten Festnetznummer (Ortsnetzname aus dem Vorwahlverzeichnis), sonst `null`. */
    val areaName: String? get() = phones.firstNotNullOfOrNull { phone -> areaName(phone) }
}
