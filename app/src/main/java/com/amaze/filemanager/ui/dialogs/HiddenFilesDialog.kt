/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.dialogs

import android.graphics.Color
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.HiddenAdapter
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.utils.DataUtils

object HiddenFilesDialog {
    /**
     * Create hidden files dialog, it shows the files hidden from the context view when selecting
     */
    @JvmStatic
    fun showHiddenDialog(mainActivity: MainActivity, mainFragment: MainFragment) {
        val sharedPrefs = mainActivity.prefs
        val appTheme = mainActivity.appTheme

        val adapter = HiddenAdapter(
            mainActivity,
            mainFragment,
            sharedPrefs,
            FileUtils.toHybridFileConcurrentRadixTree(DataUtils.getInstance().hiddenFiles),
            null,
            false
        )

        val materialDialog = MaterialDialog.Builder(mainActivity).also { builder ->
            builder.positiveText(R.string.close)
            builder.positiveColor(mainActivity.accent)
            builder.title(R.string.hiddenfiles)
            builder.theme(appTheme.getMaterialDialogTheme())
            builder.autoDismiss(true)
            builder.adapter(adapter, null)
            builder.dividerColor(Color.GRAY)
        }.build()

        adapter.materialDialog = materialDialog
        materialDialog.setOnDismissListener {
            mainFragment.loadlist(mainFragment.currentPath, false, OpenMode.UNKNOWN, false)
        }
        materialDialog.show()
    }
}
