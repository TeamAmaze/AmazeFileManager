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

    /**
     * Clear all registered server providers.
     *
     * Primarily intended for test teardown to ensure a clean state between tests.
     */
    fun clearAll() {
        servers.clear()
    }
}
