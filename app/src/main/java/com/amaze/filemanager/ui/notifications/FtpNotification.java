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

package com.amaze.filemanager.ui.notifications;

import java.net.InetAddress;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.services.ftp.FtpService;
import com.amaze.filemanager.ui.activities.MainActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

/**
 * Created by yashwanthreddyg on 19-06-2016.
 *
 * <p>Edited by zent-co on 30-07-2019
 */
public class FtpNotification {

  private static NotificationCompat.Builder buildNotification(
      Context context, @StringRes int contentTitleRes, String contentText, boolean noStopButton) {
    Intent notificationIntent = new Intent(context, MainActivity.class);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

    long when = System.currentTimeMillis();

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_FTP_ID)
            .setContentTitle(context.getString(contentTitleRes))
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_ftp_light)
            .setTicker(context.getString(R.string.ftp_notif_starting))
            .setWhen(when)
            .setOngoing(true)
            .setOnlyAlertOnce(true);

    if (!noStopButton) {
      int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
      CharSequence stopText = context.getString(R.string.ftp_notif_stop_server);
      Intent stopIntent =
          new Intent(FtpService.ACTION_STOP_FTPSERVER).setPackage(context.getPackageName());
      PendingIntent stopPendingIntent =
          PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_ONE_SHOT);

      builder.addAction(stopIcon, stopText, stopPendingIntent);
    }

    NotificationConstants.setMetadata(context, builder, NotificationConstants.TYPE_FTP);

    return builder;
  }

  public static Notification startNotification(Context context, boolean noStopButton) {
    NotificationCompat.Builder builder =
        buildNotification(
            context,
            R.string.ftp_notif_starting_title,
            context.getString(R.string.ftp_notif_starting),
            noStopButton);

    return builder.build();
  }

  public static void updateNotification(Context context, boolean noStopButton) {
    String notificationService = Context.NOTIFICATION_SERVICE;
    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(notificationService);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    int port = sharedPreferences.getInt(FtpService.PORT_PREFERENCE_KEY, FtpService.DEFAULT_PORT);
    boolean secureConnection =
        sharedPreferences.getBoolean(FtpService.KEY_PREFERENCE_SECURE, FtpService.DEFAULT_SECURE);

    InetAddress address = FtpService.getLocalInetAddress(context);

    String address_text = "Address not found";

    if (address != null) {
      address_text =
          (secureConnection ? FtpService.INITIALS_HOST_SFTP : FtpService.INITIALS_HOST_FTP)
              + address.getHostAddress()
              + ":"
              + port
              + "/";
    }

    NotificationCompat.Builder builder =
        buildNotification(
            context,
            R.string.ftp_notif_title,
            context.getString(R.string.ftp_notif_text, address_text),
            noStopButton);

    notificationManager.notify(NotificationConstants.FTP_ID, builder.build());
  }

  private static void removeNotification(Context context) {
    String ns = Context.NOTIFICATION_SERVICE;
    NotificationManager nm = (NotificationManager) context.getSystemService(ns);
    nm.cancelAll();
  }
}
