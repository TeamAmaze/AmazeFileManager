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

import android.os.Environment
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftpserver.AndroidFileSystemFactory
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory
import org.apache.ftpserver.filesystem.nativefs.impl.NativeFileSystemView
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.FileSystemFactory
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.impl.DefaultFtpRequest
import org.apache.ftpserver.impl.FtpIoSession
import org.apache.ftpserver.impl.FtpServerContext
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

/**
 * Unit test for [AVBL].
 */
class AVBLCommandTest : AbstractFtpserverCommandTest() {

    companion object {

        private lateinit var fsFactory: FileSystemFactory

        private lateinit var fsView: NativeFileSystemView

        /**
         * Mock [NativeFileSystemView] for testing.
         */
        @JvmStatic
        @BeforeClass
        fun bootstrap() {
            fsFactory = mock(NativeFileSystemFactory::class.java)
            fsView = mock(NativeFileSystemView::class.java)
            val physicalFile1 = mock(File::class.java)
            val physicalFile2 = mock(File::class.java)
            val physicalFile3 = mock(File::class.java)
            val physicalFile4 = mock(File::class.java)
            val ftpFile1 = mock(FtpFile::class.java)
            val ftpFile2 = mock(FtpFile::class.java)
            val ftpFile3 = mock(FtpFile::class.java)
            val ftpFile4 = mock(FtpFile::class.java)
            `when`(physicalFile1.isDirectory).thenReturn(true)
            `when`(physicalFile2.isDirectory).thenReturn(true)
            `when`(physicalFile3.isDirectory).thenReturn(true)
            `when`(physicalFile4.isDirectory).thenReturn(false)
            `when`(physicalFile4.isFile).thenReturn(true)
            `when`(physicalFile1.freeSpace).thenReturn(12345L)
            `when`(physicalFile2.freeSpace).thenReturn(131072L)
            `when`(physicalFile3.freeSpace).thenThrow(SecurityException())
            `when`(ftpFile1.physicalFile).thenReturn(physicalFile1)
            `when`(ftpFile2.physicalFile).thenReturn(physicalFile2)
            `when`(ftpFile3.physicalFile).thenReturn(physicalFile3)
            `when`(ftpFile4.physicalFile).thenReturn(physicalFile4)
            `when`(fsView.homeDirectory).thenReturn(ftpFile1)
            `when`(fsView.getFile(anyString())).thenReturn(null)
            `when`(fsView.getFile("/")).thenReturn(ftpFile1)
            `when`(fsView.getFile("/incoming")).thenReturn(ftpFile2)
            `when`(fsView.getFile("/secure")).thenReturn(ftpFile3)
            `when`(fsView.getFile("/test.txt")).thenReturn(ftpFile4)
            `when`(fsFactory.createFileSystemView(any())).thenReturn(fsView)
        }
    }

    /**
     * Command should return 502 not implemented if FTP server is using [AndroidFileSystemFactory].
     */
    @Test
    fun testWithAndroidFileSystem() {
        executeRequest(
            "AVBL",
            listOf(WritePermission()),
            mock(AndroidFileSystemFactory::class.java)
        )
        assertEquals(1, logger.messages.size)
        assertEquals(502, logger.messages[0].code)
        assertEquals(
            AppConfig.getInstance().getString(R.string.ftp_error_AVBL_notimplemented),
            logger.messages[0].message
        )
    }

    /**
     * No path argument should return home directory size.
     */
    @Test
    fun testHomeDirectory() {
        executeRequest("AVBL", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(213, logger.messages[0].code)
        assertEquals("12345", logger.messages[0].message)
    }

    /**
     * Root (/) argument test.
     */
    @Test
    fun testRoot() {
        executeRequest("AVBL /", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(213, logger.messages[0].code)
        assertEquals("12345", logger.messages[0].message)
    }

    /**
     * Test specified path.
     */
    @Test
    fun testGetPath() {
        executeRequest("AVBL /incoming", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(213, logger.messages[0].code)
        assertEquals("131072", logger.messages[0].message)
    }

    /**
     * Command should return 550 if path not found.
     */
    @Test
    fun testPathNotFound() {
        executeRequest("AVBL /foobar", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(550, logger.messages[0].code)
        assertEquals(
            AppConfig.getInstance().getString(R.string.ftp_error_AVBL_missing),
            logger.messages[0].message
        )
    }

    /**
     * Command should return 550 too if user does not have access to directory.
     */
    @Test
    fun testAccessDenied() {
        executeRequest("AVBL /secure", emptyList())
        assertEquals(1, logger.messages.size)
        assertEquals(550, logger.messages[0].code)
        assertEquals(
            AppConfig.getInstance().getString(R.string.ftp_error_AVBL_accessdenied),
            logger.messages[0].message
        )
    }

    /**
     * Command should return 550 if [SecurityException] was thrown when calling
     * [File.getFreeSpace].
     */
    @Test
    fun testSecurityException() {
        executeRequest("AVBL /secure", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(550, logger.messages[0].code)
        assertEquals(
            AppConfig.getInstance().getString(R.string.ftp_error_AVBL_accessdenied),
            logger.messages[0].message
        )
    }

    /**
     * Command should return 550 if user tried to get free space from a file.
     */
    @Test
    fun testFile() {
        executeRequest("AVBL /test.txt", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(550, logger.messages[0].code)
        assertEquals(
            AppConfig.getInstance().getString(R.string.ftp_error_AVBL_isafile),
            logger.messages[0].message
        )
    }

    private fun executeRequest(
        commandLine: String,
        permissions: List<Authority>,
        fileSystemFactory: FileSystemFactory = fsFactory
    ) {
        val context = mock(FtpServerContext::class.java)
        val ftpSession = FtpIoSession(session, context)
        ftpSession.user = BaseUser().also {
            it.homeDirectory = Environment.getExternalStorageDirectory().absolutePath
            it.authorities = permissions
        }
        ftpSession.setLogin(fsView)
        `when`(context.fileSystemManager).thenReturn(fileSystemFactory)
        val command = AVBL()
        command.execute(
            session = ftpSession,
            context = context,
            request = DefaultFtpRequest(commandLine)
        )
    }
}
