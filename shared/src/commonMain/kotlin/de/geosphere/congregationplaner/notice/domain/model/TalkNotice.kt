package de.geosphere.congregationplaner.notice.domain.model

const val MAX_NOTICE_ROWS = 11
const val DEFAULT_NOTICE_ROWS = 10

/** One line of the notice; empty strings mean "show nothing". */
data class NoticeRow(
    val dateText: String,
    val number: String,
    val title: String,
    val speakerLine: String,
    val chairman: String,
    val event: String,
)

data class TalkNotice(
    val title: String,
    val congregationName: String,
    val rows: List<NoticeRow>,
    val emptyHint: String?,
    val footer: String,
)
