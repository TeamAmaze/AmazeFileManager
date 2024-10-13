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

package com.amaze.filemanager.utils

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatTextView
import androidx.drawerlayout.widget.DrawerLayout
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.PasteHelper
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation
import com.amaze.filemanager.ui.selection.SelectionPopupMenu.Companion.invokeSelectionDropdown
import java.io.File
import java.lang.ref.WeakReference

class MainActivityActionMode(private val mainActivityReference: WeakReference<MainActivity>) :
    ActionMode.Callback {
    var actionModeView: View? = null
    var actionMode: ActionMode? = null
    var pasteHelper: PasteHelper? = null

    private fun hideOption(
        id: Int,
        menu: Menu,
    ) {
        val item = menu.findItem(id)
        item.isVisible = false
    }

    private fun showOption(
        id: Int,
        menu: Menu,
    ) {
        val item = menu.findItem(id)
        item.isVisible = true
    }

    // called when the action mode is created; startActionMode() was called
    override fun onCreateActionMode(
        mode: ActionMode,
        menu: Menu,
    ): Boolean {
        // Inflate a menu resource providing context menu items
        val inflater = mode.menuInflater
        mainActivityReference.get()?.let {
                mainActivity ->
            actionModeView = mainActivity.layoutInflater.inflate(R.layout.actionmode, null)
            mode.customView = actionModeView
            mainActivity.setPagingEnabled(false)
            mainActivity.hideFab()
            if (mainActivity.mReturnIntent &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
            ) {
                mainActivity.showFabConfirmSelection()
            }

            // translates the drawable content down
            // if (mainActivity.isDrawerLocked) mainActivity.translateDrawerList(true);

            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu)
            hideOption(R.id.addshortcut, menu)
            hideOption(R.id.share, menu)
            hideOption(R.id.openwith, menu)
            // hideOption(R.id.setringtone,menu);
            mode.title = mainActivity.resources.getString(R.string.select)
            mainActivity
                .updateViews(
                    ColorDrawable(
                        mainActivity.resources
                            .getColor(R.color.holo_dark_action_mode),
                    ),
                )

            // do not allow drawer to open when item gets selected
            if (!mainActivity.drawer.isLocked) {
                mainActivity.drawer.lockIfNotOnTablet(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }
        return true
    }

    /**
     * the following method is called each time the action mode is shown. Always called after
     * onCreateActionMode, but may be called multiple times if the mode is invalidated.
     */
    override fun onPrepareActionMode(
        mode: ActionMode,
        menu: Menu,
    ): Boolean {
        safeLet(
            mainActivityReference.get(),
            mainActivityReference.get()?.currentMainFragment?.mainFragmentViewModel,
            mainActivityReference.get()?.currentMainFragment?.adapter,
        ) {
                mainActivity, mainFragmentViewModel, adapter ->
            val checkedItems: ArrayList<LayoutElementParcelable> =
                mainFragmentViewModel.getCheckedItems()
            actionModeView?.setOnClickListener {
                invokeSelectionDropdown(
                    adapter,
                    actionModeView!!,
                    mainFragmentViewModel.currentPath!!,
                    mainActivity,
                )
            }
            val textView: AppCompatTextView = actionModeView!!.findViewById(R.id.item_count)
            textView.text = checkedItems.size.toString()

            if (mainActivity.mReturnIntent &&
                !mainActivity.intent.getBooleanExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    false,
                )
            ) {
                // Only one item can be returned, so there should not be a "Select all" button
                hideOption(R.id.all, menu)
            } else {
                menu.findItem(R.id.all)
                    .setTitle(
                        if (checkedItems.size
                            == mainFragmentViewModel.folderCount +
                            mainFragmentViewModel.fileCount
                        ) {
                            R.string.deselect_all
                        } else {
                            R.string.select_all
                        },
                    )
            }

            if (mainFragmentViewModel.openMode != OpenMode.FILE &&
                mainFragmentViewModel.openMode != OpenMode.TRASH_BIN &&
                !mainFragmentViewModel.getIsCloudOpenMode()
            ) {
                hideOption(R.id.addshortcut, menu)
                hideOption(R.id.compress, menu)
                return true
            }
            // tv.setText(checkedItems.size());

            hideOption(R.id.openparent, menu)
            if (checkedItems.size == 1) {
                showOption(R.id.addshortcut, menu)
                showOption(R.id.openwith, menu)
                showOption(R.id.share, menu)
                if (mainFragmentViewModel.getCheckedItems()[0].isDirectory) {
                    hideOption(R.id.openwith, menu)
                    hideOption(R.id.share, menu)
                }
            } else {
                showOption(R.id.share, menu)
                for (e in mainFragmentViewModel.getCheckedItems()) {
                    if (e.isDirectory) {
                        hideOption(R.id.share, menu)
                        break
                    }
                }
                hideOption(R.id.openwith, menu)
                hideOption(R.id.addshortcut, menu)
            }
            if (mainFragmentViewModel.openMode != OpenMode.FILE) {
                hideOption(R.id.openwith, menu)
                hideOption(R.id.compress, menu)
                hideOption(R.id.hide, menu)
                hideOption(R.id.addshortcut, menu)
                if (mainFragmentViewModel.openMode == OpenMode.TRASH_BIN) {
                    hideOption(R.id.cpy, menu)
                    hideOption(R.id.cut, menu)
                    hideOption(R.id.share, menu)
                    hideOption(R.id.hide, menu)
                    hideOption(R.id.addshortcut, menu)
                    hideOption(R.id.ex, menu)
                    showOption(R.id.delete, menu)
                    showOption(R.id.restore, menu)
                }
            }
        }
        return true // Return false if nothing is done
    }

    // called when the user selects a contextual menu item
    override fun onActionItemClicked(
        mode: ActionMode,
        item: MenuItem,
    ): Boolean {
        mainActivityReference.get()?.currentMainFragment?.computeScroll()
        safeLet(
            mainActivityReference.get(),
            mainActivityReference
                .get()?.currentMainFragment?.mainFragmentViewModel?.getCheckedItems(),
        ) {
                mainActivity, checkedItems ->
            return when (item.itemId) {
                R.id.about -> {
                    val x = checkedItems[0]
                    mainActivity.currentMainFragment?.also {
                        GeneralDialogCreation.showPropertiesDialogWithPermissions(
                            x.generateBaseFile(),
                            x.permissions,
                            mainActivity,
                            it,
                            mainActivity.isRootExplorer,
                            mainActivity.utilsProvider.appTheme,
                        )
                    }
                    mode.finish()
                    true
                }
                R.id.delete -> {
                    GeneralDialogCreation.deleteFilesDialog(
                        mainActivity,
                        mainActivity,
                        checkedItems,
                        mainActivity.utilsProvider.appTheme,
                    )
                    true
                }
                R.id.restore -> {
                    GeneralDialogCreation.restoreFilesDialog(
                        mainActivity,
                        mainActivity,
                        checkedItems,
                        mainActivity.utilsProvider.appTheme,
                    )
                    true
                }
                R.id.share -> {
                    if (checkedItems.size > 100) {
                        Toast.makeText(
                            mainActivity,
                            mainActivity.resources.getString(R.string.share_limit),
                            Toast.LENGTH_SHORT,
                        )
                            .show()
                    } else {
                        mainActivity.currentMainFragment?.mainFragmentViewModel?.also {
                                mainFragmentViewModel ->
                            when (checkedItems[0].mode) {
                                OpenMode.DROPBOX, OpenMode.BOX, OpenMode.GDRIVE,
                                OpenMode.ONEDRIVE,
                                ->
                                    FileUtils.shareCloudFiles(
                                        checkedItems,
                                        checkedItems[0].mode,
                                        mainActivity,
                                    )
                                else -> {
                                    val arrayList = ArrayList<File>()
                                    for (e in checkedItems) {
                                        arrayList.add(File(e.desc))
                                    }
                                    FileUtils.shareFiles(
                                        arrayList,
                                        mainActivity,
                                        mainActivity.utilsProvider.appTheme,
                                        mainFragmentViewModel.accentColor,
                                    )
                                }
                            }
                        }
                    }
                    true
                }
                R.id.openparent -> {
                    mainActivity.currentMainFragment?.loadlist(
                        File(checkedItems[0].desc).parent,
                        false,
                        OpenMode.FILE,
                        false,
                    )

                    true
                }
                R.id.all -> {
                    safeLet(
                        mainActivity.currentMainFragment?.mainFragmentViewModel,
                        mainActivity.currentMainFragment?.adapter,
                    ) {
                            mainFragmentViewModel, adapter ->
                        if (adapter.areAllChecked(mainFragmentViewModel.currentPath)) {
                            adapter.toggleChecked(
                                false,
                                mainFragmentViewModel.currentPath,
                            )
                            item.setTitle(R.string.select_all)
                        } else {
                            adapter.toggleChecked(
                                true,
                                mainFragmentViewModel.currentPath,
                            )
                            item.setTitle(R.string.deselect_all)
                        }
                    }
                    mode.invalidate()
                    true
                }
                R.id.rename -> {
                    val f: HybridFileParcelable = checkedItems[0].generateBaseFile()
                    mainActivity.currentMainFragment?.rename(f)
                    mode.finish()
                    true
                }
                R.id.hide -> {
                    var i1 = 0
                    while (i1 < checkedItems.size) {
                        mainActivity.currentMainFragment?.hide(checkedItems[i1].desc)
                        i1++
                    }
                    mainActivity.currentMainFragment?.updateList(false)
                    mode.finish()
                    true
                }
                R.id.ex -> {
                    mainActivity.mainActivityHelper.extractFile(File(checkedItems[0].desc))
                    mode.finish()
                    true
                }
                R.id.cpy, R.id.cut -> {
                    val copies = arrayOfNulls<HybridFileParcelable>(checkedItems.size)
                    var i = 0
                    while (i < checkedItems.size) {
                        copies[i] = checkedItems[i].generateBaseFile()
                        i++
                    }
                    val op =
                        if (item.itemId == R.id.cpy) {
                            PasteHelper.OPERATION_COPY
                        } else {
                            PasteHelper.OPERATION_CUT
                        }
                    // Making sure we don't cause an IllegalArgumentException
                    // when passing copies to PasteHelper
                    if (copies.isNotEmpty()) {
                        pasteHelper = PasteHelper(mainActivity, op, copies)
                        mainActivity.paste = pasteHelper
                    }
                    mode.finish()
                    true
                }
                R.id.compress -> {
                    val copies1 = ArrayList<HybridFileParcelable>()
                    var i4 = 0
                    while (i4 < checkedItems.size) {
                        copies1.add(checkedItems[i4].generateBaseFile())
                        i4++
                    }
                    GeneralDialogCreation.showCompressDialog(
                        mainActivity,
                        copies1,
                        mainActivity.currentMainFragment?.mainFragmentViewModel?.currentPath,
                    )
                    mode.finish()
                    true
                }
                R.id.openwith -> {
                    FileUtils.openFile(
                        File(checkedItems[0].desc),
                        mainActivity,
                        mainActivity.prefs,
                    )
                    true
                }
                R.id.addshortcut -> {
                    Utils.addShortcut(
                        mainActivity,
                        mainActivity.componentName,
                        checkedItems[0],
                    )
                    mode.finish()
                    true
                }
                else -> false
            }
        }
        return false
    }

    // called when the user exits the action mode
    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        mainActivityReference.get()?.let {
                mainActivity ->
            mainActivity.listItemSelected = false

            // translates the drawer content up
            // if (mainActivity.isDrawerLocked) mainActivity.translateDrawerList(false);
            mainActivity.showFab()
            mainActivity.hideFabConfirmSelection()

            mainActivity.setPagingEnabled(true)
            safeLet(
                mainActivity.currentMainFragment?.mainFragmentViewModel,
                mainActivity.currentMainFragment?.adapter,
            ) {
                    mainFragmentViewModel, adapter ->
                adapter.toggleChecked(false, mainFragmentViewModel.currentPath)
                mainActivity
                    .updateViews(
                        ColorDrawable(
                            if (MainActivity.currentTab == 1) {
                                mainFragmentViewModel.primaryTwoColor
                            } else {
                                mainFragmentViewModel.primaryColor
                            },
                        ),
                    )
            }

            if (mainActivity.drawer.isLocked) {
                mainActivity.drawer.unlockIfNotOnTablet()
            }
        }
    }

    /**
     * Finishes the action mode
     */
    fun disableActionMode() {
        mainActivityReference.get()?.let {
            it.listItemSelected = false
            it.hideFabConfirmSelection()
        }
        actionMode?.finish()
        actionMode = null
    }
}
