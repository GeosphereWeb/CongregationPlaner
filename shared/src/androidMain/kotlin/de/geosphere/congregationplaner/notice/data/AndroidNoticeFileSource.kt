package de.geosphere.congregationplaner.notice.data

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import de.geosphere.congregationplaner.auth.data.FirebaseAndroidContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual fun createNoticeFileSource(): NoticeFileSource = AndroidNoticeFileSource

private const val PNG_MIME = "image/png"
private const val PDF_MIME = "application/pdf"

/** The system file dialog (Storage Access Framework). */
internal object AndroidNoticeFileSource : NoticeFileSource {
    private var requestCount = 0

    override val isAvailable: Boolean get() = FirebaseAndroidContextHolder.activity is ComponentActivity

    override suspend fun pickPng(): Result<PickedPng?> = runCatching {
        val activity = activity()
        val uri = activity.awaitResult(ActivityResultContracts.OpenDocument(), arrayOf(PNG_MIME))
            ?: return@runCatching null
        withContext(Dispatchers.IO) {
            val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Die Datei konnte nicht geöffnet werden.")
            PickedPng(pickedPngName(displayName(activity, uri)), bytes)
        }
    }

    override suspend fun savePdf(fileName: String, bytes: ByteArray): Result<String?> = runCatching {
        val activity = activity()
        val uri = activity.awaitResult(ActivityResultContracts.CreateDocument(PDF_MIME), fileName)
            ?: return@runCatching null
        withContext(Dispatchers.IO) {
            activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Die Datei konnte nicht gespeichert werden.")
            // The system may have renamed the file (e.g. "Aushang (1).pdf"); show what was really created.
            displayName(activity, uri).ifBlank { fileName }
        }
    }

    private fun displayName(activity: ComponentActivity, uri: Uri): String =
        activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()

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
                launcher = activityResultRegistry.register("cp-notice-file-${requestCount++}", contract) { result ->
                    launcher?.unregister()
                    continuation.resume(result)
                }
                continuation.invokeOnCancellation { launcher.unregister() }
                launcher.launch(input)
            }
        }
}
