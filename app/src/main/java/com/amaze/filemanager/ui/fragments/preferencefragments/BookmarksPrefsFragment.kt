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

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.databinding.DialogTwoedittextsBinding
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.fragments.data.BookmarkData
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.amaze.filemanager.ui.views.preference.PathSwitchPreference
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.SimpleTextWatcher

class BookmarksPrefsFragment : BasePrefsFragment() {
    override val title = R.string.show_bookmarks_pref
    private val bookmarksViewModel by viewModels<BookmarkPrefsViewModel>()

    companion object {
        private val dataUtils = DataUtils.getInstance()!!
    }

    private val itemOnEditListener = { it: PathSwitchPreference ->
        showBookmarkDialog(it, R.string.edit_bookmark, R.string.edit) { bookmarkData ->
            updateBookmark(
                it,
                bookmarkData.name,
                bookmarkData.path,
                AppConfig.getInstance().utilsHandler,
            )
        }
    }

    private val itemOnDeleteListener = { it: PathSwitchPreference ->
        showDeleteDialog(it)
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.bookmarks_prefs, rootKey)

        findPreference<Preference>("add_bookmarks")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                showBookmarkDialog(
                    title = R.string.create_bookmark,
                    positiveTxt = R.string.create,
                ) { bookmarkData ->
                    createBookmark(
                        bookmarkData.name,
                        bookmarkData.path,
                        AppConfig.getInstance().utilsHandler,
                    )
                }
                true
            }
        bookmarksViewModel.bookmarksList = findPreference("bookmarks_list")
        reload()
    }

    private fun reload() {
        for (p in bookmarksViewModel.position) {
            bookmarksViewModel.bookmarksList?.removePreference(p.key)
        }

        bookmarksViewModel.position.clear()
        for (i in dataUtils.books.indices) {
            val p = PathSwitchPreference(activity, itemOnEditListener, itemOnDeleteListener)
            p.title = dataUtils.books[i][0]
            p.summary = dataUtils.books[i][1]
            bookmarksViewModel.position[p] = i
            bookmarksViewModel.bookmarksList?.addPreference(p)
        }
    }

    private fun DialogTwoedittextsBinding.bookmarkData(): BookmarkData {
        return BookmarkData(
            text1.text.toString().trim(),
            text2.text.toString().trim(),
        )
    }

    private fun showBookmarkDialog(
        bookmark: PathSwitchPreference? = null,
        @StringRes title: Int,
        @StringRes positiveTxt: Int,
        action: (BookmarkData) -> Unit,
    ) {
        val isEdit = bookmark != null
        val fabSkin = activity.accent
        val binding =
            DialogTwoedittextsBinding.inflate(LayoutInflater.from(requireContext()))
        binding.textInput1.hint = getString(R.string.name)
        binding.textInput2.hint = getString(R.string.directory)
        val nameEt = binding.text1
        val pathEt = binding.text2
        bookmark?.let {
            nameEt.setText(it.title)
            pathEt.setText(it.summary)
        }
        val dialog =
            MaterialDialog.Builder(requireActivity())
                .title(title)
                .theme(activity.appTheme.getMaterialDialogTheme())
                .positiveColor(fabSkin)
                .positiveText(positiveTxt)
                .negativeColor(fabSkin)
                .negativeText(android.R.string.cancel)
                .customView(binding.root, false)
                .build()

        dialog.getActionButton(DialogAction.POSITIVE).isEnabled =
            if (isEdit) {
                FileUtils.isPathAccessible(pathEt.text.toString(), activity.prefs)
            } else {
                false
            }

        disableButtonIfTitleEmpty(nameEt, dialog)
        disableButtonIfNotPath(pathEt, dialog)

        WarnableTextInputValidator(
            requireContext(),
            nameEt,
            binding.textInput1,
            dialog.getActionButton(DialogAction.POSITIVE),
        ) {
            bookmarksViewModel.isValidBookmarkName(nameEt.text.toString())
        }
        WarnableTextInputValidator(
            requireContext(),
            pathEt,
            binding.textInput2,
            dialog.getActionButton(DialogAction.POSITIVE),
        ) {
            bookmarksViewModel.isValidBookmarkPath(
                nameEt.text.toString(),
                pathEt.text.toString(),
                dataUtils,
                activity.prefs,
            )
        }
        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener {
            val bookmarkData = binding.bookmarkData()
            val result = bookmarksViewModel.isValidBookmark(bookmarkData, dataUtils, activity.prefs)
            if (result.first != null) {
                Toast.makeText(
                    requireContext(),
                    getString(result.second.text),
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }
            action(bookmarkData)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun createBookmark(
        name: String,
        path: String,
        utilsHandler: UtilsHandler,
    ) {
        val preference =
            PathSwitchPreference(activity, itemOnEditListener, itemOnDeleteListener)

        preference.title = name
        preference.summary = path

        bookmarksViewModel.position[preference] = dataUtils.books.size
        bookmarksViewModel.bookmarksList?.addPreference(preference)

        dataUtils.addBook(arrayOf(name, path))

        utilsHandler.saveToDatabase(
            OperationData(
                UtilsHandler.Operation.BOOKMARKS,
                name,
                path,
            ),
        ).subscribe()
    }

    private fun updateBookmark(
        preference: PathSwitchPreference,
        newName: String,
        newPath: String,
        utilsHandler: UtilsHandler,
    ) {
        val oldName = preference.title.toString()
        val oldPath = preference.summary.toString()
        dataUtils.removeBook(bookmarksViewModel.position[preference]!!)
        bookmarksViewModel.position.remove(preference)
        bookmarksViewModel.bookmarksList?.removePreference(preference)

        preference.title = newName
        preference.summary = newPath

        bookmarksViewModel.position[preference] = bookmarksViewModel.position.size
        bookmarksViewModel.bookmarksList?.addPreference(preference)

        dataUtils.addBook(arrayOf(newName, newPath))

        AppConfig.getInstance().runInBackground {
            utilsHandler.renameBookmark(
                oldName,
                oldPath,
                newName,
                newPath,
            )
        }
    }

    private fun showDeleteDialog(p: PathSwitchPreference) {
        val fabSkin = activity.accent
        val utilsHandler = AppConfig.getInstance().utilsHandler

        val dialog =
            MaterialDialog.Builder(activity)
                .title(R.string.question_delete_bookmark)
                .theme(activity.appTheme.getMaterialDialogTheme())
                .positiveColor(fabSkin)
                .positiveText(getString(R.string.delete).uppercase()) // TODO: 29/4/2017 don't use toUpperCase(), 20/9,2017 why not?
                .negativeColor(fabSkin)
                .negativeText(android.R.string.cancel)
                .build()
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                dataUtils.removeBook(bookmarksViewModel.position[p]!!)
                utilsHandler.removeFromDatabase(
                    OperationData(
                        UtilsHandler.Operation.BOOKMARKS,
                        p.title.toString(),
                        p.summary.toString(),
                    ),
                )
                bookmarksViewModel.bookmarksList?.removePreference(p)
                bookmarksViewModel.position.remove(p)
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun disableButtonIfNotPath(
        path: AppCompatEditText,
        dialog: MaterialDialog,
    ) {
        path.addTextChangedListener(
            object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled =
                        FileUtils.isPathAccessible(s.toString(), activity.prefs)
                }
            },
        )
    }

    private fun disableButtonIfTitleEmpty(
        title: AppCompatEditText,
        dialog: MaterialDialog,
    ) {
        title.addTextChangedListener(
            object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled = title.length() > 0
                }
            },
        )
    }
}
