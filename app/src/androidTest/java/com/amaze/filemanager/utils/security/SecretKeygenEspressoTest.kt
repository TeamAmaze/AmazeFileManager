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

package com.amaze.filemanager.utils.security

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test [SecretKeygen] runs on real device. Necessary since Robolectric doesn't have shadows for
 * AndroidKeyStore.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class SecretKeygenEspressoTest {

    /**
     * Test [SecretKeygen.getSecretKey].
     *
     * Officially our lowest supported SDK is 14, hence we will throw exception
     * if the device is so.
     */
    @Test
    fun testGetSecretKey() {
        SecretKeygen.getSecretKey()?.run {
            assertNotNull(this)
            assertEquals("aes", this.algorithm.lowercase())
        } ?: if (SDK_INT < ICE_CREAM_SANDWICH) {
            fail("Android version not supported")
        } else {
            // Do nothing but let it pass
        }
    }
}
