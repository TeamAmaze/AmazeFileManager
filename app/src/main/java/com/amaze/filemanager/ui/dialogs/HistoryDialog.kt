package com.amaze.filemanager.ui.dialogs

import android.content.SharedPreferences
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.HiddenAdapter
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.ui.theme.AppTheme
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
                true)

        val materialDialog = MaterialDialog.Builder(mainActivity).also { builder ->
            builder.positiveText(R.string.cancel)
            builder.positiveColor(mainActivity.accent)
            builder.negativeText(R.string.clear)
            builder.negativeColor(mainActivity.accent)
            builder.title(R.string.history)
            builder.onNegative { _: MaterialDialog?, _: DialogAction? ->
                DataUtils.getInstance().clearHistory()
            }
            builder.theme(appTheme.getMaterialDialogTheme(mainFragment.requireContext()))
            builder.adapter(adapter, null)
        }.build()
        adapter.materialDialog = materialDialog
        materialDialog.show()
    }

}