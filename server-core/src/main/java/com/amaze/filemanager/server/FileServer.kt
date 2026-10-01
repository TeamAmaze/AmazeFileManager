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
 * Interface for file server implementations (FTP, SSH, WebDAV, etc.)
 *
 */
interface FileServer {
    /**
     * Unique identifier for this server type
     */
    val serverType: ServerType

    /**
     * Human-readable name for this server
     */
    val displayName: String

    /**
     * Check if the server is currently running
     */
    fun isRunning(): Boolean

    /**
     * Get the URL to connect to this server
     * @return URL string or null if server is not running
     */
    fun getServerUrl(): String?

    /**
     * Get the default port for this server type
     */
    fun getDefaultPort(): Int

    /**
     * Get the currently configured port
     */
    fun getPort(): Int
}

/**
 * Enum representing different server types
 */
enum class ServerType(val id: String) {
    FTP("ftp"),
}
