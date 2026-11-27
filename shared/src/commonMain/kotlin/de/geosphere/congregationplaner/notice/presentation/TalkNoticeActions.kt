package de.geosphere.congregationplaner.notice.presentation

import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import kotlinx.datetime.LocalDate

/** Callbacks of the talk notice section; the defaults do nothing (previews). */
@Suppress("LongParameterList") // one callback per action of the section
data class TalkNoticeActions(
    val onChangeStart: (LocalDate) -> Unit = {},
    val onDecreaseCount: () -> Unit = {},
    val onIncreaseCount: () -> Unit = {},
    val onSelectImage: (String?) -> Unit = {},
    val onCreatePdf: () -> Unit = {},
    val onDismissSaved: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onEditTitle: (String) -> Unit = {},
    val onSaveTitle: () -> Unit = {},
    val onChooseColorSet: (NoticeColorSet) -> Unit = {},
    val onMakeDefault: (String?) -> Unit = {},
    val onAddImage: () -> Unit = {},
    val onRenameImage: (String, String) -> Unit = { _, _ -> },
    val isValidImageName: (String) -> Boolean = { true },
    val onRequestDeleteImage: (NoticeImage?) -> Unit = {},
    val onConfirmDeleteImage: () -> Unit = {},
)
