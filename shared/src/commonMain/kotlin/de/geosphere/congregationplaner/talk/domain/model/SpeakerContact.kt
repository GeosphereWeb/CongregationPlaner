package de.geosphere.congregationplaner.talk.domain.model

import de.geosphere.congregationplaner.user.domain.model.internationalNumber

/**
 * Wie sich ein Redner erreichen lässt. Angerufen wird die Handynummer, sonst die Festnetznummer; WhatsApp gibt es nur
 * mit einer Handynummer.
 */
data class SpeakerContact(val mobilePhone: String = "", val landlinePhone: String = "") {
    /** `tel:`-Link der Handy-, sonst der Festnetznummer; `null`, wenn keine Nummer wählbar ist. */
    val callUri: String? get() = listOf(mobilePhone, landlinePhone).firstNotNullOfOrNull(::internationalNumber)
        ?.let { "tel:$it" }

    /** `wa.me`-Link der Handynummer; `null` ohne wählbare Handynummer. */
    val whatsAppUri: String? get() = internationalNumber(mobilePhone)?.let { "https://wa.me/${it.removePrefix("+")}" }
}
