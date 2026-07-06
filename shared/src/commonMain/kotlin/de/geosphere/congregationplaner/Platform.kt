package de.geosphere.congregationplaner

/**
 * Repräsentiert eine spezifische Laufzeit-Plattform (z. B. Android, iOS, JVM).
 */
interface Platform {
    /**
     * Der Anzeigename der Plattform inklusive Version.
     */
    val name: String

    /**
     * Gibt an, ob es sich um eine Desktop-Plattform handelt.
     */
    val isDesktop: Boolean
}

/**
 * Erzeugt eine Instanz der aktuellen [Platform].
 *
 * @return Die plattformspezifische Implementierung.
 */
expect fun getPlatform(): Platform

/**
 * Ein zentraler Helfer, um Plattform-Eigenschaften abzufragen.
 */
object HostPlatform {
    /**
     * Die Instanz der aktuellen Plattform. Wird beim ersten Zugriff initialisiert.
     */
    val current: Platform by lazy { getPlatform() }

    /**
     * Kurzschreibweise, um zu prüfen, ob die App auf einem Desktop läuft.
     */
    val isDesktop: Boolean get() = current.isDesktop
}
