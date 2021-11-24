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

package com.amaze.filemanager.filesystem.ssh

import com.amaze.filemanager.filesystem.ftp.NetCopyClient
import com.amaze.filemanager.filesystem.ftp.NetCopyClientTemplate
import net.schmizz.sshj.SSHClient
import java.io.IOException

/**
 * Template class for executing actions with [SSHClient] while leave the complexities of
 * handling connection setup/teardown to [SshClientUtils].
 */
abstract class SshClientTemplate<T>(url: String, closeClientOnFinish: Boolean = true) :
    NetCopyClientTemplate<SSHClient, T>(url, closeClientOnFinish) {

    @Throws(IOException::class)
    final override fun execute(client: NetCopyClient<SSHClient>): T? {
        val sshClient: SSHClient = client.getClientImpl()
        return executeWithSSHClient(sshClient)
    }

    /**
     * Implement logic here.
     *
     * @param client [SSHClient] instance, with connection opened and authenticated
     * @param <T> Requested return type
     * @return Result of the execution of the type requested
     </T> */
    @Throws(IOException::class)
    abstract fun executeWithSSHClient(client: SSHClient): T?
}
