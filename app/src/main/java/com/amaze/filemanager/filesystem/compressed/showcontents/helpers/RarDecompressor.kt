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

package com.amaze.filemanager.filesystem.compressed.showcontents.helpers

import android.content.Context
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.asynchronous.asynctasks.compress.RarHelperTask
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import com.github.junrar.rarfile.FileHeader
import java.util.*

class RarDecompressor(context: Context) : Decompressor(context) {
    override fun changePath(
        path: String,
        addGoBackItem: Boolean,
        onFinish: OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>
    ) =
        RarHelperTask(filePath!!, path, addGoBackItem, onFinish)

    override fun realRelativeDirectory(dir: String): String {
        var dir = dir
        if (dir.endsWith(CompressedHelper.SEPARATOR)) {
            dir = dir.substring(0, dir.length - 1)
        }
        return dir.replace(CompressedHelper.SEPARATOR.toCharArray()[0], '\\')
    }

    companion object {

        /**
         * Helper method to convert RAR [FileHeader] entries containing backslashes back to slashes.
         *
         * @param file RAR entry as [FileHeader] object
         */
        @JvmStatic
        fun convertName(file: FileHeader): String {
            val name = file.fileName.replace('\\', '/')
            return if (file.isDirectory) {
                name + CompressedHelper.SEPARATOR
            } else {
                name
            }
        }
    }
}
