package com.amaze.filemanager.ui.dialogs

import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
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
            theme(appTheme.getMaterialDialogTheme(main))
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
