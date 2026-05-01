plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.sonar)
}

// Detekt Konfiguration
detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

// ktlint Konfiguration
// Hier können bei Bedarf Anpassungen vorgenommen werden.
// Standardmäßig werden die ktlint-Tasks durch das Plugin hinzugefügt.

// SonarCloud Konfiguration
sonar {
    properties {
        property("sonar.projectKey", "GeosphereWeb_CongregationPlaner") // z.B. "CongregationPlaner"
        property("sonar.organization", "geosphereweb")
        property("sonar.host.url", "https://sonarcloud.io")
        // Pfade zu den Kover XML Reports (kommagetrennt für Multi-Modul)
        property("sonar.kotlin.xml.reportPaths", "composeApp/build/reports/kover/report.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "composeApp/build/reports/kover/report.xml") // Manchmal braucht Sonar diesen Fallback
    }
}