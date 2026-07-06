package de.geosphere.congregationplaner

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WasmPlatformTest :
    FunSpec({
        test("WasmPlatform should return valid name") {
            val platform = WasmPlatform()
            platform.name shouldBe "Web with Kotlin/Wasm"
        }

        test("WasmPlatform isDesktop should be false") {
            WasmPlatform().isDesktop shouldBe false
        }
    })
