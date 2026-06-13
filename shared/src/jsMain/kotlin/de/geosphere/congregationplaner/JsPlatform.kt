package de.geosphere.congregationplaner

import web.navigator.navigator

actual fun getPlatform(): Platform = JsPlatform(navigator.userAgent)
