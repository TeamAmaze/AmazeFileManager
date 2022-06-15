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

package com.amaze.filemanager.ui.fragments.preferencefragments

import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog
import com.amaze.filemanager.ui.activities.PreferencesActivity
import java.io.File

abstract class BasePrefsFragment : PreferenceFragmentCompat(), FolderChooserDialog.FolderCallback {
    protected val activity: PreferencesActivity
        get() = requireActivity() as PreferencesActivity

    abstract val title: Int

    override fun onResume() {
        super.onResume()

        activity.supportActionBar?.title = getString(title)
    }

    override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
        dialog.dismiss()
    }

    override fun onFolderChooserDismissed(dialog: FolderChooserDialog) {
        dialog.dismiss()
    }
}
