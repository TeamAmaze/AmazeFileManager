/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import com.amaze.filemanager.BuildConfig.DEBUG
import com.amaze.filemanager.asynchronous.services.ftp.FtpService.Companion.isRunning
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Created by yashwanthreddyg on 09-06-2016.  */
class FtpReceiver : BroadcastReceiver() {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FtpReceiver::class.java)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (DEBUG) {
            logger.debug("Received: ${intent.action}")
        }
        val service = Intent(context, FtpService::class.java)
        service.putExtras(intent)
        runCatching {
            when (intent.action) {
                FtpService.ACTION_START_FTPSERVER -> {
                    if (!isRunning()) {
                        ContextCompat.startForegroundService(context, service)
                    }
                }

                FtpService.ACTION_STOP_FTPSERVER -> {
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
