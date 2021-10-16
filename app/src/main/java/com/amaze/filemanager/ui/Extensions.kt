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

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.amaze.filemanager.R
import com.google.android.material.textfield.TextInputLayout

private const val TAG = "ExtensionsKt"

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
        Log.e(TAG, "Error when starting activity: ", e)
        Toast.makeText(this, R.string.security_error, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Force keyboard pop up on focus
 */
fun EditText.openKeyboard(context: Context) {
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
