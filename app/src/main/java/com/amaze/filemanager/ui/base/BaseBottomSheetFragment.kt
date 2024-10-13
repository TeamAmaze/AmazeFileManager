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

package com.amaze.filemanager.ui.base

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.theme.AppTheme
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState) as BottomSheetDialog
    }

    /**
     * Initializes bottom sheet ui resources based on current theme
     */
    fun initDialogResources(rootView: View) {
        when ((requireActivity() as ThemedActivity).appTheme!!) {
            AppTheme.DARK -> {
                rootView.setBackgroundDrawable(
                    context?.resources?.getDrawable(
                        R.drawable.shape_dialog_bottomsheet_dark,
                    ),
                )
            }
            AppTheme.BLACK -> {
                rootView.setBackgroundDrawable(
                    context?.resources?.getDrawable(
                        R.drawable.shape_dialog_bottomsheet_black,
                    ),
                )
            }
            AppTheme.LIGHT -> {
                rootView
                    .setBackgroundDrawable(
                        context?.resources?.getDrawable(
                            R.drawable.shape_dialog_bottomsheet_white,
                        ),
                    )
            }
        }
    }
}
