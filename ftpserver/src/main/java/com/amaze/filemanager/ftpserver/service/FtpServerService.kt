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

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.amaze.filemanager.ftpserver.commands.FtpCommandMessageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * FTP Server Android Service.
 *
 * This service manages the FTP server lifecycle as a foreground service.
 */
@Suppress("LabeledExpression")
abstract class FtpServerService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isStartedByTile = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Get the notification ID for this service
     */
    abstract fun getNotificationId(): Int

    /**
     * Get the notification channel ID
     */
    abstract fun getNotificationChannelId(): String

    /**
     * Create the starting notification
     */
    abstract fun createStartingNotification(noStopButton: Boolean): android.app.Notification

    /**
     * Update the running notification
     */
    abstract fun updateRunningNotification(noStopButton: Boolean)

    /**
     * Get the keystore input stream for SSL
     */
    abstract fun getKeyStoreInputStream(): InputStream?

    /**
     * Get the keystore password
     */
    abstract fun getKeyStorePassword(): String

    /**
     * Get decrypted password for FTP authentication
     */
    abstract fun decryptPassword(encryptedPassword: String): String?

    /**
     * Check if root mode is enabled
     */
    abstract fun isRootModeEnabled(): Boolean

    /**
     * Get error message provider for AVBL command
     */
    abstract fun getMessageProvider(): FtpCommandMessageProvider

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock.setReferenceCounted(false)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        isStartedByTile = intent?.getBooleanExtra(FtpPreferences.TAG_STARTED_BY_TILE, false) == true

        // Start as foreground service immediately to avoid ANR
        val notification = createStartingNotification(isStartedByTile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                getNotificationId(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(getNotificationId(), notification)
        }

        // Wait for any existing server to stop off the main thread, then start
        serviceScope.launch(Dispatchers.IO) {
            var attempts = 10
            while (FtpServerEngine.isRunning()) {
                if (attempts > 0) {
                    attempts--
                    delay(1000)
                } else {
                    log.warn("FTP server did not stop in time; aborting start.")
                    stopSelf()
                    return@launch
                }
            }
            startServer()
        }

        return START_STICKY
    }

    private fun startServer() {
        wakeLock.acquire(TimeUnit.HOURS.toMillis(1L))

        val prefs = FtpPreferences.getPreferences(this)

        // Get password if authentication is enabled
        val username = FtpPreferences.getUsername(this)
        val password =
            if (username.isNotEmpty()) {
                val encryptedPassword = prefs.getString(FtpPreferences.KEY_PREFERENCE_PASSWORD, "") ?: ""
                if (encryptedPassword.isNotEmpty()) {
                    decryptPassword(encryptedPassword)
                } else {
                    null
                }
            } else {
                null
            }

        val config =
            FtpServerEngine.ServerConfig(
                port = FtpPreferences.getPort(this),
                timeout = FtpPreferences.getTimeout(this),
                path = FtpPreferences.getPath(this),
                username = username.takeIf { it.isNotEmpty() },
                password = password,
                isSecure = FtpPreferences.isSecure(this),
                isReadOnly = FtpPreferences.isReadOnly(this),
                useSafFilesystem = FtpPreferences.useSafFilesystem(this),
                useRootFilesystem = isRootModeEnabled(),
                keyStoreInputStream = if (FtpPreferences.isSecure(this)) getKeyStoreInputStream() else null,
                keyStorePassword = getKeyStorePassword(),
                ftpCommandMessageProvider = getMessageProvider(),
                startedByTile = isStartedByTile,
            )

        FtpServerEngine.start(this, config) { success ->
            if (success) {
                updateRunningNotification(isStartedByTile)
            } else {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        FtpServerEngine.stop()

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(FtpServerService::class.java)
    }
}
