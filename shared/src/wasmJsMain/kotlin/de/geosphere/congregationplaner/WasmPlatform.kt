package de.geosphere.congregationplaner

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val isDesktop: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()
