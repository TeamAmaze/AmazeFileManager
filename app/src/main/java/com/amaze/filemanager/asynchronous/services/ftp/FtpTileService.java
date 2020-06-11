/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.services.ftp;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import com.amaze.filemanager.R;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** Created by vishal on 1/1/17. */
@TargetApi(Build.VERSION_CODES.N)
public class FtpTileService extends TileService {

  @Subscribe
  public void onFtpReceiverActions(FtpService.FtpReceiverActions signal) {
    updateTileState();
  }

  @Override
  public void onStartListening() {
    super.onStartListening();
    EventBus.getDefault().register(this);
    updateTileState();
  }

  @Override
  public void onStopListening() {
    super.onStopListening();
    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onClick() {
    unlockAndRun(
        () -> {
          if (FtpService.isRunning()) {
            getApplicationContext()
                .sendBroadcast(
                    new Intent(FtpService.ACTION_STOP_FTPSERVER).setPackage(getPackageName()));
          } else {
            if (FtpService.isConnectedToWifi(getApplicationContext())
                || FtpService.isConnectedToLocalNetwork(getApplicationContext())
                || FtpService.isEnabledWifiHotspot(getApplicationContext())) {
              Intent i = new Intent(FtpService.ACTION_START_FTPSERVER).setPackage(getPackageName());
              i.putExtra(FtpService.TAG_STARTED_BY_TILE, true);
              getApplicationContext().sendBroadcast(i);
            } else {
              Toast.makeText(
                      getApplicationContext(), getString(R.string.ftp_no_wifi), Toast.LENGTH_LONG)
                  .show();
            }
          }
        });
  }

  private void updateTileState() {
    Tile tile = getQsTile();
    if (FtpService.isRunning()) {
      tile.setState(Tile.STATE_ACTIVE);
      tile.setIcon(Icon.createWithResource(this, R.drawable.ic_ftp_dark));
    } else {
      tile.setState(Tile.STATE_INACTIVE);
      tile.setIcon(Icon.createWithResource(this, R.drawable.ic_ftp_light));
    }
    tile.updateTile();
  }
}
