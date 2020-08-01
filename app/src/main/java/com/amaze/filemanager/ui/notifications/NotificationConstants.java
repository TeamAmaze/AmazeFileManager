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

import com.amaze.filemanager.R;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 17/9/2017, at 13:34. */
public class NotificationConstants {

  public static final int COPY_ID = 0;
  public static final int EXTRACT_ID = 1;
  public static final int ZIP_ID = 2;
  public static final int DECRYPT_ID = 3;
  public static final int ENCRYPT_ID = 4;
  public static final int FTP_ID = 5;
  public static final int FAILED_ID = 6;
  public static final int WAIT_ID = 7;

  public static final int TYPE_NORMAL = 0, TYPE_FTP = 1;

  public static final String CHANNEL_NORMAL_ID = "normalChannel";
  public static final String CHANNEL_FTP_ID = "ftpChannel";

  /**
   * This creates a channel (API >= 26) or applies the correct metadata to a notification (API < 26)
   */
  public static void setMetadata(
      Context context, NotificationCompat.Builder notification, int type) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      switch (type) {
        case TYPE_NORMAL:
          createNormalChannel(context);
          break;
        case TYPE_FTP:
          createFtpChannel(context);
          break;
        default:
          throw new IllegalArgumentException("Unrecognized type:" + type);
      }
    } else {
      switch (type) {
        case TYPE_NORMAL:
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setCategory(Notification.CATEGORY_SERVICE);
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.setPriority(Notification.PRIORITY_MIN);
          }
          break;
        case TYPE_FTP:
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setCategory(Notification.CATEGORY_SERVICE);
            notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.setPriority(Notification.PRIORITY_MAX);
          }
          break;
        default:
          throw new IllegalArgumentException("Unrecognized type:" + type);
      }
    }
  }

  /**
   * You CANNOT call this from android < O. THis channel is set so it doesn't bother the user, but
   * it has importance.
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private static void createFtpChannel(Context context) {
    NotificationManager mNotificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (mNotificationManager.getNotificationChannel(CHANNEL_FTP_ID) == null) {
      NotificationChannel mChannel =
          new NotificationChannel(
              CHANNEL_FTP_ID,
              context.getString(R.string.channel_name_ftp),
              NotificationManager.IMPORTANCE_HIGH);
      // Configure the notification channel.
      mChannel.setDescription(context.getString(R.string.channel_description_ftp));
      mNotificationManager.createNotificationChannel(mChannel);
    }
  }

  /**
   * You CANNOT call this from android < O. THis channel is set so it doesn't bother the user, with
   * the lowest importance.
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private static void createNormalChannel(Context context) {
    NotificationManager mNotificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (mNotificationManager.getNotificationChannel(CHANNEL_NORMAL_ID) == null) {
      NotificationChannel mChannel =
          new NotificationChannel(
              CHANNEL_NORMAL_ID,
              context.getString(R.string.channel_name_normal),
              NotificationManager.IMPORTANCE_MIN);
      // Configure the notification channel.
      mChannel.setDescription(context.getString(R.string.channel_description_normal));
      mNotificationManager.createNotificationChannel(mChannel);
    }
  }
}
