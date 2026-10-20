package de.geosphere.congregationplaner.organization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nimmt Einladungslinks entgegen, mit denen die App von außen gestartet wurde (Android-Intent,
 * Desktop-Startargument), bis die Login-Oberfläche sie übernimmt.
 */
object InviteLinkInbox {
    private val mutablePendingLink = MutableStateFlow<String?>(null)
    val pendingLink: StateFlow<String?> = mutablePendingLink.asStateFlow()

    fun deliver(link: String?) {
        if (!link.isNullOrBlank()) mutablePendingLink.value = link
    }

    fun consume() {
        mutablePendingLink.value = null
    }
}
