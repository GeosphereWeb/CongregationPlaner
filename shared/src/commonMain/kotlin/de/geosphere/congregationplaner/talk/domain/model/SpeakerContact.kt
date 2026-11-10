package de.geosphere.congregationplaner.talk.domain.model

import de.geosphere.congregationplaner.user.domain.model.internationalNumber

/**
 * Wie sich ein Redner erreichen lässt: Handy und Festnetz lassen sich je einzeln anrufen, WhatsApp gibt es nur mit
 * einer Handynummer. Eine Nummer, die sich nicht wählen lässt (keine Vorwahl, zu kurz), zählt als nicht vorhanden.
 */
data class SpeakerContact(val mobilePhone: String = "", val landlinePhone: String = "") {
    /** `tel:`-Link der Handynummer; `null` ohne wählbare Handynummer. */
    val mobileCallUri: String? get() = callUri(mobilePhone)

    /** `tel:`-Link der Festnetznummer; `null` ohne wählbare Festnetznummer. */
    val landlineCallUri: String? get() = callUri(landlinePhone)

    /** `wa.me`-Link der Handynummer; `null` ohne wählbare Handynummer. */
    val whatsAppUri: String? get() = internationalNumber(mobilePhone)?.let { "https://wa.me/${it.removePrefix("+")}" }

    /** `true`, wenn sich der Redner anrufen oder per WhatsApp erreichen lässt. */
    val isReachable: Boolean get() = mobileCallUri != null || landlineCallUri != null

    private fun callUri(raw: String): String? = internationalNumber(raw)?.let { "tel:$it" }
}
