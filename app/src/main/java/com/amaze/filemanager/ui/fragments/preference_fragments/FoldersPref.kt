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

package com.amaze.filemanager.ui.fragments.preference_fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.widget.EditText
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.databinding.DialogTwoedittextsBinding
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.views.preference.PathSwitchPreference
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.SimpleTextWatcher
import java.util.*

/** @author Emmanuel on 17/4/2017, at 22:49.
 */
class FoldersPref : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private var sharedPrefs: SharedPreferences? = null
    private val position: MutableMap<Preference, Int> = HashMap()
    private var dataUtils: DataUtils? = null
    private var utilsHandler: UtilsHandler? = null

    private val mainActivity: ThemedActivity
        get() = requireActivity() as ThemedActivity

    private var _dialogBinding: DialogTwoedittextsBinding? = null
    private val dialogBinding: DialogTwoedittextsBinding
        get() = _dialogBinding!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        utilsHandler = AppConfig.getInstance().utilsHandler
        dataUtils = DataUtils.getInstance()
        _dialogBinding = DialogTwoedittextsBinding.inflate(LayoutInflater.from(requireContext()))

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.folders_prefs)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        findPreference<Preference>(PreferencesConstants.PREFERENCE_SHORTCUT)!!
            .onPreferenceClickListener = this
        for (i in dataUtils!!.books.indices) {
            val p = PathSwitchPreference(activity)
            p.title = dataUtils!!.books[i][0]
            p.summary = dataUtils!!.books[i][1]
            p.onPreferenceClickListener = this
            position[p] = i
            preferenceScreen.addPreference(p)
        }
    }

    override fun onResume() {
        super.onResume()
        onCreate(null)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (preference is PathSwitchPreference) {
            when (preference.lastItemClicked) {
                PathSwitchPreference.EDIT -> loadEditDialog(preference)
                PathSwitchPreference.DELETE -> loadDeleteDialog(preference)
                else -> {
                }
            }
        } else if (preference.key == PreferencesConstants.PREFERENCE_SHORTCUT) {
            if (preferenceScreen.preferenceCount
                >= findPreference<Preference>(PreferencesConstants.PREFERENCE_SHORTCUT)!!.order
            )
                findPreference<Preference>(PreferencesConstants.PREFERENCE_SHORTCUT)
                    ?.order = preferenceScreen.preferenceCount + 10
            loadCreateDialog()
        }
        return false
    }

    private fun loadCreateDialog() {
        val fab_skin = mainActivity.accent
        val v = dialogBinding.root
        dialogBinding.textInput1.hint = getString(R.string.name)
        dialogBinding.textInput2.hint = getString(R.string.directory)
        val txtShortcutName = dialogBinding.text1
        val txtShortcutPath = dialogBinding.text2
        val dialog = MaterialDialog.Builder(requireActivity())
            .title(R.string.create_bookmark)
            .theme(mainActivity.appTheme.materialDialogTheme)
            .positiveColor(fab_skin)
            .positiveText(R.string.create)
            .negativeColor(fab_skin)
            .negativeText(android.R.string.cancel)
            .customView(v, false)
            .build()
        dialog.getActionButton(DialogAction.POSITIVE).isEnabled = false
        disableButtonIfTitleEmpty(txtShortcutName, dialog)
        disableButtonIfNotPath(txtShortcutPath, dialog)
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                val p = PathSwitchPreference(getActivity())
                p.title = txtShortcutName.text
                p.summary = txtShortcutPath.text
                p.onPreferenceClickListener = this@FoldersPref
                position[p] = dataUtils!!.books.size
                preferenceScreen.addPreference(p)
                val values = arrayOf(
                    txtShortcutName.text.toString(),
                    txtShortcutPath.text.toString()
                )
                dataUtils!!.addBook(values)
                utilsHandler!!.saveToDatabase(
                    OperationData(
                        UtilsHandler.Operation.BOOKMARKS,
                        txtShortcutName.text.toString(),
                        txtShortcutPath.text.toString()
                    )
                )
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun loadEditDialog(p: PathSwitchPreference) {
        val fab_skin = mainActivity.accent
        val v = dialogBinding.root
        dialogBinding.textInput1.hint = getString(R.string.name)
        dialogBinding.textInput2.hint = getString(R.string.directory)
        val editText1 = dialogBinding.text1
        val editText2 = dialogBinding.text2
        editText1.setText(p.title)
        editText2.setText(p.summary)
        val dialog = MaterialDialog.Builder(mainActivity)
            .title(R.string.edit_bookmark)
            .theme(mainActivity.appTheme.materialDialogTheme)
            .positiveColor(fab_skin)
            .positiveText(getString(R.string.edit).toUpperCase()) // TODO: 29/4/2017 don't use toUpperCase()
            .negativeColor(fab_skin)
            .negativeText(android.R.string.cancel)
            .customView(v, false)
            .build()
        dialog.getActionButton(DialogAction.POSITIVE).isEnabled =
            FileUtils.isPathAccessible(editText2.text.toString(), sharedPrefs)
        disableButtonIfTitleEmpty(editText1, dialog)
        disableButtonIfNotPath(editText2, dialog)
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                val oldName = p.title.toString()
                val oldPath = p.summary.toString()
                dataUtils!!.removeBook(position[p]!!)
                position.remove(p)
                preferenceScreen.removePreference(p)
                p.title = editText1.text
                p.summary = editText2.text
                position[p] = position.size
                preferenceScreen.addPreference(p)
                val values = arrayOf(editText1.text.toString(), editText2.text.toString())
                dataUtils!!.addBook(values)
                AppConfig.getInstance()
                    .runInBackground {
                        utilsHandler!!.renameBookmark(
                            oldName,
                            oldPath,
                            editText1.text.toString(),
                            editText2.text.toString()
                        )
                    }
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun loadDeleteDialog(p: PathSwitchPreference) {
        val fab_skin = mainActivity.accent
        val dialog = MaterialDialog.Builder(mainActivity)
            .title(R.string.question_delete_bookmark)
            .theme(mainActivity.appTheme.materialDialogTheme)
            .positiveColor(fab_skin)
            .positiveText(getString(R.string.delete).toUpperCase()) // TODO: 29/4/2017 don't use toUpperCase(), 20/9,2017 why not?
            .negativeColor(fab_skin)
            .negativeText(android.R.string.cancel)
            .build()
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                dataUtils!!.removeBook(position[p]!!)
                utilsHandler!!.removeFromDatabase(
                    OperationData(
                        UtilsHandler.Operation.BOOKMARKS,
                        p.title.toString(),
                        p.summary.toString()
                    )
                )
                preferenceScreen.removePreference(p)
                position.remove(p)
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun disableButtonIfNotPath(path: EditText, dialog: MaterialDialog) {
        path.addTextChangedListener(
            object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled =
                        FileUtils.isPathAccessible(s.toString(), sharedPrefs)
                }
            })
    }

    private fun disableButtonIfTitleEmpty(title: EditText, dialog: MaterialDialog) {
        title.addTextChangedListener(
            object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled = title.length() > 0
                }
            })
    }
}
