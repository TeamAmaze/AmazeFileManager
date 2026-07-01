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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.FileWindowReader
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.ReadTextFileCallable
import com.amaze.filemanager.filesystem.EditableFileAbstraction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Timer

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

    // ── Markdown preview state ────────────────────────────────────────

    /** Whether Markdown preview mode is currently enabled. */
    var markdownPreviewEnabled = false

    // ── Sliding window state ──────────────────────────────────────────

    /** Whether the editor is in windowed (read-only) mode for large files. */
    var isWindowed = false

    /** Seekable reader for the underlying file; lives in ViewModel to survive rotation. */
    var fileWindowReader: FileWindowReader? = null

    /** Byte offset of the start of the currently displayed window. */
    var windowStartByte: Long = 0L

    /** Byte offset just past the end of the currently displayed window. */
    var windowEndByte: Long = 0L

    /**
     * Byte offset of the start of the window that was displayed *before* the most recently
     * loaded window. Used by the Activity to translate a scroll anchor position (captured while
     * the previous window's text was still on screen) into an absolute byte offset in the file,
     * so the new window's scroll position can be resolved precisely without doing an expensive
     * (and ambiguous, for repetitive content) text search.
     */
    var previousWindowStartByte: Long = 0L

    /** Total file size in bytes. */
    var totalFileSize: Long = 0L

    /** Dispatcher for IO operations. Override in tests with a test dispatcher. */
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _windowContent = MutableLiveData<FileWindowReader.WindowResult>()

    /** Observed by the Activity to update the EditText when a new window is loaded. */
    val windowContent: LiveData<FileWindowReader.WindowResult> = _windowContent

    private val _isLoadingWindow = MutableLiveData(false)

    /**
     * Observed by the Activity to show/hide a progress indicator while a window (initial or
     * subsequent) is being loaded from disk.
     */
    val isLoadingWindow: LiveData<Boolean> = _isLoadingWindow

    private var windowLoadJob: Job? = null

    enum class Direction { FORWARD, BACKWARD }

    /** Direction of the most recent successful window load. */
    var lastLoadDirection: Direction? = null

    /**
     * Loads the next or previous window of text from the file.
     * Debounced: if a load is already in flight, the call is ignored.
     *
     * The method attempts to maintain ~60% overlap between consecutive windows to provide
     * smooth scrolling. Due to line-boundary snapping, exact overlap cannot be guaranteed,
     * but this provides better continuity than 50% overlap.
     */
    fun loadWindow(direction: Direction) {
        if (windowLoadJob?.isActive == true) return // debounce
        val reader = fileWindowReader ?: return

        val windowSize = windowEndByte - windowStartByte
        // Use 40% shift (60% overlap) for smoother transitions
        val shiftAmount = (windowSize * 0.4).toLong()

        val targetOffset =
            when (direction) {
                Direction.FORWARD -> {
                    // Don't shift if already at end of file
                    if (windowEndByte >= totalFileSize) return
                    windowStartByte + shiftAmount
                }
                Direction.BACKWARD -> {
                    // Don't shift if already at start of file
                    if (windowStartByte <= 0L) return
                    maxOf(0L, windowStartByte - shiftAmount)
                }
            }

        val startOfCurrentWindow = windowStartByte

        windowLoadJob =
            viewModelScope.launch {
                _isLoadingWindow.value = true
                try {
                    val result =
                        withContext(ioDispatcher) {
                            reader.readWindow(targetOffset, ReadTextFileCallable.MAX_FILE_SIZE_CHARS)
                        }
                    previousWindowStartByte = startOfCurrentWindow
                    windowStartByte = result.startByte
                    windowEndByte = result.endByte
                    lastLoadDirection = direction
                    _windowContent.value = result
                } finally {
                    _isLoadingWindow.value = false
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        windowLoadJob?.cancel()
        try {
            fileWindowReader?.close()
        } catch (_: Exception) {
        }
    }
}
