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

package com.amaze.filemanager.ui.activities.texteditor

import androidx.lifecycle.ViewModel
import com.amaze.filemanager.filesystem.EditableFileAbstraction
import java.io.File
import java.util.*

class TextEditorActivityViewModel : ViewModel() {

    var original: String? = null

    /**
     * represents a file saved in cache
     */
    var cacheFile: File? = null

    var modified = false

    /**
     * variable to maintain the position of index
     * while pressing next/previous button in the searchBox
     */
    var current = -1

    /**
     * variable to maintain line number of the searched phrase
     * further used to calculate the scroll position
     */
    var line = 0

    /**
     * List maintaining the searched text's start/end index as key/value pair
     */
    var searchResultIndices = listOf<SearchResultIndex>()

    var timer: Timer? = null

    var file: EditableFileAbstraction? = null
}
