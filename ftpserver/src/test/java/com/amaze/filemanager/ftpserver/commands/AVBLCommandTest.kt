/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ftpserver.commands

import com.amaze.filemanager.ftpserver.filesystem.AndroidFileSystemFactory
import io.mockk.every
import io.mockk.mockk
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

        // Test error messages
        private const val ERROR_NOT_IMPLEMENTED = "Command not implemented for this filesystem"
        private const val ERROR_ACCESS_DENIED = "Access denied"
        private const val ERROR_IS_A_FILE = "Path is a file, not a directory"
        private const val ERROR_MISSING = "Path not found: %s"

        private val errorMessageProvider =
            object : AVBL.ErrorMessageProvider {
                override fun getErrorMessage(
                    subId: String,
                    fileName: String?,
                ): String {
                    return when (subId) {
                        "AVBL.notimplemented" -> ERROR_NOT_IMPLEMENTED
                        "AVBL.accessdenied" -> ERROR_ACCESS_DENIED
                        "AVBL.isafile" -> ERROR_IS_A_FILE
                        "AVBL.missing" -> String.format(ERROR_MISSING, fileName ?: "")
                        else -> "Unknown error"
                    }
                }
            }

        /**
         * Mock [NativeFileSystemView] for testing.
         * Uses Mockito for java.io.File mocks (better support for final classes)
         * and MockK for FTP-specific mocks.
         */
        @JvmStatic
        @BeforeClass
        fun bootstrap() {
            // Use Mockito for File mocks (handles final classes better)
            val physicalFile1 = mock(File::class.java)
            `when`(physicalFile1.isDirectory).thenReturn(true)
            `when`(physicalFile1.freeSpace).thenReturn(12345L)
            `when`(physicalFile1.canWrite()).thenReturn(true)

            val physicalFile2 = mock(File::class.java)
            `when`(physicalFile2.isDirectory).thenReturn(true)
            `when`(physicalFile2.freeSpace).thenReturn(131072L)
            `when`(physicalFile2.canWrite()).thenReturn(true)

            val physicalFile3 = mock(File::class.java)
            `when`(physicalFile3.isDirectory).thenReturn(true)
            `when`(physicalFile3.freeSpace).thenThrow(SecurityException())
            `when`(physicalFile3.canWrite()).thenReturn(false)

            val physicalFile4 = mock(File::class.java)
            `when`(physicalFile4.isDirectory).thenReturn(false)
            `when`(physicalFile4.isFile).thenReturn(true)
            `when`(physicalFile4.canWrite()).thenReturn(true)

            val ftpFile1 =
                mockk<FtpFile> {
                    every { physicalFile } returns physicalFile1
                }
            val ftpFile2 =
                mockk<FtpFile> {
                    every { physicalFile } returns physicalFile2
                }
            val ftpFile3 =
                mockk<FtpFile> {
                    every { physicalFile } returns physicalFile3
                }
            val ftpFile4 =
                mockk<FtpFile> {
                    every { physicalFile } returns physicalFile4
                }

            fsView =
                mockk {
                    every { homeDirectory } returns ftpFile1
                    every { getFile(any()) } returns null
                    every { getFile("/") } returns ftpFile1
                    every { getFile("/incoming") } returns ftpFile2
                    every { getFile("/secure") } returns ftpFile3
                    every { getFile("/test.txt") } returns ftpFile4
                }

            fsFactory =
                mockk<NativeFileSystemFactory> {
                    every { createFileSystemView(any()) } returns fsView
                }
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
            mockk<AndroidFileSystemFactory>(),
        )
        assertEquals(1, logger.messages.size)
        assertEquals(502, logger.messages[0].code)
        assertEquals(ERROR_NOT_IMPLEMENTED, logger.messages[0].message)
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
        assertEquals(String.format(ERROR_MISSING, "/foobar"), logger.messages[0].message)
    }

    /**
     * Command should return 550 too if user does not have access to directory.
     */
    @Test
    fun testAccessDenied() {
        executeRequest("AVBL /secure", emptyList())
        assertEquals(1, logger.messages.size)
        assertEquals(550, logger.messages[0].code)
        assertEquals(ERROR_ACCESS_DENIED, logger.messages[0].message)
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
        assertEquals(ERROR_ACCESS_DENIED, logger.messages[0].message)
    }

    /**
     * Command should return 550 if user tried to get free space from a file.
     */
    @Test
    fun testFile() {
        executeRequest("AVBL /test.txt", listOf(WritePermission()))
        assertEquals(1, logger.messages.size)
        assertEquals(550, logger.messages[0].code)
        assertEquals(ERROR_IS_A_FILE, logger.messages[0].message)
    }

    private fun executeRequest(
        commandLine: String,
        permissions: List<Authority>,
        fileSystemFactory: FileSystemFactory = fsFactory,
    ) {
        val context =
            mockk<FtpServerContext> {
                every { fileSystemManager } returns fileSystemFactory
            }
        val ftpSession = FtpIoSession(session, context)
        ftpSession.user =
            BaseUser().also {
                it.homeDirectory = System.getProperty("java.io.tmpdir")
                it.authorities = permissions
            }
        ftpSession.setLogin(fsView)
        val command = AVBL(errorMessageProvider)
        command.execute(
            session = ftpSession,
            context = context,
            request = DefaultFtpRequest(commandLine),
        )
    }
}
