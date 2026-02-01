/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.server

import androidx.fragment.app.Fragment

/**
 * Registry for server implementations.
 *
 * This allows the app to discover and use different server implementations
 * without direct dependencies on specific server modules.
 */
object ServerRegistry {
    private val servers = mutableMapOf<ServerType, ServerProvider>()

    /**
     * Register a server provider
     */
    fun register(provider: ServerProvider) {
        servers[provider.serverType] = provider
    }

    /**
     * Unregister a server provider
     */
    fun unregister(serverType: ServerType) {
        servers.remove(serverType)
    }

    /**
     * Get a server provider by type
     */
    fun getProvider(serverType: ServerType): ServerProvider? {
        return servers[serverType]
    }

    /**
     * Get all registered server providers
     */
    fun getAllProviders(): Collection<ServerProvider> {
        return servers.values
    }

    /**
     * Check if a server type is registered
     */
    fun isRegistered(serverType: ServerType): Boolean {
        return servers.containsKey(serverType)
    }
}

/**
 * Provider interface for creating server-related components.
 *
 * Each server module should implement this to provide its components.
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
