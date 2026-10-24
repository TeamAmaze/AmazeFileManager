/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Receiver for third-party apps (e.g. Tasker) holding the dangerous
 * CONTROL_FTP_SERVER_SIMPLE permission. Only starts/stops the server with the user's saved
 * settings; extras of the incoming intent are deliberately never forwarded.
 */
class FtpExternalControlReceiver : BroadcastReceiver() {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FtpExternalControlReceiver::class.java)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val service = Intent(context, FtpService::class.java)
        runCatching {
            when (intent.action) {
                FtpService.ACTION_EXTERNAL_START_FTP -> {
                    if (!FtpService.isRunning()) {
                        ContextCompat.startForegroundService(context, service)
                    }
                }

                FtpService.ACTION_EXTERNAL_STOP_FTP -> {
                    context.stopService(service)
                }

                else -> {
                    Unit
                }
            }
        }.onFailure {
            logger.error("Failed to start/stop on intent ${it.message}")
        }
    }
}
