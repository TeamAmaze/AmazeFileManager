package com.amaze.filemanager.file_operations.filesystem.filetypes.file

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager

object UriForSafPersistance {
    const val PREFERENCE_URI = "URI"

    @JvmStatic
    fun persist(context: Context, treeUri: Uri) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PREFERENCE_URI, treeUri.toString())
            .apply()
    }

    @JvmStatic
    fun get(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREFERENCE_URI, null)
    }
}