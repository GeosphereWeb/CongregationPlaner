plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kover)
    // SonarQube Gradle plugin for code quality analysis
    alias(libs.plugins.sonarcube)
}

// --- Zentrale Exclusions Definition ---
val koverExclusions = listOf(
    "de.geosphere.congregationplaner.AppKt",
    "de.geosphere.congregationplaner.Greeting",
    "de.geosphere.congregationplaner.MainKt",
    "de.geosphere.congregationplaner.MainActivity",
    "de.geosphere.congregationplaner.MainViewControllerKt"
)

val koverAnnotationExclusions = listOf(
    "androidx.compose.runtime.Composable",
    "androidx.compose.ui.tooling.preview.Preview",
    "org.jetbrains.compose.ui.tooling.preview.Preview"
)

val sonarExclusions = listOf(
    "**/App.kt",
    "**/Greeting.kt",
    "**/main.kt",
    "**/MainActivity.kt",
    "**/MainViewController.kt",
    // Exclude theming and ui modules from coverage
    "theming/**",
    "ui/**"
)

// Unterprojekte nicht separat analysieren – verhindert doppelte Indexierung,
// wenn sonar.sources am Root zusätzlich zu Gradle-Auto-Detect gesetzt ist.
subprojects {
    sonar {
        isSkipProject = true
    }
}

// KMP source sets (commonMain, jvmMain, …) explizit setzen, damit SonarCloud
// Kover-Reports (package + Dateiname) den Quelldateien zuordnen kann.
// Nur echte src/<sourceSet>/kotlin-Ordner – keine build/-Artefakte.
fun collectKotlinSourceDirs(includeTests: Boolean): String =
    subprojects.flatMap { project ->
        val srcDir = project.file("src")
        if (!srcDir.exists()) {
            emptyList<String>()
        } else {
            srcDir.walkTopDown()
                .maxDepth(2)
                .filter { it.isDirectory && it.name == "kotlin" }
                .filter { dir ->
                    val sourceSet = dir.parentFile.name
                    val isTestSourceSet = sourceSet.endsWith("Test") || sourceSet == "test"
                    isTestSourceSet == includeTests
                }
                .map { it.relativeTo(rootDir).invariantSeparatorsPath }
                .toList()
        }
    }.distinct().sorted().joinToString(",")

val koverInstrumentedTestTasks = listOf(
    "jvmTest",
    "testAndroidHostTest",
    "test",
)

// Zentraler Ausschluss für Kover (gilt für alle Module)
allprojects {
    plugins.withId("org.jetbrains.kotlinx.kover") {
        configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        // Nutze die zentrale Liste für Annotationen (z.B. @Composable)
                        koverAnnotationExclusions.forEach { annotatedBy(it) }

                        // Nutze die zentrale Liste für Klassen
                        classes(koverExclusions)
                    }
                }
            }
        }
    }

    // WICHTIG: Auch für Sonar die Exclusions pro Projekt setzen
    plugins.withId("org.sonarqube") {
        configure<org.sonarqube.gradle.SonarExtension> {
            properties {
                property("sonar.coverage.exclusions", sonarExclusions.joinToString(","))
            }
        }
    }
}


sonar {
    properties {
        property("sonar.projectKey", "GeosphereWeb_CongregationPlaner")
        property("sonar.organization", "geosphereweb")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "CongregationPlaner")
        property("sonar.projectVersion", "1.0.0")
        property("sonar.sourceEncoding", "UTF-8")
        
        // Exclude build folders and non-source files
        property("sonar.exclusions", "**/build/**,**/.gradle/**,**/iosApp/**,**/theming/**,**/ui/**,**/*.png,**/*.xml")

        property("sonar.sources", collectKotlinSourceDirs(includeTests = false))
        property("sonar.tests", collectKotlinSourceDirs(includeTests = true))

        // Verheiratung: SonarCloud den Pfad zum Kover-XML geben
        // Wir nutzen hier einen relativen Pfad vom Root aus
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/merged/report.xml")

        // Nutze die zentrale Liste für Coverage Exclusions auch am Root
        property("sonar.coverage.exclusions", sonarExclusions.joinToString(","))
        
        // Use the system Java for scanning to avoid 403 JRE provisioning error
        property("sonar.scanner.skipJreProvisioning", "true")
    }
}

