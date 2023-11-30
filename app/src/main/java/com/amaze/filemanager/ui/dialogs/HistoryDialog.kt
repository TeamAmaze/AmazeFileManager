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

import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.HiddenAdapter
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.utils.DataUtils

object HistoryDialog {
    /**
     * Create history dialog, it shows the accessed folders from last accessed to first accessed
     */
    @JvmStatic
    fun showHistoryDialog(mainActivity: MainActivity, mainFragment: MainFragment) {
        val sharedPrefs = mainActivity.prefs
        val appTheme = mainActivity.appTheme

        val adapter = HiddenAdapter(
            mainActivity,
            mainFragment,
            sharedPrefs,
            FileUtils.toHybridFileArrayList(DataUtils.getInstance().history),
            null,
            true
        )

        val materialDialog = MaterialDialog.Builder(mainActivity).also { builder ->
            builder.positiveText(R.string.cancel)
            builder.positiveColor(mainActivity.accent)
            builder.negativeText(R.string.clear)
            builder.negativeColor(mainActivity.accent)
            builder.title(R.string.history)
            builder.onNegative { _: MaterialDialog?, _: DialogAction? ->
                DataUtils.getInstance().clearHistory()
            }
            builder.theme(appTheme.getMaterialDialogTheme())
            builder.adapter(adapter, null)
        }.build()
        adapter.materialDialog = materialDialog
        materialDialog.show()
    }
}
