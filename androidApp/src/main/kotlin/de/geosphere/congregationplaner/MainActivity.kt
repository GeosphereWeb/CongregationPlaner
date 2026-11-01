package de.geosphere.congregationplaner

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.geosphere.congregationplaner.auth.FirebaseAndroidContextHolder
import de.geosphere.congregationplaner.auth.FirebaseSupport
import de.geosphere.congregationplaner.organization.InviteLinkInbox

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FirebaseAndroidContextHolder.configure(
            context = applicationContext,
            activity = this,
            useAuthEmulator = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            authEmulatorHost = applicationInfo.metaData
                ?.getString("firebase_auth_emulator_host")
                ?.takeIf { it.isNotBlank() }
                ?: "10.0.2.2",
        )
        FirebaseSupport.initialize()
        InviteLinkInbox.deliver(intent?.dataString)

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
}

@Preview
@Composable
fun AppAndroidPreview() {
    AppPreview()
}