// Configure Kover merged XML report if available (Kover >= 0.9.x provides a merged report task).
// If the official `koverMergedXmlReport` task is present at evaluation time, prefer it.
// Otherwise, fall back to a lightweight merger that concatenates module reports.
gradle.projectsEvaluated {
    subprojects.forEach { sp ->
        sp.tasks.findByName("koverXmlReport")?.let { koverTask ->
            koverInstrumentedTestTasks.forEach { testTaskName ->
                sp.tasks.findByName(testTaskName)?.let { koverTask.dependsOn(it) }
            }
        }
    }

    // try to find the official merged task provided by the Kover plugin
    val officialMerged = tasks.findByName("koverMergedXmlReport")

    if (officialMerged != null) {
        // Make sure each module's koverXmlReport runs before the merged task
        tasks.named("koverMergedXmlReport") {
            // ensure subprojects with koverXmlReport are executed first
            subprojects.forEach { sp ->
                sp.tasks.findByName("koverXmlReport")?.let { dependsOn(it) }
            }

            // After the official merged task runs, copy the result to a stable path
            doLast {
                val defaultLocation = file("${'$'}{buildDir}/reports/kover/report.xml")
                val target = file("${'$'}{buildDir}/reports/kover/merged/report.xml")
                if (defaultLocation.exists()) {
                    target.parentFile.mkdirs()
                    defaultLocation.copyTo(target, overwrite = true)
                }
            }
        }

        // Make sonar depend on the official merged task
        tasks.named("sonar") { dependsOn("koverMergedXmlReport") }
    } else {
        // Fallback: create a simple merger similar to the previous implementation
        tasks.register("mergeKoverXml") {
            group = "verification"
            description = "Merge per-module Kover XML reports into build/reports/kover/merged/report.xml"

            // Explizite Abhängigkeiten zu den Reports der Unterprojekte hinzufügen
            subprojects.forEach { sp ->
                sp.tasks.findByName("koverXmlReport")?.let { dependsOn(it) }
            }

            val reports = files(
                "shared/build/reports/kover/report.xml",
                "androidApp/build/reports/kover/report.xml",
                "desktopApp/build/reports/kover/report.xml",
                "webApp/build/reports/kover/report.xml"
            )
            inputs.files(reports)
            val outFile = layout.buildDirectory.file("reports/kover/merged/report.xml")
            outputs.file(outFile)

            doLast {
                val out = outFile.get().asFile
                out.parentFile.mkdirs()
                out.writeText("<?xml version=\"1.0\" ?>\n<report name=\"Merged Coverage Report\">\n")
                reports.files.forEach { f ->
                    if (f.exists()) {
                        val text = f.readText()
                        // remove XML declaration and outer <report> wrapper so we only append package/class counters
                        val inner = text.substringAfter("<report").substringAfter(">").substringBeforeLast("</report>")
                        out.appendText(inner)
                    }
                }
                out.appendText("\n</report>\n")
            }
        }

        // Ensure Sonar runs after the fallback merged Kover report is generated
        tasks.named("sonar") { dependsOn("mergeKoverXml") }
    }
}

// Create a merged HTML coverage report that collects per-module Kover HTML reports
// If Kover provides an official merged HTML task (koverMergedHtmlReport) use it,
// otherwise create a lightweight aggregator that runs every subproject's
// `koverHtmlReport` (if present) and copies their HTML output into
// `build/reports/kover/merged/html/<module>` with a small index page linking to them.
gradle.projectsEvaluated {
    val officialHtml = tasks.findByName("koverMergedHtmlReport")

    if (officialHtml != null) {
        tasks.named("koverMergedHtmlReport") {
            subprojects.forEach { sp ->
                sp.tasks.findByName("koverHtmlReport")?.let { dependsOn(it) }
            }

            // copy the official output into a stable merged location
            doLast {
                val defaultDir = file("${'$'}{buildDir}/reports/kover/html")
                val targetDir = file("${'$'}{buildDir}/reports/kover/merged/html")
                if (defaultDir.exists()) {
                    targetDir.deleteRecursively()
                    defaultDir.copyRecursively(targetDir)
                }
            }
        }
    } else {
        tasks.register("koverMergedHtmlReport") {
            group = "verification"
            description = "Collect per-module Kover HTML reports into build/reports/kover/merged/html"
            // make sure each module's html report (if available) is generated first
            subprojects.forEach { sp ->
                sp.tasks.findByName("koverHtmlReport")?.let { dependsOn(it) }
            }

            doLast {
                val merged = file("${'$'}{buildDir}/reports/kover/merged/html")
                merged.deleteRecursively()
                merged.mkdirs()

                // copy each subproject's html report (if exists) to merged/<projectName>
                subprojects.forEach { sp ->
                    val src = file("${'$'}{sp.buildDir}/reports/kover/html")
                    if (src.exists()) {
                        val dest = File(merged, sp.name)
                        src.copyRecursively(dest, overwrite = true)
                    }
                }

                // also include this root project's html report if present
                val rootHtml = file("${'$'}{buildDir}/reports/kover/html")
                if (rootHtml.exists()) {
                    rootHtml.copyRecursively(File(merged, "root"), overwrite = true)
                }

                // write a minimal index.html linking to the collected reports
                val index = File(merged, "index.html")
                index.writeText(buildString {
                    append("<html><head><meta charset=\"utf-8\"/><title>Merged Coverage Reports</title></head><body>\n")
                    append("<h1>Merged Coverage Reports</h1>\n<ul>\n")
                    // add root link if exists
                    if (rootHtml.exists()) append("<li><a href=\"root/index.html\">root</a></li>\n")
                    subprojects.forEach { sp ->
                        val candidate = file("${'$'}{sp.buildDir}/reports/kover/html/index.html")
                        if (candidate.exists()) {
                            append("<li><a href=\"${sp.name}/index.html\">${sp.name}</a></li>\n")
                        }
                    }
                    append("</ul>\n</body></html>")
                })
            }
        }
    }
}
