package com.amaze.filemanager.ui.dialogs

import androidx.annotation.Nullable
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity

/**
 * Alert Dialog.
 */
object AlertDialog {

    /**
     * Display an alert dialog. Optionally accepts a [MaterialDialog.SingleButtonCallback] to
     * provide additional behaviour when dialog button is pressed.
     *
     * Button default text is OK, but can be customized too.
     */
    @JvmStatic
    fun show(
        activity: ThemedActivity,
        @StringRes content: Int,
        @StringRes title: Int,
        @StringRes positiveButtonText: Int = android.R.string.ok,
        @Nullable onPositive: MaterialDialog.SingleButtonCallback? = null,
        contentIsHtml: Boolean = false
    ) {
        val accentColor: Int = activity.accent
        val a = MaterialDialog.Builder(activity)
            .content(content, contentIsHtml)
            .widgetColor(accentColor)
            .theme(
                activity
                    .appTheme
                    .getMaterialDialogTheme(activity.applicationContext)
            )
            .title(title)
            .positiveText(positiveButtonText)
            .positiveColor(accentColor)

        if (onPositive != null) {
            a.onPositive(onPositive)
        }
        a.build().show()
    }
}
