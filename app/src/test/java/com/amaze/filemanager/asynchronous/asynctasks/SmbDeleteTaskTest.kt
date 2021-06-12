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

package com.amaze.filemanager.asynchronous.asynctasks

import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.shadows.ShadowSmbUtil.Companion.PATH_CANNOT_DELETE_FILE
import com.amaze.filemanager.utils.SmbUtil
import org.junit.Test

class SmbDeleteTaskTest : AbstractDeleteTaskTestBase() {

    /**
     * Test case to verify delete SMB file success scenario.
     *
     * @see AbstractDeleteTaskTestBase.doTestDeleteFileOk
     */
    @Test
    fun testDeleteSmbFileOk() {
        doTestDeleteFileOk(
            HybridFileParcelable(SmbUtil.create("smb://user:password@1.2.3.4/just/a/file.txt"))
        )
    }

    /**
     * Test case to verify delete SSH file failure scenario.
     *
     * @see AbstractDeleteTaskTestBase.doTestDeleteFileAccessDenied
     */
    @Test
    fun testDeleteSmbFileAccessDenied() {
        doTestDeleteFileAccessDenied(HybridFileParcelable(SmbUtil.create(PATH_CANNOT_DELETE_FILE)))
    }
}
