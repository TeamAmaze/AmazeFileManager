/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.files.sort

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.P, Build.VERSION_CODES.R]
)
class SortByTest {

    /** Tests if [SortBy.getSortBy] returns the correct [SortBy] corresponding to the given index */
    @Test
    fun getSortByTest() {
        Assert.assertEquals(
            "SortBy.getSortBy(0) did not return NAME",
            SortBy.NAME,
            SortBy.getSortBy(0)
        )
        Assert.assertEquals(
            "SortBy.getSortBy(1) did not return LAST_MODIFIED",
            SortBy.LAST_MODIFIED,
            SortBy.getSortBy(1)
        )
        Assert.assertEquals(
            "SortBy.getSortBy(2) did not return SIZE",
            SortBy.SIZE,
            SortBy.getSortBy(2)
        )
        Assert.assertEquals(
            "SortBy.getSortBy(3) did not return TYPE",
            SortBy.TYPE,
            SortBy.getSortBy(3)
        )
        Assert.assertEquals(
            "SortBy.getSortBy(4) did not return RELEVANCE",
            SortBy.RELEVANCE,
            SortBy.getSortBy(4)
        )
        // could be any int except 0 to 4
        val randomIndex = Random.nextInt(5, Int.MAX_VALUE)
        Assert.assertEquals(
            "SortBy.getDirectorySortBy($randomIndex) did not return NAME",
            SortBy.NAME,
            SortBy.getDirectorySortBy(randomIndex)
        )
    }

    /** Tests if [SortBy.getDirectorySortBy] returns the correct [SortBy] corresponding to the given index */
    @Test
    fun getDirectorySortByTest() {
        Assert.assertEquals(
            "SortBy.getDirectorySortBy(0) did not return NAME",
            SortBy.NAME,
            SortBy.getDirectorySortBy(0)
        )
        Assert.assertEquals(
            "SortBy.getDirectorySortBy(1) did not return LAST_MODIFIED",
            SortBy.LAST_MODIFIED,
            SortBy.getDirectorySortBy(1)
        )
        Assert.assertEquals(
            "SortBy.getDirectorySortBy(2) did not return SIZE",
            SortBy.SIZE,
            SortBy.getDirectorySortBy(2)
        )
        Assert.assertEquals(
            "SortBy.getDirectorySortBy(3) did not return TYPE",
            SortBy.TYPE,
            SortBy.getDirectorySortBy(3)
        )
        // could be any int except 0 to 3
        val randomIndex = Random.nextInt(4, Int.MAX_VALUE)
        Assert.assertEquals(
            "SortBy.getDirectorySortBy($randomIndex) did not return NAME",
            SortBy.NAME,
            SortBy.getDirectorySortBy(randomIndex)
        )
    }
}
