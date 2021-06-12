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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class XzHelperTaskTest2 : AbstractCompressedHelperTaskTest() {
    @Test
    override fun testRoot() {
        val task = createTask("")
        val result = task.doInBackground()
        assertEquals(result.result.size.toLong(), 1)
        assertEquals("compress", result.result[0].name)
    }

    @Test
    override fun testSublevels() {
        var task = createTask("compress")
        var result = task.doInBackground()
        assertEquals(result.result.size.toLong(), 3)
        assertEquals("a", result.result[0].name)
        assertEquals("bç", result.result[1].name)
        assertEquals("r.txt", result.result[2].name)
        assertEquals(4, result.result[2].size)
        task = createTask("compress/a")
        result = task.doInBackground()
        assertEquals(result.result.size.toLong(), 0)
        task = createTask("compress/bç")
        result = task.doInBackground()
        assertEquals(result.result.size.toLong(), 1)
        assertEquals("t.txt", result.result[0].name)
        assertEquals(6, result.result[0].size)
    }

    override fun createTask(relativePath: String): CompressedHelperTask = XzHelperTask(
        ApplicationProvider.getApplicationContext(),
        File(Environment.getExternalStorageDirectory(), "compress.tar.xz").absolutePath,
        relativePath,
        false,
        emptyCallback
    )
}
