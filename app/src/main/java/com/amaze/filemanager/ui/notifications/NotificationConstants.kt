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

package com.amaze.filemanager.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.amaze.filemanager.R

/**
 * @author Emmanuel Messulam <emmanuelbendavid></emmanuelbendavid>@gmail.com> on 17/9/2017, at 13:34.
 */
object NotificationConstants {
    const val COPY_ID = 0
    const val EXTRACT_ID = 1
    const val ZIP_ID = 2
    const val DECRYPT_ID = 3
    const val ENCRYPT_ID = 4
    const val FTP_ID = 5
    const val FAILED_ID = 6
    const val WAIT_ID = 7

    const val TYPE_NORMAL = 0
    const val TYPE_FTP = 1

    const val CHANNEL_NORMAL_ID = "normalChannel"
    const val CHANNEL_FTP_ID = "ftpChannel"

    /**
     * This creates a channel (API >= 26) or applies the correct metadata to a notification (API < 26)
     */
    @JvmStatic
    fun setMetadata(
        context: Context,
        notification: NotificationCompat.Builder?,
        type: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (type) {
                TYPE_NORMAL -> createNormalChannel(context)
                TYPE_FTP -> createFtpChannel(context)
                else -> throw IllegalArgumentException("Unrecognized type:$type")
            }
        } else {
            when (type) {
                TYPE_NORMAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        notification?.setCategory(Notification.CATEGORY_SERVICE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        notification?.priority = Notification.PRIORITY_MIN
                    }
                }
                TYPE_FTP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        notification?.setCategory(Notification.CATEGORY_SERVICE)
                        notification?.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        notification?.priority = Notification.PRIORITY_MAX
                    }
                }
                else -> throw IllegalArgumentException("Unrecognized type:$type")
            }
        }
    }

    /**
     * You CANNOT call this from  API< 26. THis channel is set so it doesn't bother the user, but
     * it has importance.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createFtpChannel(context: Context) {
        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mNotificationManager.getNotificationChannel(CHANNEL_FTP_ID) == null) {
            val mChannel = NotificationChannel(
                CHANNEL_FTP_ID,
                context.getString(R.string.channel_name_ftp),
                NotificationManager.IMPORTANCE_HIGH
            )
            // Configure the notification channel.
            mChannel.description = context.getString(R.string.channel_description_ftp)
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }

    /**
     * You CANNOT call this from API< 26. This channel is set so it doesn't bother the user, with
     * the lowest importance.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNormalChannel(context: Context) {
        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mNotificationManager.getNotificationChannel(CHANNEL_NORMAL_ID) == null) {
            val mChannel = NotificationChannel(
                CHANNEL_NORMAL_ID,
                context.getString(R.string.channel_name_normal),
                NotificationManager.IMPORTANCE_MIN
            )
            // Configure the notification channel.
            mChannel.description = context.getString(R.string.channel_description_normal)
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }
}
