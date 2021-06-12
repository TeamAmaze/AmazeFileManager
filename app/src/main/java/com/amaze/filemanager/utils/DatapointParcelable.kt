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

package com.amaze.filemanager.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Class stores the [AbstractProgressiveService] progress variables. This class also acts
 * as data carrier to communicate with [ProcessViewerFragment]
 *
 * @param name name of source file being copied
 * @param amountOfSourceFiles total number of source files to be copied
 * @param sourceProgress which file is being copied from total number of files
 * @param totalSize total size of all source files combined
 * @param byteProgress current byte position in total bytes pool
 * @param speedRaw bytes being copied per sec
 * @param move allows changing the text from "Copying" to "Moving" in case of copy
 * @param completed if the operation has finished
 */
@Parcelize
data class DatapointParcelable(
    val name: String?,
    val amountOfSourceFiles: Int,
    val sourceProgress: Int,
    val totalSize: Long,
    val byteProgress: Long,
    val speedRaw: Long,
    val move: Boolean,
    val completed: Boolean
) : Parcelable {

    companion object {
        /**
         * For the first datapoint, everything is 0 or false except the params. Allows move boolean to
         * change the text from "Copying" to "Moving" in case of copy.
         *
         * @param name name of source file being copied
         * @param amountOfSourceFiles total number of source files to be copied
         * @param totalSize total size of all source files combined
         * @param move allows changing the text from "Copying" to "Moving" in case of copy
         */
        fun buildDatapointParcelable(
            name: String?,
            amountOfSourceFiles: Int,
            totalSize: Long,
            move: Boolean
        ): DatapointParcelable =
            DatapointParcelable(
                name,
                amountOfSourceFiles,
                0,
                totalSize,
                0,
                0,
                move,
                false
            )
    }
}
