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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Broadcast receiver for FTP server start/stop commands.
 *
 * This is an abstract class that should be extended by the app module
 * to provide the concrete FtpServerService class.
 */
abstract class FtpReceiver : BroadcastReceiver() {
    companion object {
        @JvmStatic
        protected val logger: Logger = LoggerFactory.getLogger(FtpReceiver::class.java)
    }

    /**
     * Get the FTP service class to start/stop
     */
    abstract fun getFtpServiceClass(): Class<out FtpServerService>

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val serviceIntent = Intent(context, getFtpServiceClass())
        serviceIntent.putExtras(intent)

        runCatching {
            when (intent.action) {
                FtpPreferences.ACTION_START_FTPSERVER -> {
                    if (!FtpServerEngine.isRunning()) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                    Unit
                }
                FtpPreferences.ACTION_STOP_FTPSERVER -> {
                    context.stopService(serviceIntent)
                    Unit
                }
                else -> Unit
            }
        }.onFailure {
            logger.error("Failed to start/stop on intent", it)
        }
    }
}
