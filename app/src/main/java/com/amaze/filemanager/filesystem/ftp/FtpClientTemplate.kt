/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.ftp

import org.apache.commons.net.ftp.FTPClient
import java.io.IOException

/**
 * Template class for executing actions with [NetCopyClient] while leave the complexities of
 * handling connection setup/teardown to [NetCopyClientUtils].
 */
abstract class FtpClientTemplate<T>(url: String, closeClientOnFinish: Boolean = true) :
    NetCopyClientTemplate<FTPClient, T>(url, closeClientOnFinish) {

    @Throws(IOException::class)
    final override fun execute(client: NetCopyClient<FTPClient>): T? {
        val ftpClient: FTPClient = client.getClientImpl()
        return executeWithFtpClient(ftpClient)
    }

    /**
     * Implement logic here.
     *
     * @param client [FTPClient] instance, with connection opened and authenticated
     * @param <T> Requested return type
     * @return Result of the execution of the type requested </T>
     **/
    @Throws(IOException::class)
    abstract fun executeWithFtpClient(ftpClient: FTPClient): T?
}
