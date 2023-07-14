/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.ssh

import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.PacketType
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPEngine
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.TimeUnit

const val READ_AHEAD_MAX_UNCONFIRMED_READS: Int = 16

/**
 * Monkey-patch [SFTPEngine.open] until sshj adds back read ahead support in [RemoteFile].
 */
@Throws(IOException::class)
fun SFTPEngine.openWithReadAheadSupport(
    path: String,
    modes: Set<OpenMode>,
    fa: FileAttributes
): RemoteFile {
    val handle: ByteArray = request(
        newRequest(PacketType.OPEN).putString(path, subsystem.remoteCharset)
            .putUInt32(OpenMode.toMask(modes).toLong()).putFileAttributes(fa)
    ).retrieve(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .ensurePacketTypeIs(PacketType.HANDLE).readBytes()
    return RemoteFile(this, path, handle)
}

/**
 * Monkey-patch [SFTPClient.open] until sshj adds back read ahead support in [RemoteFile].
 */
fun SFTPClient.openWithReadAheadSupport(path: String): RemoteFile {
    return sftpEngine.openWithReadAheadSupport(
        path,
        EnumSet.of(OpenMode.READ),
        FileAttributes.EMPTY
    )
}
