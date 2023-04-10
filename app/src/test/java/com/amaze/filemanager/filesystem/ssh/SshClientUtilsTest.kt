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

package com.amaze.filemanager.filesystem.ssh

import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.deriveUriFrom
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.io.IOException

/**
 * Unit tests for [SshClientUtils].
 */
class SshClientUtilsTest {

    companion object {
        private const val SYSROOT_ETC = "/sysroot/etc"
    }

    /**
     * Test [NetCopyClientUtils.deriveUriFrom].
     */
    @Test
    fun testDeriveSftpPathFrom() {
        assertEquals(
            "ssh://root:toor@127.0.0.1:22/",
            deriveUriFrom(
                SSH_URI_PREFIX,
                "127.0.0.1",
                22,
                null,
                "root",
                "toor"
            )
        )
        assertEquals(
            "ssh://root:toor@127.0.0.1:22",
            deriveUriFrom(
                SSH_URI_PREFIX,
                "127.0.0.1",
                22,
                "",
                "root",
                "toor"
            )
        )
    }

    /**
     * Tests [SshClientUtils.isDirectory] for normal cases.
     */
    @Test
    @Throws(IOException::class)
    fun testIsDirectoryNormal() {
        val mock = Mockito.mock(RemoteResourceInfo::class.java)
        `when`(mock.isDirectory).thenReturn(true)
        val mockAttributes = FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build()
        `when`(mock.attributes).thenReturn(mockAttributes)
        val mockClient = Mockito.mock(SFTPClient::class.java)
        assertTrue(SshClientUtils.isDirectory(mockClient, mock))
    }

    /**
     * Tests [SshClientUtils.isDirectory] with a file.
     */
    @Test
    @Throws(IOException::class)
    fun testIsDirectoryWithFile() {
        val mock = Mockito.mock(RemoteResourceInfo::class.java)
        `when`(mock.isDirectory).thenReturn(false)
        val mockAttributes = FileAttributes.Builder().withType(FileMode.Type.REGULAR).build()
        `when`(mock.attributes).thenReturn(mockAttributes)
        val mockClient = Mockito.mock(SFTPClient::class.java)
        assertFalse(SshClientUtils.isDirectory(mockClient, mock))
    }

    /**
     * Tests [SshClientUtils.isDirectory] for symlinks to directory.
     */
    @Test
    @Throws(IOException::class)
    fun testIsDirectorySymlinkNormal() {
        val mock = Mockito.mock(RemoteResourceInfo::class.java)
        `when`(mock.path).thenReturn(SYSROOT_ETC)
        `when`(mock.isDirectory).thenReturn(true)
        var mockAttributes = FileAttributes.Builder().withType(FileMode.Type.SYMLINK).build()
        `when`(mock.attributes).thenReturn(mockAttributes)
        val mockClient = Mockito.mock(SFTPClient::class.java)
        mockAttributes = FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build()
        `when`(mockClient.stat(SYSROOT_ETC)).thenReturn(mockAttributes)
        assertTrue(SshClientUtils.isDirectory(mockClient, mock))
    }

    /**
     * Tests [SshClientUtils.isDirectory] for broken symlinks.
     */
    @Test
    @Throws(IOException::class)
    fun testIsDirectorySymlinkBrokenDirectory() {
        val mock = Mockito.mock(RemoteResourceInfo::class.java)
        `when`(mock.path).thenReturn(SYSROOT_ETC)
        `when`(mock.isDirectory).thenReturn(true)
        var mockAttributes = FileAttributes.Builder().withType(FileMode.Type.SYMLINK).build()
        `when`(mock.attributes).thenReturn(mockAttributes)
        val mockClient = Mockito.mock(SFTPClient::class.java)
        mockAttributes = FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build()
        `when`(mockClient.stat(SYSROOT_ETC)).thenThrow(IOException())
        assertThrows(IOException::class.java) {
            SshClientUtils.isDirectory(
                mockClient,
                mock
            )
        }
    }

    /**
     * Tests [SshClientUtils.isDirectory] for broken symlibks to file.
     */
    @Test
    @Throws(IOException::class)
    fun testIsDirectorySymlinkBrokenFile() {
        val mock = Mockito.mock(RemoteResourceInfo::class.java)
        `when`(mock.path).thenReturn(SYSROOT_ETC)
        `when`(mock.isDirectory).thenReturn(false)
        var mockAttributes = FileAttributes.Builder().withType(FileMode.Type.SYMLINK).build()
        `when`(mock.attributes).thenReturn(mockAttributes)
        val mockClient = Mockito.mock(SFTPClient::class.java)
        mockAttributes = FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build()
        `when`(mockClient.stat(SYSROOT_ETC)).thenThrow(IOException())
        assertThrows(IOException::class.java) {
            SshClientUtils.isDirectory(
                mockClient,
                mock
            )
        }
    }
}
