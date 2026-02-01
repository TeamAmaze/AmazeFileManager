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

package com.amaze.filemanager.server

import android.app.Notification
import android.content.Context

/**
 * Interface for server notification management.
 *
 * Each server implementation should provide its own notification handler
 * to show server status in the notification bar.
 */
interface ServerNotification {
    /**
     * Get the notification ID for this server
     */
    fun getNotificationId(): Int

    /**
     * Get the notification channel ID for this server
     */
    fun getChannelId(): String

    /**
     * Create the initial notification when server is starting
     * @param context Application context
     * @param noStopButton Whether to hide the stop button (e.g., when started from tile)
     * @return Notification to display
     */
    fun createStartingNotification(
        context: Context,
        noStopButton: Boolean = false,
    ): Notification

    /**
     * Update the notification when server is running
     * @param context Application context
     * @param noStopButton Whether to hide the stop button
     */
    fun updateRunningNotification(
        context: Context,
        noStopButton: Boolean = false,
    )

    /**
     * Remove the notification
     * @param context Application context
     */
    fun removeNotification(context: Context)
}
