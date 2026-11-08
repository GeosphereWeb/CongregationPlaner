package de.geosphere.congregationplaner.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual fun createImportFileSource(): ImportFileSource = SwingImportFileSource

/** File dialog of the desktop app; reads the chosen `.xlsx` with [XlsxReader]. */
internal object SwingImportFileSource : ImportFileSource {
    override val isAvailable: Boolean = true

    override suspend fun pickTable(title: String): Result<ImportTable?> = withContext(Dispatchers.IO) {
        runCatching { chooseFile(title)?.let { XlsxReader.readFirstSheet(it.readBytes()) } }
    }

    private fun chooseFile(title: String): File? {
        var chosen: File? = null
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileFilter = FileNameExtensionFilter("Excel-Datei (*.xlsx)", "xlsx")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chosen = chooser.selectedFile
        }
        return chosen
    }
}
