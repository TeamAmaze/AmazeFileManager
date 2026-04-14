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

package com.amaze.filemanager.ui.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.amaze.filemanager.server.ServerNotification

/**
 * Adapter that bridges [ServerNotification] to the existing [FtpNotification] static methods.
 *
 * This allows [FtpServerProvider][com.amaze.filemanager.ftpserver.FtpServerProvider] to be wired
 * into [ServerRegistry][com.amaze.filemanager.server.ServerRegistry] without replacing the
 * existing notification infrastructure. In a future consolidation step this adapter can be removed
 * in favour of [FtpServerNotification][com.amaze.filemanager.ftpserver.ui.FtpServerNotification]
 * taking over entirely (Option A).
 */
class FtpNotificationAdapter : ServerNotification {
    override fun getNotificationId(): Int = NotificationConstants.FTP_ID

    override fun getChannelId(): String = NotificationConstants.CHANNEL_FTP_ID

    override fun createStartingNotification(
        context: Context,
        noStopButton: Boolean,
    ): Notification = FtpNotification.startNotification(context, noStopButton)

    override fun updateRunningNotification(
        context: Context,
        noStopButton: Boolean,
    ) = FtpNotification.updateNotification(context, noStopButton)

    override fun removeNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NotificationConstants.FTP_ID)
    }
}
