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

import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.theme.AppTheme

/**
 * File encryption warning dialog.
 *
 * This dialog is to warn users of the caveat of using Amaze's own encryption format.
 */
object EncryptWarningDialog {

    /**
     * Display warning dialog on use of Amaze's own encryption format.
     */
    @JvmStatic
    fun show(
        main: MainActivity,
        appTheme: AppTheme
    ) {
        val accentColor: Int = main.accent
        val preferences = PreferenceManager.getDefaultSharedPreferences(main)
        MaterialDialog.Builder(main).run {
            title(main.getString(R.string.warning))
            content(main.getString(R.string.crypt_warning_key))
            theme(appTheme.getMaterialDialogTheme())
            negativeText(main.getString(R.string.warning_never_show))
            positiveText(main.getString(R.string.warning_confirm))
            positiveColor(accentColor)
            onPositive { dialog, _ ->
                dialog.dismiss()
            }
            onNegative { dialog, _ ->
                preferences
                    .edit()
                    .putBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, true)
                    .apply()
                dialog.dismiss()
            }
            show()
        }
    }
}
