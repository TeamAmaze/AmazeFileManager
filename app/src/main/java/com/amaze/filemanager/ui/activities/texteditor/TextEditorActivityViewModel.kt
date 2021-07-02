package com.amaze.filemanager.ui.activities.texteditor

import androidx.lifecycle.ViewModel
import com.amaze.filemanager.filesystem.EditableFileAbstraction
import com.amaze.filemanager.utils.SearchResultIndex
import java.io.File
import java.util.*

class TextEditorActivityViewModel : ViewModel() {

    var original: String? = null

    /**
     * represents a file saved in cache
     */
    var cacheFile : File? = null


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