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

package com.amaze.filemanager.adapters.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.adapters.data.IconDataParcelable.IMAGE_FROMCLOUD
import com.amaze.filemanager.adapters.data.IconDataParcelable.IMAGE_RES
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutElementParcelableEspressoTest {
    /**
     * Test constructor of [LayoutElementParcelable] with a big remote file (size > 10 MB)
     * and verify that the icon type is set to [IMAGE_RES].
     */
    @Test
    fun testConstructorWithBigRemoteFile() {
        val a =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "test-verify.jpg",
                "ssh://127.0.0.1:22222/home/user/test-verify.jpg",
                "777",
                "",
                "17.89 MB",
                17889945,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SFTP,
            )
        a.iconData.run {
            assertEquals(IMAGE_RES, type)
        }
    }

    /**
     * Test constructor of [LayoutElementParcelable] with a small remote file (size <= 10 MB)
     * and verify that the icon type is set to [IMAGE_FROMCLOUD].
     */
    @Test
    fun testConstructorWithSmallRemoteFile() {
        val b =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "test-verify.jpg",
                "ssh://127.0.0.1:22222/home/user/test-verify.jpg",
                "777",
                "",
                "100 KB",
                102400,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SFTP,
            )
        b.iconData.run {
            assertEquals(IMAGE_FROMCLOUD, type)
        }
    }
}
