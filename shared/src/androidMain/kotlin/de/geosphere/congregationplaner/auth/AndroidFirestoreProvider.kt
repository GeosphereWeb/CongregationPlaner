package de.geosphere.congregationplaner.auth

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Liefert die gemeinsame Firestore-Instanz. Der Emulator darf pro Instanz nur einmal und vor dem
 * ersten Zugriff konfiguriert werden, daher wird die Instanz hier zentral und einmalig erzeugt.
 */
internal object AndroidFirestoreProvider {
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().also { firestore ->
            if (FirebaseAndroidContextHolder.useAuthEmulator) {
                firestore.useEmulator(FirebaseAndroidContextHolder.authEmulatorHost, FIRESTORE_EMULATOR_PORT)
            }
        }
    }

    private const val FIRESTORE_EMULATOR_PORT = 8080
}
