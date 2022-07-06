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

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftpserver.AndroidFileSystemFactory
import org.apache.ftpserver.command.AbstractCommand
import org.apache.ftpserver.ftplet.DefaultFtpReply
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.FtpReply.REPLY_213_FILE_STATUS
import org.apache.ftpserver.ftplet.FtpReply.REPLY_502_COMMAND_NOT_IMPLEMENTED
import org.apache.ftpserver.ftplet.FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.impl.FtpIoSession
import org.apache.ftpserver.impl.FtpServerContext
import org.apache.ftpserver.usermanager.impl.WriteRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Implements FTP extension AVBL command, to answer device remaining space in FTP command.
 *
 * Only supports [com.amaze.filemanager.filesystem.ftpserver.RootFileSystemFactory] and
 * [org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory]. Otherwise will simply return
 * 550 Access Denied.
 *
 * See [Draft spec](https://www.ietf.org/archive/id/draft-peterson-streamlined-ftp-command-extensions-10.txt)
 */
class AVBL : AbstractCommand() {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(AVBL::class.java)
    }

    override fun execute(session: FtpIoSession, context: FtpServerContext, request: FtpRequest) {
        // argument check
        val fileName: String? = request.argument
        if (context.fileSystemManager is AndroidFileSystemFactory) {
            doWriteReply(
                session,
                REPLY_502_COMMAND_NOT_IMPLEMENTED,
                "AVBL.notimplemented"
            )
        } else {
            val ftpFile: FtpFile? = if (true == fileName?.isNotBlank()) {
                runCatching {
                    session.fileSystemView.getFile(fileName)
                }.getOrNull()
            } else {
                session.fileSystemView.homeDirectory
            }
            if (ftpFile != null) {
                if (session.user.authorize(
                        if (true == fileName?.isNotBlank()) {
                            WriteRequest(fileName)
                        } else {
                            WriteRequest()
                        }
                    ) != null ||
                    !(ftpFile.physicalFile as File).canWrite()
                ) {
                    (ftpFile.physicalFile as File).apply {
                        if (this.isDirectory) {
                            runCatching {
                                freeSpace.let {
                                    session.write(
                                        DefaultFtpReply(REPLY_213_FILE_STATUS, it.toString())
                                    )
                                }
                            }.onFailure {
                                LOG.error("Error getting directory free space", it)
                                replyError(session, "AVBL.accessdenied")
                                return
                            }
                        } else {
                            replyError(session, "AVBL.isafile")
                        }
                    }
                } else {
                    replyError(session, "AVBL.accessdenied")
                }
            } else {
                replyError(session, "AVBL.missing", fileName)
            }
        }
    }

    private fun replyError(
        session: FtpIoSession,
        subId: String,
        fileName: String? = null
    ) = doWriteReply(session, REPLY_550_REQUESTED_ACTION_NOT_TAKEN, subId, fileName)

    private fun doWriteReply(
        session: FtpIoSession,
        code: Int,
        subId: String,
        fileName: String? = null
    ) {
        val packageName = AppConfig.getInstance().packageName
        val resources = AppConfig.getInstance().resources
        session.write(
            DefaultFtpReply(
                code,
                resources.getString(
                    resources.getIdentifier("$packageName:string/ftp_error_$subId", null, null),
                    fileName
                )
            )
        )
    }
}
