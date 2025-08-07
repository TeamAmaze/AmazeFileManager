package com.amaze.filemanager.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION_CODES.O

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
    flag: Int = 0x4,
) {
    if (Build.VERSION.SDK_INT >= O) {
        this.registerReceiver(broadcastReceiver, intentFilter, flag)
    } else {
        this.registerReceiver(broadcastReceiver, intentFilter)
    }
}
