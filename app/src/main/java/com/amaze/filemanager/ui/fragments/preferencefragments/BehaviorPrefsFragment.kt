/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import android.text.InputType
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment.Companion.clearPreferences
import com.amaze.trashbin.TrashBinConfig
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
        findPreference<Preference>(PreferencesConstants.PREFERENCE_TRASH_BIN_RETENTION_NUM_OF_FILES)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            trashBinRetentionNumOfFiles()
            true
        }
        findPreference<Preference>(PreferencesConstants.PREFERENCE_TRASH_BIN_RETENTION_DAYS)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            trashBinRetentionDays()
            true
        }
        findPreference<Preference>(PreferencesConstants.PREFERENCE_TRASH_BIN_RETENTION_BYTES)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            trashBinRetentionBytes()
            true
        }
        findPreference<Preference>(PreferencesConstants.PREFERENCE_TRASH_BIN_CLEANUP_INTERVAL)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            trashBinCleanupInterval()
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

    private fun trashBinRetentionNumOfFiles() {
        val dialogBuilder = MaterialDialog.Builder(activity)
        dialogBuilder.title(
            resources.getString(R.string.trash_bin_retention_num_of_files_title)
        )
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val numOfFiles = sharedPrefs.getInt(
            PreferencesConstants.KEY_TRASH_BIN_RETENTION_NUM_OF_FILES,
            TrashBinConfig.RETENTION_NUM_OF_FILES
        )
        dialogBuilder.inputType(InputType.TYPE_CLASS_NUMBER)
        dialogBuilder.input(
            "",
            "$numOfFiles",
            true
        ) { _, _ -> }
        dialogBuilder.theme(
            activity.utilsProvider.appTheme.materialDialogTheme
        )
        dialogBuilder.positiveText(resources.getString(R.string.ok))
        dialogBuilder.negativeText(resources.getString(R.string.cancel))
        dialogBuilder.neutralText(resources.getString(R.string.default_string))
        dialogBuilder.positiveColor(activity.accent)
        dialogBuilder.negativeColor(activity.accent)
        dialogBuilder.neutralColor(activity.accent)
        dialogBuilder.onPositive { dialog, _ ->
            val inputText = dialog.inputEditText?.text.toString()
            sharedPrefs.edit().putInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_NUM_OF_FILES,
                inputText.toInt()
            ).apply()
        }
        dialogBuilder.onNeutral { _, _ ->
            sharedPrefs.edit().putInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_NUM_OF_FILES,
                TrashBinConfig.RETENTION_NUM_OF_FILES
            ).apply()
        }
        dialogBuilder.onNegative { dialog, _ -> dialog.cancel() }
        dialogBuilder.build().show()
    }

    private fun trashBinRetentionDays() {
        val dialogBuilder = MaterialDialog.Builder(activity)
        dialogBuilder.title(
            resources.getString(R.string.trash_bin_retention_days_title)
        )
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val days = sharedPrefs.getInt(
            PreferencesConstants.KEY_TRASH_BIN_RETENTION_DAYS,
            TrashBinConfig.RETENTION_DAYS_INFINITE
        )
        dialogBuilder.inputType(InputType.TYPE_CLASS_NUMBER)
        dialogBuilder.input(
            "",
            "$days",
            true
        ) { _, _ -> }
        dialogBuilder.theme(
            activity.utilsProvider.appTheme.materialDialogTheme
        )
        dialogBuilder.positiveText(resources.getString(R.string.ok))
        dialogBuilder.negativeText(resources.getString(R.string.cancel))
        dialogBuilder.neutralText(resources.getString(R.string.default_string))
        dialogBuilder.positiveColor(activity.accent)
        dialogBuilder.negativeColor(activity.accent)
        dialogBuilder.neutralColor(activity.accent)
        dialogBuilder.onPositive { dialog, _ ->
            val inputText = dialog.inputEditText?.text.toString()
            sharedPrefs.edit().putInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_DAYS,
                inputText.toInt()
            ).apply()
        }
        dialogBuilder.onNeutral { _, _ ->
            sharedPrefs.edit().putInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_DAYS,
                TrashBinConfig.RETENTION_DAYS_INFINITE
            ).apply()
        }
        dialogBuilder.onNegative { dialog, _ -> dialog.cancel() }
        dialogBuilder.build().show()
    }

    private fun trashBinRetentionBytes() {
        val dialogBuilder = MaterialDialog.Builder(activity)
        dialogBuilder.title(
            resources.getString(R.string.trash_bin_retention_bytes_title)
        )
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val bytes = sharedPrefs.getLong(
            PreferencesConstants.KEY_TRASH_BIN_RETENTION_BYTES,
            TrashBinConfig.RETENTION_BYTES_INFINITE
        )
        dialogBuilder.inputType(InputType.TYPE_CLASS_NUMBER)
        dialogBuilder.input(
            "",
            "$bytes",
            true
        ) { _, _ -> }
        dialogBuilder.theme(
            activity.utilsProvider.appTheme.materialDialogTheme
        )
        dialogBuilder.positiveText(resources.getString(R.string.ok))
        dialogBuilder.negativeText(resources.getString(R.string.cancel))
        dialogBuilder.neutralText(resources.getString(R.string.default_string))
        dialogBuilder.positiveColor(activity.accent)
        dialogBuilder.negativeColor(activity.accent)
        dialogBuilder.neutralColor(activity.accent)
        dialogBuilder.onPositive { dialog, _ ->
            val inputText = dialog.inputEditText?.text.toString()
            sharedPrefs.edit().putLong(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_BYTES,
                inputText.toLong()
            ).apply()
        }
        dialogBuilder.onNeutral { _, _ ->
            sharedPrefs.edit().putLong(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_BYTES,
                TrashBinConfig.RETENTION_BYTES_INFINITE
            ).apply()
        }
        dialogBuilder.onNegative { dialog, _ -> dialog.cancel() }
        dialogBuilder.build().show()
    }

    private fun trashBinCleanupInterval() {
        val dialogBuilder = MaterialDialog.Builder(activity)
        dialogBuilder.title(
            resources.getString(R.string.trash_bin_cleanup_interval_title)
        )
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val intervalHours = sharedPrefs.getInt(
            PreferencesConstants.KEY_TRASH_BIN_CLEANUP_INTERVAL_HOURS,
            TrashBinConfig.INTERVAL_CLEANUP_HOURS
        )
        dialogBuilder.inputType(InputType.TYPE_CLASS_NUMBER)
        dialogBuilder.input(
            "",
            "$intervalHours",
            true
        ) { _, _ -> }
        dialogBuilder.theme(
            activity.utilsProvider.appTheme.materialDialogTheme
        )
        dialogBuilder.positiveText(resources.getString(R.string.ok))
        dialogBuilder.negativeText(resources.getString(R.string.cancel))
        dialogBuilder.neutralText(resources.getString(R.string.default_string))
        dialogBuilder.positiveColor(activity.accent)
        dialogBuilder.negativeColor(activity.accent)
        dialogBuilder.neutralColor(activity.accent)
        dialogBuilder.onPositive { dialog, _ ->
            val inputText = dialog.inputEditText?.text.toString()
            sharedPrefs.edit().putInt(
                PreferencesConstants.KEY_TRASH_BIN_CLEANUP_INTERVAL_HOURS,
                inputText.toInt()
            ).apply()
        }
        dialogBuilder.onNeutral { _, _ ->
            sharedPrefs.edit().putInt(
                PreferencesConstants.KEY_TRASH_BIN_CLEANUP_INTERVAL_HOURS,
                TrashBinConfig.INTERVAL_CLEANUP_HOURS
            ).apply()
        }
        dialogBuilder.onNegative { dialog, _ -> dialog.cancel() }
        dialogBuilder.build().show()
    }
}
