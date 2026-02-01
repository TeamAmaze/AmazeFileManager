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

package com.amaze.filemanager.ftpserver

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.amaze.filemanager.ftpserver.service.FtpPreferences
import com.amaze.filemanager.ftpserver.service.FtpServerEngine
import com.amaze.filemanager.server.FileServer
import com.amaze.filemanager.server.ServerNotification
import com.amaze.filemanager.server.ServerPreferences
import com.amaze.filemanager.server.ServerProvider
import com.amaze.filemanager.server.ServerType

/**
 * FTP Server provider implementation for the server registry.
 */
class FtpServerProvider(
    private val context: Context,
    private val fragmentFactory: () -> Fragment,
    private val notificationHandler: ServerNotification
) : ServerProvider {

    override val serverType: ServerType = ServerType.FTP

    override val displayName: String = "FTP Server"

    override fun createFragment(): Fragment = fragmentFactory()

    override fun getPreferences(): ServerPreferences = FtpServerPreferencesImpl()

    override fun getNotification(): ServerNotification = notificationHandler

    override fun isServerRunning(): Boolean = FtpServerEngine.isRunning()

    override fun getServerUrl(): String? {
        if (!isServerRunning()) return null
        val port = FtpPreferences.getPort(context)
        val secure = FtpPreferences.isSecure(context)
        val prefix = if (secure) FtpPreferences.INITIALS_HOST_SFTP else FtpPreferences.INITIALS_HOST_FTP
        // Note: actual IP address needs to be obtained from NetworkUtil in the app module
        return "${prefix}localhost:$port/"
    }

    /**
     * Implementation of ServerPreferences for FTP
     */
    private inner class FtpServerPreferencesImpl : ServerPreferences {
        override fun getPreferences(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

        override fun getPort(context: Context): Int = FtpPreferences.getPort(context)

        override fun setPort(context: Context, port: Int) {
            getPreferences(context).edit()
                .putInt(FtpPreferences.PORT_PREFERENCE_KEY, port)
                .apply()
        }

        override fun getPath(context: Context): String = FtpPreferences.getPath(context)

        override fun setPath(context: Context, path: String) {
            getPreferences(context).edit()
                .putString(FtpPreferences.KEY_PREFERENCE_PATH, path)
                .apply()
        }

        override fun getUsername(context: Context): String? =
            FtpPreferences.getUsername(context).takeIf { it.isNotEmpty() }

        override fun setUsername(context: Context, username: String?) {
            getPreferences(context).edit()
                .putString(FtpPreferences.KEY_PREFERENCE_USERNAME, username ?: "")
                .apply()
        }

        override fun isAuthenticationEnabled(context: Context): Boolean =
            getUsername(context) != null

        override fun isSecureConnection(context: Context): Boolean =
            FtpPreferences.isSecure(context)

        override fun setSecureConnection(context: Context, secure: Boolean) {
            getPreferences(context).edit()
                .putBoolean(FtpPreferences.KEY_PREFERENCE_SECURE, secure)
                .apply()
        }

        override fun isReadOnly(context: Context): Boolean =
            FtpPreferences.isReadOnly(context)

        override fun setReadOnly(context: Context, readOnly: Boolean) {
            getPreferences(context).edit()
                .putBoolean(FtpPreferences.KEY_PREFERENCE_READONLY, readOnly)
                .apply()
        }

        override fun getTimeout(context: Context): Int = FtpPreferences.getTimeout(context)

        override fun setTimeout(context: Context, timeout: Int) {
            getPreferences(context).edit()
                .putInt(FtpPreferences.KEY_PREFERENCE_TIMEOUT, timeout)
                .apply()
        }
    }
}
