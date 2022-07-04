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

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import java.util.ArrayList

/**
 * For gzip, bz2, lzma and xz compressed files.
 *
 * These are only single file with compression, not to consider as archives.
 *
 * These files will only show uncompressed size = 0 and last modified date = 1 Jan 1970, reason -
 *
 * gzip stores uncompressed size at the last 4 bytes, which may be costly to obtain this value on
 * mobile devices, and may be inaccurate for files larger than 4GB anyway.
 *
 * It does stores file last modified time, but it's optional.
 *
 * https://datatracker.ietf.org/doc/html/rfc1952
 *
 * xz and lzma stores uncompressed size at header, but is optional. No file last modified date.
 *
 * https://svn.python.org/projects/external/xz-5.0.3/doc/lzma-file-format.txt
 * https://tukaani.org/xz/xz-file-format.txt
 *
 * bzip2 does not store uncompressed size nor last modified time as current documentation shows.
 *
 * Therefore, we only use placeholder value of filesize = 0, last modified date = 0
 * for all of the above types.
 *
 * It is possible to implement uncompressed size for xz and lzma properly in the future, but are of
 * lower priority. Any help would be appreciated.
 */
class UnknownCompressedFileHelperCallable(
    private val filePath: String,
    goBack: Boolean
) :
    CompressedHelperCallable(goBack) {

    override fun addElements(elements: ArrayList<CompressedObjectParcelable>) {
        val entryName = filePath.substringAfterLast('/').substringBeforeLast('.')
        elements.add(
            CompressedObjectParcelable(
                entryName,
                0L,
                0L,
                false
            )
        )
    }
}
