package de.geosphere.congregationplaner.notice.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual fun createNoticeFileSource(): NoticeFileSource = SwingNoticeFileSource

/** File dialogs of the desktop app. */
internal object SwingNoticeFileSource : NoticeFileSource {
    override val isAvailable: Boolean = true

    override suspend fun pickPng(): Result<PickedPng?> = withContext(Dispatchers.IO) {
        runCatching {
            chooseFile("Grafik wählen", save = false, filter = FileNameExtensionFilter("PNG-Grafik (*.png)", "png"))
                ?.let { PickedPng(pickedPngName(it.name), it.readBytes()) }
        }
    }

    override suspend fun savePdf(fileName: String, bytes: ByteArray): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val filter = FileNameExtensionFilter("PDF-Dokument (*.pdf)", "pdf")
            val file = chooseFile("Speichern unter", save = true, filter = filter, proposed = fileName)
                ?: return@runCatching false
            val target = if (file.extension.equals("pdf", ignoreCase = true)) file else File("${file.path}.pdf")
            target.writeBytes(bytes)
            true
        }
    }

    private fun chooseFile(
        title: String,
        save: Boolean,
        filter: FileNameExtensionFilter,
        proposed: String? = null,
    ): File? {
        var chosen: File? = null
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileFilter = filter
                proposed?.let { selectedFile = File(it) }
            }
            val result = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) chosen = chooser.selectedFile
        }
        return chosen
    }
}
