package de.geosphere.congregationplaner

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FirebaseAndroidContextHolder.configure(
            context = applicationContext,
            useAuthEmulator = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            authEmulatorHost = applicationInfo.metaData
                ?.getString("firebase_auth_emulator_host")
                ?.takeIf { it.isNotBlank() }
                ?: "10.0.2.2",
        )
        FirebaseSupport.initialize()

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}