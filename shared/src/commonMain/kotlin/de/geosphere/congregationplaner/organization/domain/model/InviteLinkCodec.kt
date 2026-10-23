package de.geosphere.congregationplaner.organization.domain.model

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Verschlüsselt den Versammlungspfad eines Einladungslinks mit AES-GCM, damit er nicht im Klartext lesbar ist.
 *
 * Der Schlüssel liegt in der App, denn die Links werden ohne Server geöffnet. Das verbirgt den Pfad vor
 * Mitlesenden, ersetzt aber keinen Zugriffsschutz: Wer die App zerlegt, kann Links entschlüsseln.
 */
@OptIn(ExperimentalEncodingApi::class)
internal object InviteLinkCodec {
    private const val KEY_HEX = "f4a35509d84777bea4d7ed7ea8bb40f89ac2d626a49efbda31ce605d9a6f6a9f"
    private val associatedData = "congregationplaner-invite-v1".encodeToByteArray()
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    private val cipher by lazy {
        CryptographyProvider.Default
            .get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArrayBlocking(AES.Key.Format.RAW, KEY_HEX.hexToByteArray())
            .cipher(tagSize = 128.bits)
    }

    /** Liefert den URL-sicheren Text für [path]; der Nonce ist zufällig, daher ergibt jeder Aufruf einen anderen Text. */
    fun encrypt(path: String): String = base64.encode(cipher.encryptBlocking(path.encodeToByteArray(), associatedData))

    /** Liefert den Pfad zu [token] oder `null`, wenn der Text nicht von der App erzeugt wurde. */
    fun decrypt(token: String): String? = runCatching {
        cipher.decryptBlocking(base64.decode(token), associatedData).decodeToString()
    }.getOrNull()
}
