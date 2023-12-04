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

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.P, Build.VERSION_CODES.R]
)
class SortTypeTest {

    /** Tests if the Int returned from [SortType.toDirectorySortInt] is as expected */
    @Test
    fun toDirectorySortIntTest() {
        Assert.assertEquals(
            "SortType with SortBy.NAME and SortOrder.ASC was not 0",
            0,
            SortType(SortBy.NAME, SortOrder.ASC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.LAST_MODIFIED and SortOrder.ASC was not 1",
            1,
            SortType(SortBy.LAST_MODIFIED, SortOrder.ASC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.SIZE and SortOrder.ASC was not 2",
            2,
            SortType(SortBy.SIZE, SortOrder.ASC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.TYPE and SortOrder.ASC was not 3",
            3,
            SortType(SortBy.TYPE, SortOrder.ASC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.NAME and SortOrder.DESC was not 4",
            4,
            SortType(SortBy.NAME, SortOrder.DESC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.LAST_MODIFIED and SortOrder.DESC was not 5",
            5,
            SortType(SortBy.LAST_MODIFIED, SortOrder.DESC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.SIZE and SortOrder.DESC was not 6",
            6,
            SortType(SortBy.SIZE, SortOrder.DESC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.TYPE and SortOrder.DESC was not 7",
            7,
            SortType(SortBy.TYPE, SortOrder.DESC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.RELEVANCE and SortOrder.ASC was not 0",
            0,
            SortType(SortBy.RELEVANCE, SortOrder.ASC).toDirectorySortInt()
        )
        Assert.assertEquals(
            "SortType with SortBy.RELEVANCE and SortOrder.DESC was not 4",
            4,
            SortType(SortBy.RELEVANCE, SortOrder.DESC).toDirectorySortInt()
        )
    }

    /** Tests if the [SortType] returned from [SortType.getDirectorySortType] corresponds to the given index */
    @Test
    fun getDirectorySortTypeTest() {
        Assert.assertEquals(
            "0 was not translated to SortType with SortBy.NAME and SortOrder.ASC",
            SortType(SortBy.NAME, SortOrder.ASC),
            SortType.getDirectorySortType(0)
        )
        Assert.assertEquals(
            "1 was not translated to SortType with SortBy.LAST_MODIFIED and SortOrder.ASC",
            SortType(SortBy.LAST_MODIFIED, SortOrder.ASC),
            SortType.getDirectorySortType(1)
        )
        Assert.assertEquals(
            "2 was not translated to SortType with SortBy.SIZE and SortOrder.ASC",
            SortType(SortBy.SIZE, SortOrder.ASC),
            SortType.getDirectorySortType(2)
        )
        Assert.assertEquals(
            "3 was not translated to SortType with SortBy.TYPE and SortOrder.ASC",
            SortType(SortBy.TYPE, SortOrder.ASC),
            SortType.getDirectorySortType(3)
        )
        Assert.assertEquals(
            "4 was not translated to SortType with SortBy.NAME and SortOrder.DESC",
            SortType(SortBy.NAME, SortOrder.DESC),
            SortType.getDirectorySortType(4)
        )
        Assert.assertEquals(
            "5 was not translated to SortType with SortBy.LAST_MODIFIED and SortOrder.DESC",
            SortType(SortBy.LAST_MODIFIED, SortOrder.DESC),
            SortType.getDirectorySortType(5)
        )
        Assert.assertEquals(
            "6 was not translated to SortType with SortBy.SIZE and SortOrder.DESC",
            SortType(SortBy.SIZE, SortOrder.DESC),
            SortType.getDirectorySortType(6)
        )
        Assert.assertEquals(
            "7 was not translated to SortType with SortBy.TYPE and SortOrder.DESC",
            SortType(SortBy.TYPE, SortOrder.DESC),
            SortType.getDirectorySortType(7)
        )
    }
}
