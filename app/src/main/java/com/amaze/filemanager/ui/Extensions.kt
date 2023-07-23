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

package com.amaze.filemanager.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger(AppConfig::class.java)

/**
 * Marks a text input field as mandatory (appends * at end)
 *
 */
fun TextInputLayout.makeRequired() {
    hint = TextUtils.concat(hint, " *")
}

/**
 * Makes the [Activity] starting not crash in case the app is
 * not meant to deal with this kind of intent
 */
fun Context.startActivityCatchingSecurityException(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: SecurityException) {
        log.error("Error when starting activity: ", e)
        Toast.makeText(this, R.string.security_error, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Update the alias, based on if Amaze Utils is installed, then we disable the alias intent-filters
 * if not installed then we enable the amaze utilities alias
 */
fun Context.updateAUAlias(shouldEnable: Boolean) {
    val component = ComponentName(this, "com.amaze.filemanager.amazeutilsalias")
    if (!shouldEnable) {
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } else {
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

/**
 * Force keyboard pop up on focus
 */
fun AppCompatEditText.openKeyboard(context: Context) {
    this.requestFocus()

    this.postDelayed(
        {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(
                    this,
                    InputMethodManager.SHOW_IMPLICIT
                )
        },
        100
    )
}

/**
 * Hides view with fade animation
 */
fun View.hideFade(duration: Long) {
    this.animate().alpha(0f).duration = duration
    this.visibility = View.GONE
}

/**
 * Shows view with fade animation
 */
fun View.showFade(duration: Long) {
    this.animate().alpha(1f).duration = duration
    this.visibility = View.VISIBLE
}

/**
 * Extension function to check for activity in package manager before triggering code
 */
fun Intent.runIfDocumentsUIExists(context: Context, callback: Runnable) {
    if (this.resolveActivity(context.packageManager) != null) {
        callback.run()
    } else {
        AppConfig.toast(context, R.string.no_app_found_intent)
    }
}
