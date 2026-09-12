package de.geosphere.congregationplaner

expect class FirebasePlatformSupport() {
    fun initialize()
    fun isReady(): Boolean
}

object FirebaseSupport {
    private val platform by lazy { FirebasePlatformSupport() }

    fun initialize() {
        platform.initialize()
    }

    fun isReady(): Boolean = platform.isReady()
}
