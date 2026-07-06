package de.geosphere.congregationplaner

private val BROWSER_NAMES = listOf("Chrome", "Firefox", "Safari", "Edge")

internal fun detectBrowserName(userAgent: String): String =
    userAgent.findAnyOf(BROWSER_NAMES, ignoreCase = true)
        ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
        ?: "Unknown"

class JsPlatform(userAgent: String) : Platform {
    override val name: String = detectBrowserName(userAgent)
    override val isDesktop: Boolean = false
}
