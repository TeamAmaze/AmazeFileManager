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

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Template class for executing actions with [SFTPClient] while leave the complexities of
 * handling connection and session setup/teardown to [SshClientUtils].
 */
abstract class SFtpClientTemplate<T>(url: String, closeClientOnFinish: Boolean = true) :
    SshClientTemplate<T>(url, closeClientOnFinish) {

    private val LOG: Logger = LoggerFactory.getLogger(javaClass)

    override fun executeWithSSHClient(sshClient: SSHClient): T? {
        var sftpClient: SFTPClient? = null
        var retval: T? = null
        try {
            sftpClient = sshClient.newSFTPClient()
            retval = execute(sftpClient)
        } catch (e: IOException) {
            LOG.error("Error executing template method", e)
        } finally {
            if (sftpClient != null && closeClientOnFinish) {
                try {
                    sftpClient.close()
                } catch (e: IOException) {
                    LOG.warn("Error closing SFTP client", e)
                }
            }
        }
        return retval
    }

    /**
     * Implement logic here.
     *
     * @param client [SFTPClient] instance, with connection opened and authenticated, and SSH
     * session had been set up.
     * @param <T> Requested return type
     * @return Result of the execution of the type requested
     */
    @Throws(IOException::class)
    abstract fun execute(client: SFTPClient): T?
}
