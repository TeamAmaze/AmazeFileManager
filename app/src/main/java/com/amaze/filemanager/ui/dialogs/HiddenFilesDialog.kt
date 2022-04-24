package com.amaze.filemanager.ui.dialogs

import android.graphics.Color
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.HiddenAdapter
import com.amaze.filemanager.file_operations.filesystem.OpenMode
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
                false)

        val materialDialog = MaterialDialog.Builder(mainActivity).also { builder ->
            builder.positiveText(R.string.close)
            builder.positiveColor(mainActivity.accent)
            builder.title(R.string.hiddenfiles)
            builder.theme(appTheme.getMaterialDialogTheme(mainFragment.requireContext()))
            builder.autoDismiss(true)
            builder.adapter(adapter, null)
            builder.dividerColor(Color.GRAY)
        }.build()

        adapter.materialDialog = materialDialog
        materialDialog.setOnDismissListener {
            mainFragment.loadlist(mainFragment.currentPath, false, OpenMode.UNKNOWN)
        }
        materialDialog.show()
    }
}