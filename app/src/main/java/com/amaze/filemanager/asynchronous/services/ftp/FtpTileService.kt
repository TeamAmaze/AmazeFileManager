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

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.PowerManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.amaze.filemanager.R
import com.amaze.filemanager.ftpserver.service.FtpEventBus
import com.amaze.filemanager.ftpserver.service.FtpPreferences
import com.amaze.filemanager.ftpserver.service.FtpServerEngine
import com.amaze.filemanager.utils.NetworkUtil.isConnectedToLocalNetwork
import com.amaze.filemanager.utils.NetworkUtil.isConnectedToWifi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * [AppFtpService] tile service to start and stop the FTP server.
 *
 * Created by vishal on 1/1/17.  */
@RequiresApi(Build.VERSION_CODES.N)
class FtpTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            FtpEventBus.events.collect { _ ->
                onFtpReceiverActions()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        unlockAndRun {
            if (FtpServerEngine.isRunning()) {
                applicationContext
                    .sendBroadcast(
                        Intent(FtpPreferences.ACTION_STOP_FTPSERVER).setPackage(packageName),
                    )
            } else {
                if (isConnectedToWifi(applicationContext) ||
                    isConnectedToLocalNetwork(applicationContext)
                ) {
                    val pm = getSystemService(POWER_SERVICE) as PowerManager
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        Toast.makeText(
                            applicationContext,
                            R.string.ftp_battery_optimization_tile_warning,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    val i = Intent(FtpPreferences.ACTION_START_FTPSERVER).setPackage(packageName)
                    i.putExtra(FtpPreferences.TAG_STARTED_BY_TILE, true)
                    applicationContext.sendBroadcast(i)
                } else {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.ftp_no_wifi),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile
        if (FtpServerEngine.isRunning()) {
            tile.state = Tile.STATE_ACTIVE
            tile.icon =
                Icon.createWithResource(
                    this,
                    R.drawable.ic_ftp_dark,
                )
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.icon =
                Icon.createWithResource(
                    this,
                    R.drawable.ic_ftp_light,
                )
        }
        tile.updateTile()
    }

    private fun onFtpReceiverActions() {
        updateTileState()
    }
}
