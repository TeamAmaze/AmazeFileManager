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

package com.amaze.filemanager.filesystem.cloud

import com.amaze.filemanager.database.CloudHandler
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.file_operations.filesystem.OpenMode.*
import com.amaze.filemanager.filesystem.RandomPathGenerator
import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

class CloudTest {
    /**
     * Tests [CloudUtil.stripPath]
     */
    @Test
    fun stripPathTest() {
        val assertForTest = { mode: OpenMode, path: String, completePath: String ->
            Assert.assertEquals(path, CloudUtil.stripPath(mode, completePath))
        }

        val generatePathForMode = { mode: OpenMode, path: String ->
            val prefix = when (mode) {
                DROPBOX -> CloudHandler.CLOUD_PREFIX_DROPBOX
                BOX -> CloudHandler.CLOUD_PREFIX_BOX
                GDRIVE -> CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE
                ONEDRIVE -> CloudHandler.CLOUD_PREFIX_ONE_DRIVE
                else -> null
            }
            requireNotNull(prefix)
            prefix + RandomPathGenerator.separator + path
        }

        val r = Random(123)

        for (i in 0..50) {
            val path = RandomPathGenerator.generateRandomPath(r, 50)

            val genAndStrip = { mode: OpenMode ->
                CloudUtil.stripPath(mode, generatePathForMode(mode, path))
            }

            assertForTest(DROPBOX, path, genAndStrip(DROPBOX))
            assertForTest(BOX, path, genAndStrip(BOX))
            assertForTest(GDRIVE, path, genAndStrip(GDRIVE))
            assertForTest(ONEDRIVE, path, genAndStrip(ONEDRIVE))
        }
    }
}
