import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

apply(from = "${rootProject.projectDir}/gradle/kover.gradle.kts")

dependencies {
    implementation(projects.shared)

    implementation("com.google.firebase:firebase-admin:9.4.3")

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)
}

compose.desktop {
    application {
        mainClass = "de.geosphere.congregationplaner.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "de.geosphere.congregationplaner"
            packageVersion = "1.0.0"
        }
    }
}