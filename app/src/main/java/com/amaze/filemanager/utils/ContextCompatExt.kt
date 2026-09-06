/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION_CODES.O
import androidx.core.content.ContextCompat

/**
 * Context.registerReceiver() for SDK compatibility.
 *
 * Without additional checks in original ContextCompat to prevent breaking Roboletric tests.
 * See https://github.com/robolectric/robolectric/issues/9124
 */
@SuppressLint("WrongConstant")
fun Context.registerReceiverCompat(
    broadcastReceiver: BroadcastReceiver,
    intentFilter: IntentFilter,
    flag: Int = ContextCompat.RECEIVER_NOT_EXPORTED,
) {
    if (Build.VERSION.SDK_INT >= O) {
        this.registerReceiver(broadcastReceiver, intentFilter, flag)
    } else {
        this.registerReceiver(broadcastReceiver, intentFilter)
    }
}
