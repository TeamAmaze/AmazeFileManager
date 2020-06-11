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

/** Created by yashwanthreddyg on 09-06-2016. */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FtpReceiver extends BroadcastReceiver {

  static final String TAG = FtpReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v(TAG, "Received: " + intent.getAction());

    try {
      Intent service = new Intent(context, FtpService.class);
      service.putExtras(intent);
      if (intent.getAction().equals(FtpService.ACTION_START_FTPSERVER) && !FtpService.isRunning()) {
        context.startService(service);
      } else if (intent.getAction().equals(FtpService.ACTION_STOP_FTPSERVER)) {
        context.stopService(service);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to start/stop on intent " + e.getMessage());
    }
  }
}
