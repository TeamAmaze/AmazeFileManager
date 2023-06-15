/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import com.amaze.filemanager.filesystem.HybridFile

object MediaConnectionUtils {
    private val TAG = MediaConnectionUtils::class.java.simpleName

    /**
     * Invokes MediaScannerConnection#scanFile for the given files
     *
     * @param context the context
     * @param hybridFiles files to be scanned
     */
    @JvmStatic
    fun scanFile(context: Context, hybridFiles: Array<HybridFile>) {
        val paths = arrayOfNulls<String>(hybridFiles.size)

        for (i in hybridFiles.indices) paths[i] = hybridFiles[i].path

        MediaScannerConnection.scanFile(
            context,
            paths,
            null
        ) {
                path: String, _: Uri? ->
            Log.i(TAG, "public scanFile() Finished scanning path$path")
        }
    }
}
