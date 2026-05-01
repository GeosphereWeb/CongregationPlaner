package de.geosphere.congregationplaner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform