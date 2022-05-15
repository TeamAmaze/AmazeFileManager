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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.services.ftp.FtpService
import com.amaze.filemanager.asynchronous.services.ftp.FtpService.Companion.getLocalInetAddress
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.notifications.NotificationConstants.setMetadata

/**
 * Created by yashwanthreddyg on 19-06-2016.
 * Edited by zent-co on 30-07-2019.
 * Re-edited by hojat72elect on 14-05-2022.
 */
object FtpNotification {
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotification(
        context: Context,
        @StringRes contentTitleRes: Int,
        contentText: String,
        noStopButton: Boolean
    ): NotificationCompat.Builder {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val currentTime = System.currentTimeMillis()
        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_FTP_ID)
            .setContentTitle(context.getString(contentTitleRes))
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_ftp_light)
            .setTicker(context.getString(R.string.ftp_notif_starting))
            .setWhen(currentTime)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (!noStopButton) {
            val stopIcon = android.R.drawable.ic_menu_close_clear_cancel
            val stopText: CharSequence = context.getString(R.string.ftp_notif_stop_server)
            val stopIntent =
                Intent(FtpService.ACTION_STOP_FTPSERVER).setPackage(context.packageName)
            val stopPendingIntent =
                PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_ONE_SHOT)
            builder.addAction(stopIcon, stopText, stopPendingIntent)
        }
        setMetadata(context, builder, NotificationConstants.TYPE_FTP)
        return builder
    }

    fun startNotification(context: Context, noStopButton: Boolean): Notification {
        val builder = buildNotification(
            context,
            R.string.ftp_notif_starting_title,
            context.getString(R.string.ftp_notif_starting),
            noStopButton
        )
        return builder.build()
    }

    fun updateNotification(context: Context, noStopButton: Boolean) {
        val notificationService = Context.NOTIFICATION_SERVICE
        val notificationManager =
            context.getSystemService(notificationService) as NotificationManager
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val port = sharedPreferences.getInt(FtpService.PORT_PREFERENCE_KEY, FtpService.DEFAULT_PORT)
        val secureConnection = sharedPreferences.getBoolean(
            FtpService.KEY_PREFERENCE_SECURE,
            FtpService.DEFAULT_SECURE
        )
        val address = getLocalInetAddress(context)
        var address_text = "Address not found"
        if (address != null) {
            address_text =
                (
                    (
                        if (secureConnection) FtpService.INITIALS_HOST_SFTP
                        else FtpService.INITIALS_HOST_FTP
                        ) +
                        address.hostAddress +
                        ":" +
                        port +
                        "/"
                    )
        }
        val builder = buildNotification(
            context,
            R.string.ftp_notif_title,
            context.getString(R.string.ftp_notif_text, address_text),
            noStopButton
        )
        notificationManager.notify(NotificationConstants.FTP_ID, builder.build())
    }

    private fun removeNotification(context: Context) {
        val ns = Context.NOTIFICATION_SERVICE
        val nm = context.getSystemService(ns) as NotificationManager
        nm.cancelAll()
    }
}
