package de.geosphere.congregationplaner.importer.data

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import de.geosphere.congregationplaner.auth.data.FirebaseAndroidContextHolder
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual fun createImportFileSource(): ImportFileSource = AndroidImportFileSource

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

// Some file managers report .xlsx files only as generic binary data.
private val OPEN_TYPES = arrayOf(XLSX_MIME, "application/octet-stream")

/** The system file dialog (Storage Access Framework); reads and writes the `.xlsx` with [Xlsx]. */
internal object AndroidImportFileSource : ImportFileSource {
    private var requestCount = 0

    override val isAvailable: Boolean get() = FirebaseAndroidContextHolder.activity is ComponentActivity

    override suspend fun pickTable(title: String): Result<ImportTable?> = runCatching {
        val activity = activity()
        val uri = activity.awaitResult(ActivityResultContracts.OpenDocument(), OPEN_TYPES) ?: return@runCatching null
        withContext(Dispatchers.IO) {
            val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Die Datei konnte nicht geöffnet werden.")
            Xlsx.readFirstSheet(ZipArchive.unzip(bytes))
        }
    }

    override suspend fun saveTable(fileName: String, table: ImportTable, dateHeaders: Set<String>): Result<Boolean> =
        runCatching { save(fileName, ZipArchive.zip(Xlsx.write(table, dateHeaders))) }

    private suspend fun save(fileName: String, bytes: ByteArray): Boolean {
        val activity = activity()
        val uri = activity.awaitResult(ActivityResultContracts.CreateDocument(XLSX_MIME), fileName) ?: return false
        withContext(Dispatchers.IO) {
            activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Die Datei konnte nicht gespeichert werden.")
        }
        return true
    }

    private fun activity(): ComponentActivity = FirebaseAndroidContextHolder.activity as? ComponentActivity
        ?: error("Der Dateidialog braucht eine geöffnete App.")

    /**
     * Shows the system dialog of [contract] and waits for its result.
     * shortcut: the callback is lost if Android recreates the activity while the dialog is open (e.g. rotation);
     * the user then picks again. Upgrade to a Compose launcher if that becomes a problem.
     */
    private suspend fun <I, O> ComponentActivity.awaitResult(contract: ActivityResultContract<I, O>, input: I): O =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                var launcher: ActivityResultLauncher<I>? = null
                launcher = activityResultRegistry.register("cp-file-${requestCount++}", contract) { result ->
                    launcher?.unregister()
                    continuation.resume(result)
                }
                continuation.invokeOnCancellation { launcher.unregister() }
                launcher.launch(input)
            }
        }
}
