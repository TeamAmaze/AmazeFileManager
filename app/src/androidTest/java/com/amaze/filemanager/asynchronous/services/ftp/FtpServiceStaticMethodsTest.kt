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

package com.amaze.filemanager.asynchronous.services.ftp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.filters.Suppress
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test is separated from FtpServiceEspressoTest since it does not actually requires the FTP
 * service itself.
 *
 *
 * It is expected that you are not running all the cases in one go. **You have been warned**.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class FtpServiceStaticMethodsTest {
    /** To test [FtpService.getLocalInetAddress] must not return an empty string.  */
    @Test
    fun testGetLocalInetAddressMustNotBeEmpty() {
        ApplicationProvider.getApplicationContext<Context>().run {
            if (!FtpService.isConnectedToLocalNetwork(this))
                fail("Please connect your device to network to run this test!")

            FtpService.getLocalInetAddress(this).also {
                assertNotNull(it)
                assertNotNull(it?.hostAddress)
            }
        }
    }
}
