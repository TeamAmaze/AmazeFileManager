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

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.ui.notifications.NotificationConstants.CHANNEL_FTP_ID;
import static com.amaze.filemanager.ui.notifications.NotificationConstants.CHANNEL_NORMAL_ID;
import static com.amaze.filemanager.ui.notifications.NotificationConstants.TYPE_FTP;
import static com.amaze.filemanager.ui.notifications.NotificationConstants.TYPE_NORMAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNotificationManager;

import com.amaze.filemanager.R;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {KITKAT, P})
public class NotificationConstantsTest {

  private Context context;

  private NotificationManager notificationManager;

  private ShadowNotificationManager shadowNotificationManager;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    shadowNotificationManager = shadowOf(notificationManager);
  }

  @After
  public void tearDown() {
    notificationManager.cancelAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMetadataIllegalType() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NORMAL_ID);
    NotificationConstants.setMetadata(context, builder, -1);
    NotificationConstants.setMetadata(context, builder, 2);
    NotificationConstants.setMetadata(context, builder, Integer.MAX_VALUE);
    builder = new NotificationCompat.Builder(context, CHANNEL_FTP_ID);
    NotificationConstants.setMetadata(context, builder, -1);
    NotificationConstants.setMetadata(context, builder, 2);
    NotificationConstants.setMetadata(context, builder, Integer.MAX_VALUE);
  }

  @Test
  @Config(sdk = {KITKAT}) // max sdk is N
  public void testNormalNotification() {
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, CHANNEL_NORMAL_ID)
            .setContentTitle(context.getString(R.string.waiting_title))
            .setContentText(context.getString(R.string.waiting_content))
            .setAutoCancel(false)
            .setSmallIcon(R.drawable.ic_all_inclusive_white_36dp)
            .setProgress(0, 0, true);

    NotificationConstants.setMetadata(context, builder, TYPE_NORMAL);
    Notification result = builder.build();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      assertEquals(Notification.CATEGORY_SERVICE, result.category);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      assertEquals(Notification.PRIORITY_MIN, result.priority);
    } else {
      assertEquals(Notification.PRIORITY_DEFAULT, result.priority);
    }
  }

  @Test
  @Config(sdk = {KITKAT}) // max sdk is N
  public void testFtpNotification() {
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, CHANNEL_FTP_ID)
            .setContentTitle("FTP server test")
            .setContentText("FTP listening at 127.0.0.1:22")
            .setSmallIcon(R.drawable.ic_ftp_light)
            .setTicker(context.getString(R.string.ftp_notif_starting))
            .setOngoing(true)
            .setOnlyAlertOnce(true);
    NotificationConstants.setMetadata(context, builder, TYPE_FTP);
    Notification result = builder.build();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      assertEquals(Notification.CATEGORY_SERVICE, result.category);
      assertEquals(Notification.VISIBILITY_PUBLIC, result.visibility);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      assertEquals(Notification.PRIORITY_MAX, result.priority);
    } else {
      assertEquals(Notification.PRIORITY_DEFAULT, result.priority);
    }
  }

  @Test
  @Config(sdk = {P}) // min sdk is O
  public void testCreateNormalChannel() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NORMAL_ID);
    NotificationConstants.setMetadata(context, builder, TYPE_NORMAL);
    List<Object> channels = shadowNotificationManager.getNotificationChannels();
    assertNotNull(channels);
    assertEquals(1, channels.size());
    NotificationChannel channel = (NotificationChannel) channels.get(0);
    assertEquals(IMPORTANCE_MIN, channel.getImportance());
    assertEquals(CHANNEL_NORMAL_ID, channel.getId());
    assertEquals(context.getString(R.string.channel_name_normal), channel.getName());
    assertEquals(context.getString(R.string.channel_description_normal), channel.getDescription());
  }

  @Test
  @Config(sdk = {P}) // min sdk is O
  public void testCreateFtpChannel() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FTP_ID);
    NotificationConstants.setMetadata(context, builder, TYPE_FTP);
    List<Object> channels = shadowNotificationManager.getNotificationChannels();
    assertNotNull(channels);
    assertEquals(1, channels.size());
    NotificationChannel channel = (NotificationChannel) channels.get(0);
    assertEquals(IMPORTANCE_HIGH, channel.getImportance());
    assertEquals(CHANNEL_FTP_ID, channel.getId());
    assertEquals(context.getString(R.string.channel_name_ftp), channel.getName());
    assertEquals(context.getString(R.string.channel_description_ftp), channel.getDescription());
  }
}
