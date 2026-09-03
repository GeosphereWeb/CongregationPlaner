import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    id("dev.zacsweers.metro") version "1.2.1"
}

apply(from = "${rootProject.projectDir}/gradle/kover.gradle.kts")

compose.resources {
    publicResClass = true
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "de.geosphere.congregationplaner.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation("com.google.firebase:firebase-analytics-ktx:22.3.0")
            implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
            implementation(libs.compose.uiToolingPreview)
        }
        jvmMain.dependencies {
            implementation("com.google.firebase:firebase-admin:9.4.3")
        }
        commonMain.dependencies {
            implementation(projects.theming)
            implementation(projects.uilib)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.assertions)
            implementation(libs.kotest.framework)
            implementation(libs.turbine)
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// ktlint configuration
ktlint {
    version.set("1.3.0")
    verbose.set(false)
    android.set(false)
    outputToConsole.set(true)
    coloredOutput.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
    }
}

// detekt configuration
detekt {
    toolVersion = "1.23.6"
    config.setFrom("${rootProject.projectDir}/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
//    allRules = false
    ignoreFailures = false
}
