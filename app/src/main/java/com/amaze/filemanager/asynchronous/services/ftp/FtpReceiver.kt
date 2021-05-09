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
import android.util.Log
import com.amaze.filemanager.BuildConfig.DEBUG
import com.amaze.filemanager.asynchronous.services.ftp.FtpService.Companion.isRunning

/** Created by yashwanthreddyg on 09-06-2016.  */
class FtpReceiver : BroadcastReceiver() {

    private val TAG = FtpReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) {
            Log.v(TAG, "Received: ${intent.action}")
        }
        val service = Intent(context, FtpService::class.java)
        service.putExtras(intent)
        runCatching {
            if (intent.action == FtpService.ACTION_START_FTPSERVER && !isRunning()) {
                context.startService(service)
            } else if (intent.action == FtpService.ACTION_STOP_FTPSERVER) {
                context.stopService(service)
            } else Unit
        }.onFailure {
            Log.e(TAG, "Failed to start/stop on intent ${it.message}")
        }
    }
}
