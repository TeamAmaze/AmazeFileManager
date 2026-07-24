package com.amaze.filemanager.ui.fragments.preferencefragments

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.fragments.data.BookmarkData
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.amaze.filemanager.utils.DataUtils

class BookmarkPrefsViewModel : ViewModel() {
     val position: MutableMap<Preference, Int> = HashMap()
     var bookmarksList: PreferenceCategory? = null


    fun isValidBookmarkPath(
        name: String,
        path: String,
        dataUtils: DataUtils,
        prefs: SharedPreferences
    ): WarnableTextInputValidator.ReturnState {
        return when {
            path.isBlank() -> WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR,
                R.string.ftp_path_change_error_invalid
            )

            dataUtils.containsBooks(
                arrayOf(
                    name,
                    path
                )
            ) != -1 -> WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR,
                R.string.bookmark_exists
            )

            !FileUtils.isPathAccessible(
                path,
                prefs
            ) -> WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR,
                R.string.ftp_path_change_error_invalid
            )

            else -> WarnableTextInputValidator.ReturnState()
        }
    }

    fun isValidBookmarkName(
        name: String,
    ): WarnableTextInputValidator.ReturnState {
        return when {
            name.isBlank() -> WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR,
                R.string.invalid_name
            )

            else -> WarnableTextInputValidator.ReturnState()
        }
    }

    fun isValidBookmark(
        bookmark: BookmarkData,
        dataUtils: DataUtils,
        prefs: SharedPreferences
    ): Pair<BookmarkField?, WarnableTextInputValidator.ReturnState> {
        return when {
            bookmark.name.isBlank() -> Pair(
                BookmarkField.NAME, WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR,
                    R.string.invalid_name
                )
            )
            bookmark.path.isBlank() -> Pair(
                BookmarkField.PATH, WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR,
                    R.string.ftp_path_change_error_invalid
                )
            )
            dataUtils.containsBooks(
                arrayOf(
                    bookmark.name,
                    bookmark.path
                )
            ) != -1 -> Pair(
                BookmarkField.PATH, WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR,
                    R.string.bookmark_exists
                )
            )
            !FileUtils.isPathAccessible(
                bookmark.path,
                prefs
            ) -> Pair(
                BookmarkField.PATH, WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR,
                    R.string.ftp_path_change_error_invalid
                )
            )

            else -> Pair(null, WarnableTextInputValidator.ReturnState())
        }
    }


}
enum class BookmarkField{
    NAME,
    PATH
}