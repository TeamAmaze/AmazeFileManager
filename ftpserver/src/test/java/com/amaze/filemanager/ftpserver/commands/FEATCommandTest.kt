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

import io.mockk.mockk
import org.apache.ftpserver.impl.DefaultFtpRequest
import org.apache.ftpserver.impl.FtpIoSession
import org.apache.ftpserver.impl.FtpServerContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for [FEAT].
 */
class FEATCommandTest : AbstractFtpserverCommandTest() {
    companion object {
        private const val FEAT_RESPONSE = "Extensions supported:\n AVBL\n UTF8"
    }

    /**
     * Test command output. Expect AVBL is among list of extensions implemented.
     */
    @Test
    fun testCommand() {
        val context = mockk<FtpServerContext>(relaxed = true)
        val ftpSession = FtpIoSession(session, context)
        val command = FEAT { FEAT_RESPONSE }
        command.execute(
            session = ftpSession,
            context = context,
            request = DefaultFtpRequest("FEAT"),
        )
        assertEquals(1, logger.messages.size)
        assertEquals(211, logger.messages[0].code)
        assertEquals(FEAT_RESPONSE, logger.messages[0].message)
        assertTrue(logger.messages[0].message.contains("AVBL"))
    }
}
