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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.amaze.filemanager.utils.NetworkUtil
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
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
        /* Android emulator's "wifi connectivity" only exists from API 25.
         * On the other hand, we don't do wifi AP in code, either it's not possible
         * for lower APIs, nor we currently have no plans on doing this - see #515, #2720 -
         * therefore we only run this test from API 25 or above.
         * - TranceLove
         */
        if (SDK_INT >= N_MR1) {
            ApplicationProvider.getApplicationContext<Context>().run {
                if (!NetworkUtil.isConnectedToLocalNetwork(this)) {
                    fail("Please connect your device to network to run this test!")
                }

                NetworkUtil.getLocalInetAddress(this).also {
                    assertNotNull(it)
                    assertNotNull(it?.hostAddress)
                }
            }
        }
    }
}
