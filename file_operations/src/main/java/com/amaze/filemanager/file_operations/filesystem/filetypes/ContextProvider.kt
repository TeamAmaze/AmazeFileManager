package com.amaze.filemanager.file_operations.filesystem.filetypes

import android.content.Context

interface ContextProvider {
    /**
     * This *must* be thread safe.
     * There are no guarantees on *when* this function is called,
     * it must return a non null ref to [Context] or crash
     */
    fun getContext(): Context?
}