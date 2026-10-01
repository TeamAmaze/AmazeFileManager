package com.amaze.filemanager.server

import androidx.fragment.app.Fragment

/**
 * Provider interface for creating server-related components.
 */
interface ServerProvider {
    /**
     * The server type this provider handles
     */
    val serverType: ServerType

    /**
     * Display name for this server type
     */
    val displayName: String

    /**
     * Create the UI fragment for this server
     */
    fun createFragment(): Fragment

    /**
     * Get the server preferences handler
     */
    fun getPreferences(): ServerPreferences

    /**
     * Get the notification handler
     */
    fun getNotification(): ServerNotification

    /**
     * Check if the server is currently running
     */
    fun isServerRunning(): Boolean

    /**
     * Get the server URL if running
     */
    fun getServerUrl(): String?
}
