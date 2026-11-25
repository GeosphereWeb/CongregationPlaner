package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlin.random.Random

/** Wie der Planer den Redner zu erreichen versucht hat. */
enum class ContactKind(val label: String) {
    CALL("Anruf"),
    VOICEMAIL("Anrufbeantworter"),
    MESSAGE("Nachricht"),
    EMAIL("E-Mail"),
    IN_PERSON("Persönlich"),
}

/** Ein Kontaktversuch bei einer Anfrage, z. B. „Anrufbeantworter am 12.11.“. */
data class ContactAttempt(val date: LocalDate, val kind: ContactKind, val note: String? = null)

/**
 * Eine offene Anfrage bei einem Redner aus `…/congregation/{id}/talkRequests/{id}` (ADR-0039). Nur Planer und Helfer
 * lesen sie, deshalb stehen Notiz und Kontaktversuche hier und nicht am Termin.
 *
 * Verweist ein Termin über [TalkSlot.requestId] auf die Anfrage, ist sie „mit Tag“; Redner und Vortrag stehen dann am
 * Termin. Ohne verweisenden Termin ist sie „ohne Tag“ und trägt Redner und Vortrag selbst (gleiche Form wie am Termin).
 */
data class SpeakerRequest(
    val id: String,
    val createdOn: LocalDate,
    val note: String? = null,
    val contactAttempts: List<ContactAttempt> = emptyList(),
    val speakerMemberId: String? = null,
    val speakerLastName: String? = null,
    val speakerFirstName: String? = null,
    val externalSpeakerId: String? = null,
    val speakerCongregation: String? = null,
    val talkNumber: Int? = null,
    val talkTitle: String? = null,
) {
    /** Der letzte Kontaktversuch, ohne Versuche der Tag der Anfrage. */
    val lastContact: LocalDate get() = contactAttempts.maxOfOrNull { it.date } ?: createdOn

    val hasSpeaker: Boolean get() = speakerMemberId != null || externalSpeakerId != null || speakerLastName != null

    /** Die Felder als Termin, damit Anzeige und Regeln dieselben bleiben wie dort. */
    internal val asSlotFields: TalkSlot
        get() = TalkSlot(
            date = createdOn,
            time = LocalTime(0, 0),
            talkNumber = talkNumber,
            talkTitle = talkTitle,
            speakerMemberId = speakerMemberId,
            speakerLastName = speakerLastName,
            speakerFirstName = speakerFirstName,
            externalSpeakerId = externalSpeakerId,
            speakerCongregation = speakerCongregation,
        )

    val speakerDisplayName: String? get() = asSlotFields.speakerDisplayName

    companion object {
        const val MAX_NOTE_LENGTH = 500
        const val MAX_ATTEMPT_NOTE_LENGTH = 200
        const val MAX_ATTEMPTS = 20
        const val FOLLOW_UP_DAYS = 7
    }
}

/** Eine Anfrage für die Übersicht: mit dem Termin, der auf sie verweist (`null` = Tag offen). */
data class OpenRequest(
    val request: SpeakerRequest,
    val slot: TalkSlot?,
    val needsFollowUp: Boolean,
    val datePassed: Boolean,
) {
    val speakerDisplayName: String? get() = slot?.speakerDisplayName ?: request.speakerDisplayName

    val talkLabel: String? get() = (slot ?: request.asSlotFields).talkLabel()
}

/**
 * Die offenen Anfragen aus [requests] mit ihrem Termin aus [slots], der älteste letzte Kontakt zuerst, bei Gleichstand
 * nach Tag (Tag offen zuletzt). Eine Anfrage ohne Termin und ohne Redner (verwaist) fällt weg.
 */
fun openRequestsOf(requests: List<SpeakerRequest>, slots: List<TalkSlot>, today: LocalDate): List<OpenRequest> {
    val slotByRequest = slots.filter { it.requestId != null }.associateBy { it.requestId }
    val followUpBefore = today.minus(DatePeriod(days = SpeakerRequest.FOLLOW_UP_DAYS))
    return requests.mapNotNull { request ->
        val slot = slotByRequest[request.id]
        if (slot == null && !request.hasSpeaker) {
            null
        } else {
            OpenRequest(
                request = request,
                slot = slot,
                needsFollowUp = request.lastContact < followUpBefore,
                datePassed = slot != null && slot.date < today,
            )
        }
    }.sortedWith(compareBy<OpenRequest> { it.request.lastContact }.thenBy(nullsLast()) { it.slot?.date })
}

/**
 * Die IDs der verwaisten Anfragen in [requests] (kein Termin aus [slots] verweist darauf, kein Redner), die gelöscht
 * werden dürfen. Vor [today] angelegt, denn eine neue Anfrage und ihr Termin kommen über zwei Abfragen an.
 */
fun orphanRequestIds(requests: List<SpeakerRequest>, slots: List<TalkSlot>, today: LocalDate): List<String> {
    val referenced = slots.mapNotNullTo(HashSet()) { it.requestId }
    return requests.filter { it.createdOn < today && it.id !in referenced && !it.hasSpeaker }.map { it.id }
}

/** Die Tage, auf die sich eine Anfrage ohne Tag legen lässt: ab [today], frei und ohne abgesagten Vortrag. */
fun assignableSlots(slots: List<TalkSlot>, today: LocalDate): List<TalkSlot> =
    slots.filter { it.date >= today && !it.talkCancelled && !it.hasAssignment }.sortedBy { it.date }

private const val REQUEST_ID_LENGTH = 20
private const val ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

/** Eine neue Dokument-ID für `talkRequests`, wie Firestore sie selbst vergibt (20 Zeichen). */
fun newRequestId(): String = buildString {
    repeat(REQUEST_ID_LENGTH) { append(ID_ALPHABET[Random.nextInt(ID_ALPHABET.length)]) }
}
