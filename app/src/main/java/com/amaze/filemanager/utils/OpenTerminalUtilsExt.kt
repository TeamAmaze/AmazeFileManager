/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import com.amaze.filemanager.ui.activities.MainActivity

const val TERMONE_PLUS = "com.termoneplus"
const val ANDROID_TERM = "jackpal.androidterm"
const val TERMUX = "com.termux"

/**
 * Extension function to detect installed Terminal apps.
 *
 * Termux, Termone plus (Android terminal) and its predecessor by Jack Palovich are supported.
 */
fun MainActivity.detectInstalledTerminalApps(): Array<String> {
    val retval = ArrayList<String>()
    for (pkg in arrayOf(TERMONE_PLUS, ANDROID_TERM, TERMUX)) {
        packageManager.getLaunchIntentForPackage(pkg)?.run {
            val resolveInfos = packageManager.queryIntentActivitiesCompat(this, MATCH_DEFAULT_ONLY)
            if (resolveInfos.isNotEmpty()
            ) {
                retval.add(pkg)
            }
        }
    }
    return retval.toTypedArray()
}
