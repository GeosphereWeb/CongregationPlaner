package de.geosphere.congregationplaner.notice.domain.model

const val DEFAULT_NOTICE_TITLE = "Zusammenkünfte für die Öffentlichkeit"
const val MAX_TITLE_LENGTH = 120
const val MAX_IMAGE_NAME_LENGTH = 60
const val MAX_IMAGE_BYTES = 700_000
const val MAX_IMAGES = 10

/** Notice settings of a congregation; without a stored document these defaults apply. */
data class TalkNoticeSettings(
    val title: String = DEFAULT_NOTICE_TITLE,
    val colorSet: NoticeColorSet = NoticeColorSet.BLUE,
    val defaultImageId: String? = null,
)

/** A PNG graphic of a congregation that can be placed on the notice. */
class NoticeImage(val id: String, val name: String, val png: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is NoticeImage && id == other.id && name == other.name && png.contentEquals(other.png)

    override fun hashCode(): Int = 31 * (31 * id.hashCode() + name.hashCode()) + png.contentHashCode()
}
