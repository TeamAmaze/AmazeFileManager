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

import net.schmizz.sshj.SSHClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SSHClientImpl(private val sshClient: SSHClient) : NetCopyClient<SSHClient> {

    companion object {
        @JvmStatic
        private val logger: Logger = LoggerFactory.getLogger(SSHClientImpl::class.java)
    }

    override fun getClientImpl() = sshClient

    override fun isConnectionValid(): Boolean =
        sshClient.isConnected && sshClient.isAuthenticated

    override fun expire() {
        if (sshClient.isConnected) {
            runCatching {
                sshClient.disconnect()
            }.onFailure {
                logger.warn("Error closing SSHClient connection", it)
            }
        }
    }
}
