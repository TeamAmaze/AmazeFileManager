/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.os.Bundle
import android.os.Environment
import androidx.preference.Preference
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment.Companion.clearPreferences
import java.io.File

class BehaviorPrefsFragment : BasePrefsFragment(), FolderChooserDialog.FolderCallback {
    override val title = R.string.behavior

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.behavior_prefs, rootKey)

        findPreference<Preference>("clear_open_file")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                clearPreferences(activity.prefs)
                AppConfig.toast(getActivity(), activity.getString(R.string.done))
                true
            }

        findPreference<Preference>(PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            FolderChooserDialog.Builder(activity)
                .tag(PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH)
                .goUpLabel(getString(R.string.folder_go_up_one_level))
                .chooseButton(R.string.choose_folder)
                .cancelButton(R.string.cancel)
                .initialPath(
                    activity.prefs.getString(
                        PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH,
                        Environment.getExternalStorageDirectory().path
                    )
                )
                .build()
                .show(activity)
            true
        }
    }

    override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
        // Just to be safe
        if (folder.exists() && folder.isDirectory) {
            // Write settings to preferences
            val e = activity.prefs.edit()
            e.putString(dialog.tag, folder.absolutePath)
            e.apply()
        }
        dialog.dismiss()
    }
}
