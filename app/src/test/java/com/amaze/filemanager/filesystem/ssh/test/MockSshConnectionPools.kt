/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.ssh.test

import com.amaze.filemanager.filesystem.ssh.SshConnectionPool
import net.schmizz.sshj.Config
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPException
import org.mockito.Mockito.*

object MockSshConnectionPools {

    private const val ACCESS_DENIED = "Access is denied."

    private val hostKeyPair = TestUtils.createKeyPair()

    private val userKeyPair = TestUtils.createKeyPair()

    fun prepareCanDeleteScenario() {
        doPrepareSSHClientInternal(true)
    }

    fun prepareCannotDeleteScenario() {
        doPrepareSSHClientInternal(false)
    }

    // Yes, idiot hardcoded paths. Shall expand as more test cases arrive.
    private fun doPrepareSSHClientInternal(canDelete: Boolean) {
        TestUtils.saveSshConnectionSettings(hostKeyPair, "user", "password", userKeyPair.private)

        val fileAttributes = mock(FileAttributes::class.java).apply {
            `when`(type).thenReturn(FileMode.Type.DIRECTORY)
        }
        val sftpClient = mock(SFTPClient::class.java).apply {
            doThrow(SFTPException(ACCESS_DENIED))
                .`when`(this).rename("/tmp/old.file", "/tmp/new.file")
            `when`(stat("/tmp/old.file")).thenReturn(fileAttributes)
            `when`(stat("/tmp/new.file")).thenReturn(null)
            val fa = mock(FileAttributes::class.java).apply {
                `when`(type).thenReturn(FileMode.Type.REGULAR)
            }
            `when`(stat("/test.file")).thenReturn(fa)

            if (canDelete) {
                doNothing().`when`(this).rm(anyString())
                doNothing().`when`(this).rmdir(anyString())
            } else {
                `when`(rm(anyString())).thenThrow(SFTPException(ACCESS_DENIED))
                `when`(rmdir(anyString())).thenThrow(SFTPException(ACCESS_DENIED))
            }
        }
        val sshClient = mock(SSHClient::class.java).apply {
            doNothing().`when`(this).addHostKeyVerifier(anyString())
            doNothing().`when`(this).connect(anyString(), anyInt())
            doNothing().`when`(this).authPassword(anyString(), anyString())
            doNothing().`when`(this).disconnect()
            `when`(isConnected).thenReturn(true)
            `when`(isAuthenticated).thenReturn(true)
            `when`(newSFTPClient()).thenReturn(sftpClient)
        }

        /*
         * We don't need to go through authentication flow here, and in fact SshAuthenticationTask
         * was not working in the case of Operations.rename() due to the threading model
         * Robolectric imposed. So we are injecting the SSHClient here by force.
         */
        SshConnectionPool::class.java.getDeclaredField("connections").run {
            this.isAccessible = true
            this.set(
                SshConnectionPool,
                mutableMapOf(
                    Pair<String, SSHClient>("ssh://user:password@127.0.0.1:22222", sshClient)
                )
            )
        }

        SshConnectionPool.sshClientFactory = object : SshConnectionPool.SSHClientFactory {
            override fun create(config: Config?): SSHClient = sshClient
        }
    }
}
