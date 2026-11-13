package de.geosphere.congregationplaner.talk.domain.model

/** Der Name des angemeldeten Nutzers für `{MeinVorname}` und `{MeinNachname}`. */
data class MessageSender(val firstName: String = "", val lastName: String = "")

/** Eine ausgefüllte Vorlage hinter dem WhatsApp-Wischen: [name] der Vorlage, [uri] der fertige `wa.me`-Link. */
data class WhatsAppMessage(val name: String, val uri: String)
