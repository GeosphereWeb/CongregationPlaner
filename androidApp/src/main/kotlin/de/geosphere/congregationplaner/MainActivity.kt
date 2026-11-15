package de.geosphere.congregationplaner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.geosphere.congregationplaner.auth.data.FirebaseAndroidContextHolder
import de.geosphere.congregationplaner.auth.presentation.AppPreview
import de.geosphere.congregationplaner.notification.NotificationCheckWorker
import de.geosphere.congregationplaner.organization.presentation.selection.InviteLinkInbox

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Firebase konfiguriert CongregationPlanerApplication; die Google-Anmeldung braucht zusätzlich die Activity.
        FirebaseAndroidContextHolder.activity = this
        InviteLinkInbox.deliver(intent?.dataString)
        requestNotificationPermission()
        NotificationCheckWorker.checkNow(this)

        setContent {
            App()
        }

        // Immersive fullscreen: bars reappear transiently on edge swipe.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        InviteLinkInbox.deliver(intent.dataString)
    }

    // Ab Android 13 braucht das Anzeigen von Benachrichtigungen die Zustimmung; Android fragt höchstens zweimal.
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AppPreview()
}
