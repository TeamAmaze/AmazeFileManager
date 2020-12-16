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

package com.amaze.filemanager.filesystem.smb

import android.content.Context
import android.os.Build.VERSION_CODES.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.ShadowSmbUtil
import com.amaze.filemanager.shadows.ShadowSmbUtil.Companion.PATH_CANNOT_DELETE_FILE
import jcifs.smb.SmbException
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSQLiteConnection

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowSmbUtil::class, ShadowMultiDex::class],
    sdk = [JELLY_BEAN, KITKAT, P]
)
@LooperMode(LooperMode.Mode.PAUSED)
class SmbHybridFileTest {

    private var ctx: Context? = null

    /**
     * Test case setup.
     *
     * TODO: some even more generic test case base to prevent copy-and-paste?
     */
    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    /**
     * Close database on test finished.
     */
    @After
    fun tearDown() {
        ShadowSQLiteConnection.reset()
    }

    /**
     * Test case to verify delete SMB file success scenario.
     *
     * @see HybridFile.delete
     */
    @Test
    fun testDeleteOk() {
        val file = HybridFile(OpenMode.SMB, "smb://user:password@1.2.3.4/just/a/file.txt")
        file.delete(ctx, false)
        assertFalse(file.exists())
    }

    /**
     * Test case to verify delete SMB file failure scenario.
     *
     * @see HybridFile.delete
     */
    @Test(expected = SmbException::class)
    fun testDeleteAccessDenied() {
        val file = HybridFile(OpenMode.SMB, PATH_CANNOT_DELETE_FILE)
        file.delete(ctx, false)
        assertTrue(file.exists())
    }
}
