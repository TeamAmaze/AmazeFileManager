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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceManager
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.io.IOException

class BackupPrefsFragment : BasePrefsFragment() {

    private val TAG: String = BasePrefsFragment::class.java.simpleName

    override val title = R.string.backup

    private val onExportPrefClick = OnPreferenceClickListener {

        val map: Map<*, *> = PreferenceManager.getDefaultSharedPreferences(getActivity()).all

        val gsonString: String = Gson().toJson(map)

        try {

            val file = File(context?.cacheDir?.absolutePath + File.separator + "amaze_backup.json")

            val fileWriter = FileWriter(file)

            fileWriter.append(gsonString)

            Log.e(TAG, "wrote export to :${file.absolutePath}")

            fileWriter.flush()
            fileWriter.close()

            Toast.makeText(
                context,
                getString(R.string.select_save_location),
                Toast.LENGTH_SHORT
            )
                .show()

            val intent = Intent(context, MainActivity::class.java)

            intent.action = Intent.ACTION_SEND
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))

            startActivity(intent)
        } catch (e: IOException) {
            Toast.makeText(context, getString(R.string.exporting_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        true
    }

    private val onImportPrefClick = OnPreferenceClickListener {

        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.backup_prefs, rootKey)

        findPreference<Preference>(
            PreferencesConstants.PREFERENCE_EXPORT_SETTINGS
        )?.onPreferenceClickListener = onExportPrefClick

        findPreference<Preference>(
            PreferencesConstants.PREFERENCE_IMPORT_SETTINGS
        )?.onPreferenceClickListener = onImportPrefClick
    }
}
