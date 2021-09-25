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

package com.amaze.filemanager.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.asynctasks.PrepareCopyTask
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.utils.safeLet

class DragAndDropDialog : DialogFragment() {

    var pasteLocation: String? = null
    var operationFiles: ArrayList<HybridFileParcelable>? = null
    var mainActivity: MainActivity? = null

    companion object {

        private const val KEY_PASTE_LOCATION = "pasteLocation"
        private const val KEY_FILES = "files"

        /**
         * Show move / copy dialog on drop or perform the operation directly based on
         * remember preference selected by user previously in this dialog
         */
        fun showDialogOrPerformOperation(
            pasteLocation: String,
            files: ArrayList<HybridFileParcelable>,
            activity: MainActivity
        ) {
            val dragAndDropPref = activity.prefs
                .getInt(
                    PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
                    PreferencesConstants.PREFERENCE_DRAG_DEFAULT
                )
            if (dragAndDropPref == PreferencesConstants.PREFERENCE_DRAG_TO_MOVE_COPY) {
                val dragAndDropCopy = activity.prefs
                    .getString(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED, "")
                if (dragAndDropCopy != "") {
                    startCopyOrMoveTask(
                        pasteLocation, files,
                        PreferencesConstants.PREFERENCE_DRAG_REMEMBER_MOVE
                            .equals(dragAndDropCopy, ignoreCase = true),
                        activity
                    )
                } else {
                    val dragAndDropDialog = newInstance(pasteLocation, files)
                    dragAndDropDialog.show(
                        activity.supportFragmentManager,
                        javaClass.simpleName
                    )
                }
            } else {
                Log.w(
                    javaClass.simpleName,
                    "Trying to drop for copy / move while setting " +
                        "is drag select"
                )
            }
        }
        private fun newInstance(pasteLocation: String, files: ArrayList<HybridFileParcelable>):
            DragAndDropDialog {
                val dragAndDropDialog = DragAndDropDialog()
                val args = Bundle()
                args.putString(KEY_PASTE_LOCATION, pasteLocation)
                args.putParcelableArrayList(KEY_FILES, files)
                dragAndDropDialog.arguments = args
                return dragAndDropDialog
            }

        private fun startCopyOrMoveTask(
            pasteLocation: String,
            files: ArrayList<HybridFileParcelable>,
            move: Boolean,
            mainActivity: MainActivity
        ) {
            PrepareCopyTask(
                pasteLocation,
                move,
                mainActivity,
                mainActivity.isRootExplorer,
                mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode
            )
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, files)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pasteLocation = arguments?.getString(KEY_PASTE_LOCATION)
        operationFiles = arguments?.getParcelableArrayList(KEY_FILES)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        safeLet(
            context, mainActivity?.appTheme?.materialDialogTheme, mainActivity?.accent,
            pasteLocation, operationFiles
        ) {
            context, dialogTheme, accent, pasteLocation, operationFiles ->
            val dialog: MaterialDialog = MaterialDialog.Builder(context)
                .title(getString(R.string.choose_operation))
                .customView(R.layout.dialog_drag_drop, true)
                .theme(dialogTheme)
                .negativeText(getString(R.string.cancel).toUpperCase())
                .negativeColor(accent)
                .cancelable(false)
                .onNeutral { _: MaterialDialog?, _: DialogAction? ->
                    dismiss()
                }
                .build()

            dialog.customView?.run {
                // Get views from custom layout to set text values.
                val rememberCheckbox = this.findViewById<CheckBox>(R.id.remember_drag)
                val moveButton = this.findViewById<Button>(R.id.button_move)
                moveButton.setOnClickListener {
                    mainActivity?.run {
                        if (rememberCheckbox.isChecked) {
                            rememberDragOperation(true)
                        }
                        startCopyOrMoveTask(pasteLocation, operationFiles, true, this)
                        dismiss()
                    }
                }
                val copyButton = this.findViewById<Button>(R.id.button_copy)
                copyButton.setOnClickListener {
                    mainActivity?.run {
                        if (rememberCheckbox.isChecked) {
                            rememberDragOperation(false)
                        }
                        startCopyOrMoveTask(pasteLocation, operationFiles, false, this)
                        dismiss()
                    }
                }
                if (dialogTheme == Theme.LIGHT) {
                    moveButton.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_content_cut_24,
                        0, 0, 0
                    )
                    copyButton.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_content_copy_24,
                        0, 0, 0
                    )
                }
            }
            return dialog
        }
        Log.w(javaClass.simpleName, "Failed to show drag drop dialog view")
        return super.onCreateDialog(savedInstanceState)
    }

    override fun isCancelable(): Boolean {
        return false
    }

    private fun rememberDragOperation(shouldMove: Boolean) {
        mainActivity?.prefs?.edit()
            ?.putString(
                PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED,
                if (shouldMove) PreferencesConstants.PREFERENCE_DRAG_REMEMBER_MOVE
                else PreferencesConstants.PREFERENCE_DRAG_REMEMBER_COPY
            )?.apply()
    }
}
