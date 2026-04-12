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

package com.amaze.filemanager.ftpserver.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.amaze.filemanager.ftpserver.R
import com.amaze.filemanager.ftpserver.service.FtpPreferences
import com.amaze.filemanager.server.ServerNotification

/**
 * Handles FTP server notifications.
 */
class FtpServerNotification(
    private val notificationId: Int,
    private val channelId: String,
    private val mainActivityIntent: Intent,
    private val getLocalAddress: (Context) -> String?,
) : ServerNotification {
    override fun getNotificationId(): Int = notificationId

    override fun getChannelId(): String = channelId

    override fun createStartingNotification(
        context: Context,
        noStopButton: Boolean,
    ): Notification {
        ensureNotificationChannel(context)
        return buildNotification(
            context,
            R.string.ftpmod_notification_title,
            context.getString(R.string.ftpmod_notification_starting),
            noStopButton,
        ).build()
    }

    override fun updateRunningNotification(
        context: Context,
        noStopButton: Boolean,
    ) {
        ensureNotificationChannel(context)
        val notificationManager = NotificationManagerCompat.from(context)

        val port = FtpPreferences.getPort(context)
        val secureConnection = FtpPreferences.isSecure(context)

        val address = getLocalAddress(context)
        val addressText =
            if (address != null) {
                val prefix =
                    if (secureConnection) {
                        FtpPreferences.INITIALS_HOST_SFTP
                    } else {
                        FtpPreferences.INITIALS_HOST_FTP
                    }
                "$prefix$address:$port/"
            } else {
                context.getString(R.string.ftpmod_notification_error_address_not_found)
            }

        val notification =
            buildNotification(
                context,
                R.string.ftpmod_notification_running_title,
                context.getString(R.string.ftpmod_notification_running_text, addressText),
                noStopButton,
            ).build()

        notificationManager.notify(notificationId, notification)
    }

    override fun removeNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun buildNotification(
        context: Context,
        titleResId: Int,
        contentText: String,
        noStopButton: Boolean,
    ): NotificationCompat.Builder {
        val contentIntent =
            PendingIntent.getActivity(
                context,
                0,
                mainActivityIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                getPendingIntentFlag(0),
            )

        val builder =
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(titleResId))
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_ftp_light)
                .setTicker(context.getString(R.string.ftpmod_notification_starting))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setOnlyAlertOnce(true)

        if (!noStopButton) {
            val stopIntent =
                Intent(FtpPreferences.ACTION_STOP_FTPSERVER)
                    .setPackage(context.packageName)
            val stopPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    stopIntent,
                    getPendingIntentFlag(PendingIntent.FLAG_ONE_SHOT),
                )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.ftpmod_notification_stop_server),
                stopPendingIntent,
            )
        }

        return builder
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    context.getString(R.string.ftpmod_notification_title),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.ftpmod_notification_channel_desc)
                    setShowBadge(false)
                }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPendingIntentFlag(baseFlag: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            baseFlag or PendingIntent.FLAG_IMMUTABLE
        } else {
            baseFlag
        }
    }
}
