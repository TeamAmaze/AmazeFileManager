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
class DirSortByTest {
    /** Tests if [DirSortBy.getDirSortBy] returns the correct [DirSortBy] corresponding to the given index */
    @Test
    fun getDirSortByTest() {
        Assert.assertEquals(
            "DirSortBy.getDirSortBy(0) did not return DIR_ON_TOP",
            DirSortBy.DIR_ON_TOP,
            DirSortBy.getDirSortBy(0)
        )
        Assert.assertEquals(
            "DirSortBy.getDirSortBy(1) did not return FILE_ON_TOP",
            DirSortBy.FILE_ON_TOP,
            DirSortBy.getDirSortBy(1)
        )
        Assert.assertEquals(
            "DirSortBy.getDirSortBy(2) did not return NONE_ON_TOP",
            DirSortBy.NONE_ON_TOP,
            DirSortBy.getDirSortBy(2)
        )
        // could be any int except 0 to 2
        val randomIndex = Random.nextInt(3, Int.MAX_VALUE)
        Assert.assertEquals(
            "DirSortBy.getDirSortBy($randomIndex) did not return NONE_ON_TOP",
            DirSortBy.NONE_ON_TOP,
            DirSortBy.getDirSortBy(randomIndex)
        )
    }
}
