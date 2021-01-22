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

import android.os.Build.VERSION_CODES.*
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Ignore("Excluded until build runs on JDK 9 and Robolectric upgrades to 4.5")
@RunWith(AndroidJUnit4::class)
@Config(sdk = [JELLY_BEAN, KITKAT, P])
@LooperMode(LooperMode.Mode.PAUSED)
@Suppress("StringLiteralDuplication")
class FileUtilTest {

    /**
     * Test [FileUtil.remapPathForApi30OrAbove] for unmeaningful paths.
     */
    @Test
    fun testRemapPathForApi30OrAboveForUnmeaningfulPaths() {
        assertEquals(
            "/foo/bar",
            FileUtil.remapPathForApi30OrAbove("/foo/bar", true)
        )
        assertEquals(
            "/foo/bar",
            FileUtil.remapPathForApi30OrAbove("/foo/bar", false)
        )
    }

    /**
     * Test [FileUtil.remapPathForApi30OrAbove] for meaningful paths but without root.
     */
    @Test
    fun testRemapPathForApi30OrAboveForMeaningfulPathsWithoutRoot() {
        val prefix = Environment.getExternalStorageDirectory().absolutePath
        assertEquals(
            "$prefix/Android/data",
            FileUtil.remapPathForApi30OrAbove("$prefix/Android/data", false)
        )
        assertEquals(
            "$prefix/Android/obb",
            FileUtil.remapPathForApi30OrAbove("$prefix/Android/obb", false)
        )
    }

    /**
     * Test [FileUtil.remapPathForApi30OrAbove] for meaningful paths but without root.
     */
    @Test
    fun testRemapPathForApi30OrAboveForMeaningfulPathsWithRoot() {
        val prefix = Environment.getExternalStorageDirectory().absolutePath
        assertEquals(
            "/data/media/external-cache/Android/data",
            FileUtil.remapPathForApi30OrAbove("$prefix/Android/data", true)
        )
        assertEquals(
            "/data/media/external-cache/Android/obb",
            FileUtil.remapPathForApi30OrAbove("$prefix/Android/obb", true)
        )
    }
}
