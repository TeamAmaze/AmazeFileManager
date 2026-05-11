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
package com.amaze.filemanager.utils.cloud

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build.VERSION_CODES
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.database.CloudContract
import com.amaze.filemanager.utils.cloud.CloudPluginUtil.isCloudProviderAvailable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Test class for [CloudPluginUtil].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [VERSION_CODES.O, VERSION_CODES.P, VERSION_CODES.R])
class CloudPluginUtilTest {
    /**
     * Test for [isCloudProviderAvailable] for app not installed and installed.
     */
    @Test
    fun testIsCloudProviderAvailable() {
        assertFalse(
            isCloudProviderAvailable(ApplicationProvider.getApplicationContext()),
        )

        val pi = PackageInfo()
        pi.packageName = CloudContract.APP_PACKAGE_NAME
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Context>().packageManager)
            .installPackage(pi)

        assertTrue(
            isCloudProviderAvailable(ApplicationProvider.getApplicationContext()),
        )
    }
}
