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

package com.amaze.filemanager.asynchronous.services.ftp

import android.app.Notification
import android.content.res.Resources
import androidx.preference.PreferenceManager
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.R
import com.amaze.filemanager.ftpserver.commands.AVBL
import com.amaze.filemanager.ftpserver.service.FtpServerService
import com.amaze.filemanager.server.ServerRegistry
import com.amaze.filemanager.server.ServerType
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_ROOTMODE
import com.amaze.filemanager.utils.PasswordUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException

/**
 * Concrete implementation of FtpServerService for the Amaze File Manager app.
 *
 * This class provides app-specific implementations for notifications, keystore access,
 * password decryption, and error messages.
 */
class AppFtpService : FtpServerService() {
    private val serverNotification
        get() = ServerRegistry.getProvider(ServerType.FTP)!!.getNotification()

    override fun getNotificationId(): Int = serverNotification.getNotificationId()

    override fun getNotificationChannelId(): String = serverNotification.getChannelId()

    override fun createStartingNotification(noStopButton: Boolean): Notification {
        return serverNotification.createStartingNotification(applicationContext, noStopButton)
    }

    override fun updateRunningNotification(noStopButton: Boolean) {
        serverNotification.updateRunningNotification(applicationContext, noStopButton)
    }

    override fun getKeyStoreInputStream(): InputStream? {
        return try {
            resources.openRawResource(R.raw.key)
        } catch (e: Resources.NotFoundException) {
            log.error("Failed to open keystore", e)
            null
        }
    }

    override fun getKeyStorePassword(): String {
        return BuildConfig.FTP_SERVER_KEYSTORE_PASSWORD
    }

    override fun decryptPassword(encryptedPassword: String): String? {
        return try {
            PasswordUtil.decryptPassword(applicationContext, encryptedPassword)
        } catch (e: GeneralSecurityException) {
            log.warn("Failed to decrypt password", e)
            null
        } catch (e: IOException) {
            log.warn("Unexpected error during password decryption", e)
            null
        }
    }

    override fun isRootModeEnabled(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        return preferences.getBoolean(PREFERENCE_ROOTMODE, false)
    }

    override fun getErrorMessageProvider(): AVBL.ErrorMessageProvider {
        return object : AVBL.ErrorMessageProvider {
            override fun getErrorMessage(
                subId: String,
                fileName: String?,
            ): String {
                return when (subId) {
                    "AVBL.notimplemented" -> getString(R.string.ftp_error_AVBL_notimplemented)
                    "AVBL.accessdenied" -> getString(R.string.ftp_error_AVBL_accessdenied)
                    "AVBL.isafile" -> getString(R.string.ftp_error_AVBL_isafile)
                    "AVBL.missing" -> getString(R.string.ftp_error_AVBL_missing)
                    else -> getString(R.string.unknown_error)
                }
            }
        }
    }

    override fun getFeatResponse(): String {
        return getString(R.string.ftp_command_FEAT)
    }

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(AppFtpService::class.java)
    }
}
