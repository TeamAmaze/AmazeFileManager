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
import org.apache.ftpserver.filesystem.nativefs.impl.NativeFileSystemView
import org.apache.ftpserver.ftplet.User
import org.apache.ftpserver.impl.DefaultFtpRequest
import org.apache.ftpserver.impl.FtpIoSession
import org.apache.ftpserver.impl.FtpServerContext
import org.apache.ftpserver.message.MessageResourceFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.io.File

/**
 * Unit test for [PWD].
 */
@Suppress("StringLiteralDuplication")
class PWDCommandTest : AbstractFtpserverCommandTest() {

    private lateinit var fsView: NativeFileSystemView

    private lateinit var user: User

    private lateinit var ftpSession: FtpIoSession

    companion object {

        // Nobody is interested in this, nor asking it any question.
        // Of course in reality FtpServerContext will never be static
        private lateinit var context: FtpServerContext

        /**
         * Mock [FtpServerContext] for test, with custom logic to skip effort to create
         * [MessageResource].
         */
        @JvmStatic
        @BeforeClass
        fun bootstrap() {
            context = Mockito.mock(FtpServerContext::class.java)
            val messages = MessageResourceFactory().createMessageResource()
            `when`(context.messageResource).thenReturn(messages)
        }
    }

    /**
     * Setup before test.
     */
    @Before
    override fun setUp() {
        super.setUp()
        File(Environment.getExternalStorageDirectory(), "Music").mkdirs()
        user = BaseUser().also {
            it.homeDirectory = Environment.getExternalStorageDirectory().absolutePath
            it.authorities = listOf(WritePermission())
        }
        fsView = NativeFileSystemView(user, false)

        ftpSession = FtpIoSession(session, context)
        ftpSession.user = user
        ftpSession.setLogin(fsView)
    }

    /**
     * PWD should never expose device real path to user.
     */
    @Test
    fun testRootDir() {
        executeRequest()
        assertEquals(1, logger.messages.size)
        assertEquals(257, logger.messages[0].code)
        assertEquals("\"/\" is current directory.", logger.messages[0].message)
    }

    /**
     * Test scenario after changing working directory.
     */
    @Test
    fun testChangeDirDown() {
        executeRequest()
        assertEquals(1, logger.messages.size)
        assertEquals(257, logger.messages.last().code)
        assertEquals("\"/\" is current directory.", logger.messages.last().message)
        ftpSession.fileSystemView.changeWorkingDirectory("/Music")
        executeRequest()
        assertEquals(2, logger.messages.size)
        assertEquals(257, logger.messages.last().code)
        assertEquals("\"/Music\" is current directory.", logger.messages.last().message)
    }

    /**
     * Test scenario after a CDUP.
     */
    @Test
    fun testChangeDirUp() {
        executeRequest()
        assertEquals(1, logger.messages.size)
        assertEquals(257, logger.messages.last().code)
        assertEquals("\"/\" is current directory.", logger.messages.last().message)
        ftpSession.fileSystemView.changeWorkingDirectory("/Music")
        executeRequest()
        assertEquals(2, logger.messages.size)
        assertEquals(257, logger.messages.last().code)
        assertEquals("\"/Music\" is current directory.", logger.messages.last().message)
        ftpSession.fileSystemView.changeWorkingDirectory("/")
        executeRequest()
        assertEquals(3, logger.messages.size)
        assertEquals(257, logger.messages.last().code)
        assertEquals("\"/\" is current directory.", logger.messages.last().message)
    }

    private fun executeRequest() {
        val command = PWD()
        command.execute(
            session = ftpSession,
            context = context,
            request = DefaultFtpRequest("PWD")
        )
    }
}
