package de.geosphere.congregationplaner.auth

import java.io.File

internal object DesktopEnv {
    private val candidateFiles by lazy {
        val currentDir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val searchRoots = linkedSetOf<File>()
        var dir = currentDir
        while (dir != null) {
            searchRoots += dir
            if (File(dir, "settings.gradle.kts").exists() ||
                File(dir, "build.gradle.kts").exists() ||
                File(
                    dir,
                    ".git",
                ).exists()
            ) {
                searchRoots += dir
            }
            dir = dir.parentFile
        }

        val files = mutableListOf<File>()
        searchRoots.forEach { root ->
            files += File(root, ".env")
        }
        files += listOf(
            File(currentDir, ".env"),
            File(".env"),
        )

        files.distinctBy { it.absolutePath }
    }

    fun load(): Map<String, String> {
        val values = linkedMapOf<String, String>()
        candidateFiles.forEach { file -> loadFile(file, values) }
        return values
    }

    internal fun loadFile(file: File, values: MutableMap<String, String>) {
        if (!file.isFile) return

        file.forEachLine { rawLine ->
            parseEntry(rawLine)?.let { (key, value) -> values.putIfAbsent(key, value) }
        }
    }

    internal fun parseEntry(rawLine: String): Pair<String, String>? {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return null

        val separator = line.indexOf('=')
        if (separator <= 0) return null

        val key = line.substring(0, separator).trim()
        if (key.isEmpty()) return null

        val value = line.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
        return key to value
    }

    fun getValue(vararg keys: String): String? {
        val environment = keys.mapNotNull { key -> System.getenv(key)?.let { key to it } }.toMap()
        val properties = keys.mapNotNull { key -> System.getProperty(key)?.let { key to it } }.toMap()
        return findValue(keys, environment, properties, load())
    }

    internal fun findValue(
        keys: Array<out String>,
        environment: Map<String, String>,
        properties: Map<String, String>,
        loaded: Map<String, String>,
    ): String? {
        keys.forEach { key ->
            environment[key]?.takeIf { it.isNotBlank() }?.let { return it }
            properties[key]?.takeIf { it.isNotBlank() }?.let { return it }
        }

        keys.forEach { key ->
            loaded[key]?.takeIf { it.isNotBlank() }?.let { return it }
            loaded[key.replace("_", ".")]?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return null
    }
}
