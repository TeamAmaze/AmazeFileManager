/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.ftpserver.commands

import org.apache.ftpserver.ftplet.FtpReply
import org.apache.mina.core.filterchain.IoFilter
import org.apache.mina.core.filterchain.IoFilterAdapter
import org.apache.mina.core.session.IoSession
import org.apache.mina.core.write.WriteRequest

/**
 * [IoFilter] to store messages written in an [IoSession]. Test use only.
 */
class LogMessageFilter : IoFilterAdapter() {
    val messages = mutableListOf<FtpReply>()

    override fun messageSent(
        nextFilter: IoFilter.NextFilter,
        session: IoSession,
        writeRequest: WriteRequest
    ) {
        writeRequest.message.run {
            messages.add(this as FtpReply)
        }
        super.messageSent(nextFilter, session, writeRequest)
    }

    /**
     * Expunge all messages sent to this filter.
     */
    fun reset() {
        messages.clear()
    }
}
