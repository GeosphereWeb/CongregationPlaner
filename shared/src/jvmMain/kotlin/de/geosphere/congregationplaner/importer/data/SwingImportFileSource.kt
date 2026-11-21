package de.geosphere.congregationplaner.importer.data

import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual fun createImportFileSource(): ImportFileSource = SwingImportFileSource

/** File dialogs of the desktop app; reads and writes the `.xlsx` with [Xlsx]. */
internal object SwingImportFileSource : ImportFileSource {
    override val isAvailable: Boolean = true

    override suspend fun pickTable(title: String): Result<ImportTable?> = withContext(Dispatchers.IO) {
        runCatching { chooseFile(title, save = false)?.let { Xlsx.readFirstSheet(ZipArchive.unzip(it.readBytes())) } }
    }

    override suspend fun saveTable(fileName: String, table: ImportTable): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = chooseFile("Speichern unter", save = true, proposed = fileName) ?: return@runCatching false
                val target = if (file.extension.equals("xlsx", ignoreCase = true)) file else File("${file.path}.xlsx")
                target.writeBytes(ZipArchive.zip(Xlsx.write(table)))
                true
            }
        }

    private fun chooseFile(title: String, save: Boolean, proposed: String? = null): File? {
        var chosen: File? = null
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileFilter = FileNameExtensionFilter("Excel-Datei (*.xlsx)", "xlsx")
                proposed?.let { selectedFile = File(it) }
            }
            val result = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) chosen = chooser.selectedFile
        }
        return chosen
    }
}
