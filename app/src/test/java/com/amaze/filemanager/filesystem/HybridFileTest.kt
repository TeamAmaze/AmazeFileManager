/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem

import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [JELLY_BEAN, KITKAT, P])
class HybridFileTest {

    /**
     * Test []
     */
    @Test
    fun testGetParentGeneric() {
        val r = Random(123)
        for (i in 0..50) {
            val path = RandomPathGenerator.generateRandomPath(r, 50)
            val file = HybridFile(OpenMode.UNKNOWN, path)
            file.getParent(ApplicationProvider.getApplicationContext())
        }

        for (i in 0..50) {
            val path = RandomPathGenerator.generateRandomPath(r, 50, 0)
            val file = HybridFile(OpenMode.UNKNOWN, path)
            file.getParent(ApplicationProvider.getApplicationContext())
        }
    }

    /**
     * Test [HybridFile.getParent] for SSH paths with trailing slash.
     */
    @Test
    fun testSshGetParentWithTrailingSlash() {
        val file = HybridFile(OpenMode.SFTP, "ssh://user@127.0.0.1/home/user/next/project/")
        assertEquals(
            "ssh://user@127.0.0.1/home/user/next/",
            file.getParent(ApplicationProvider.getApplicationContext())
        )
    }

    /**
     * Test [HybridFile.getParent] for SSH paths without trailing slash.
     */
    @Test
    fun testSshGetParentWithoutTrailingSlash() {
        val file = HybridFile(OpenMode.SFTP, "ssh://user@127.0.0.1/home/user/next/project")
        assertEquals(
            "ssh://user@127.0.0.1/home/user/next",
            file.getParent(ApplicationProvider.getApplicationContext())
        )
    }
}
