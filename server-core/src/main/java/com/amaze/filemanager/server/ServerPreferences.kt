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

import android.content.Context
import android.content.SharedPreferences

/**
 * Interface for server preferences management.
 *
 * Each server implementation can have its own preferences for port, path, credentials, etc.
 */
interface ServerPreferences {
    /**
     * Get the shared preferences instance for this server
     */
    fun getPreferences(context: Context): SharedPreferences

    /**
     * Get the configured port
     */
    fun getPort(context: Context): Int

    /**
     * Set the port
     */
    fun setPort(
        context: Context,
        port: Int,
    )

    /**
     * Get the configured path to share
     */
    fun getPath(context: Context): String

    /**
     * Set the path to share
     */
    fun setPath(
        context: Context,
        path: String,
    )

    /**
     * Get the configured username (if authentication is enabled)
     */
    fun getUsername(context: Context): String?

    /**
     * Set the username
     */
    fun setUsername(
        context: Context,
        username: String?,
    )

    /**
     * Check if the server requires authentication
     */
    fun isAuthenticationEnabled(context: Context): Boolean

    /**
     * Check if the server is configured for secure connection
     */
    fun isSecureConnection(context: Context): Boolean

    /**
     * Set secure connection preference
     */
    fun setSecureConnection(
        context: Context,
        secure: Boolean,
    )

    /**
     * Check if the server is read-only
     */
    fun isReadOnly(context: Context): Boolean

    /**
     * Set read-only preference
     */
    fun setReadOnly(
        context: Context,
        readOnly: Boolean,
    )

    /**
     * Get the idle timeout in seconds
     */
    fun getTimeout(context: Context): Int

    /**
     * Set the idle timeout
     */
    fun setTimeout(
        context: Context,
        timeout: Int,
    )
}
