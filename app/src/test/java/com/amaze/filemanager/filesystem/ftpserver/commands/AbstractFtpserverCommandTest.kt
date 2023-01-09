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

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.apache.mina.core.session.DummySession
import org.apache.mina.core.session.IoSession
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Base class for ftpserver command unit tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
abstract class AbstractFtpserverCommandTest {

    protected lateinit var logger: LogMessageFilter

    protected lateinit var session: IoSession

    /**
     * Test setup. Create dummy [IoSession] and bind logging filter to it.
     */
    @Before
    open fun setUp() {
        logger = LogMessageFilter()
        session = DummySession()
        session.filterChain.addFirst("logging", logger)
    }

    /**
     * Post test cleanup
     */
    @After
    open fun tearDown() {
        session.closeNow()
        logger.reset()
    }
}
