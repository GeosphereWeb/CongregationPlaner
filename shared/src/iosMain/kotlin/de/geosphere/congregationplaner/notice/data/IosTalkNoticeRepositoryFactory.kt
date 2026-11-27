package de.geosphere.congregationplaner.notice.data

import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository

actual fun createTalkNoticeRepository(): TalkNoticeRepository = unavailableTalkNoticeRepository("iOS")

actual fun createNoticeFileSource(): NoticeFileSource = UnavailableNoticeFileSource
