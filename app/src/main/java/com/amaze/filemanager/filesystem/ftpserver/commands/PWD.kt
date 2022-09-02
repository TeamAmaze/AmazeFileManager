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

import org.apache.ftpserver.command.AbstractCommand
import org.apache.ftpserver.ftplet.FtpException
import org.apache.ftpserver.ftplet.FtpReply
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.impl.FtpIoSession
import org.apache.ftpserver.impl.FtpServerContext
import org.apache.ftpserver.impl.LocalizedFtpReply
import java.io.IOException

/**
 * Monkey-patch [org.apache.ftpserver.command.impl.PWD] to prevent true path exposed to end user.
 */
class PWD : AbstractCommand() {

    @Throws(IOException::class, FtpException::class)
    override fun execute(
        session: FtpIoSession,
        context: FtpServerContext,
        request: FtpRequest
    ) {
        session.resetState()
        val fsView = session.fileSystemView
        var currDir = fsView.workingDirectory.absolutePath
            .substringAfter(fsView.homeDirectory.absolutePath)
        if (currDir.isEmpty()) {
            currDir = "/"
        }
        if (!currDir.startsWith("/")) {
            currDir = "/$currDir"
        }
        session.write(
            LocalizedFtpReply.translate(
                session,
                request,
                context,
                FtpReply.REPLY_257_PATHNAME_CREATED,
                "PWD",
                currDir
            )
        )
    }
}
