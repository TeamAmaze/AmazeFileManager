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

package com.amaze.filemanager.ftpserver.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Environment
import android.provider.DocumentsContract
import androidx.preference.PreferenceManager

/**
 * Configuration and preference keys for FTP server.
 */
object FtpPreferences {
    const val DEFAULT_PORT = 2211
    const val DEFAULT_USERNAME = ""
    const val DEFAULT_TIMEOUT = 600 // default timeout, in sec
    const val DEFAULT_SECURE = true

    const val PORT_PREFERENCE_KEY = "ftpPort"
    const val KEY_PREFERENCE_PATH = "ftp_path"
    const val KEY_PREFERENCE_USERNAME = "ftp_username"
    const val KEY_PREFERENCE_PASSWORD = "ftp_password_encrypted"
    const val KEY_PREFERENCE_TIMEOUT = "ftp_timeout"
    const val KEY_PREFERENCE_SECURE = "ftp_secure"
    const val KEY_PREFERENCE_READONLY = "ftp_readonly"
    const val KEY_PREFERENCE_SAF_FILESYSTEM = "ftp_saf_filesystem"
    const val KEY_PREFERENCE_ROOT_FILESYSTEM = "ftp_root_filesystem"

    const val INITIALS_HOST_FTP = "ftp://"
    const val INITIALS_HOST_SFTP = "ftps://"

    const val ACTION_START_FTPSERVER =
        "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_START_FTPSERVER"
    const val ACTION_STOP_FTPSERVER =
        "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_STOP_FTPSERVER"
    const val TAG_STARTED_BY_TILE = "started_by_tile"

    /**
     * Get default preferences for FTP server
     */
    @JvmStatic
    fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Get configured port
     */
    @JvmStatic
    fun getPort(context: Context): Int {
        return getPreferences(context).getInt(PORT_PREFERENCE_KEY, DEFAULT_PORT)
    }

    /**
     * Get whether secure connection is enabled
     */
    @JvmStatic
    fun isSecure(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_PREFERENCE_SECURE, DEFAULT_SECURE)
    }

    /**
     * Get configured timeout
     */
    @JvmStatic
    fun getTimeout(context: Context): Int {
        return getPreferences(context).getInt(KEY_PREFERENCE_TIMEOUT, DEFAULT_TIMEOUT)
    }

    /**
     * Get configured path
     */
    @JvmStatic
    fun getPath(context: Context): String {
        return getPreferences(context).getString(KEY_PREFERENCE_PATH, defaultPath(context))
            ?: defaultPath(context)
    }

    /**
     * Get configured username
     */
    @JvmStatic
    fun getUsername(context: Context): String {
        return getPreferences(context).getString(KEY_PREFERENCE_USERNAME, DEFAULT_USERNAME)
            ?: DEFAULT_USERNAME
    }

    /**
     * Check if read-only mode is enabled
     */
    @JvmStatic
    fun isReadOnly(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_PREFERENCE_READONLY, false)
    }

    /**
     * Check if SAF filesystem should be used
     */
    @JvmStatic
    fun useSafFilesystem(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_PREFERENCE_SAF_FILESYSTEM, false)
    }

    /**
     * Derive the FTP server's default share path, depending the user's Android version.
     */
    @JvmStatic
    fun defaultPath(context: Context): String {
        return if (useSafFilesystem(context) && SDK_INT > M) {
            DocumentsContract.buildTreeDocumentUri(
                "com.android.externalstorage.documents",
                "primary:",
            ).toString()
        } else {
            Environment.getExternalStorageDirectory().absolutePath
        }
    }
}
