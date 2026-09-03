package de.geosphere.congregationplaner

import java.io.File

internal object DesktopEnvLoader {
    private val candidateFiles by lazy {
        val currentDir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val searchRoots = linkedSetOf<File>()
        var dir = currentDir
        while (dir != null) {
            searchRoots += dir
            if (File(dir, "settings.gradle.kts").exists() || File(dir, "build.gradle.kts").exists() || File(dir, ".git").exists()) {
                searchRoots += dir
            }
            dir = dir.parentFile
        }

        val files = mutableListOf<File>()
        searchRoots.forEach { root ->
            files += File(root, ".env")
            files += File(root, ".env.example")
        }
        files += listOf(
            File(currentDir, ".env"),
            File(currentDir, ".env.example"),
            File(".env"),
            File(".env.example"),
        )

        files.distinctBy { it.absolutePath }
    }

    fun load(): Map<String, String> {
        val values = linkedMapOf<String, String>()
        candidateFiles.forEach { file ->
            if (file.exists() && file.isFile) {
                file.readLines().forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isEmpty() || line.startsWith("#")) {
                        return@forEach
                    }
                    val index = line.indexOf('=')
                    if (index <= 0) {
                        return@forEach
                    }
                    val key = line.substring(0, index).trim()
                    val value = line.substring(index + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                    if (key.isNotEmpty()) {
                        values[key] = value
                    }
                }
            }
        }
        return values
    }

    fun getValue(vararg keys: String): String? {
        keys.forEach { key ->
            System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }
            System.getProperty(key)?.takeIf { it.isNotBlank() }?.let { return it }
        }

        val loaded = load()
        keys.forEach { key ->
            loaded[key]?.takeIf { it.isNotBlank() }?.let { return it }
            loaded[key.replace("_", ".")]?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return null
    }
}
