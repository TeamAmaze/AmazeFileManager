package com.amaze.filemanager.utils

import android.content.pm.PackageManager

class PackageUtils {

    companion object {
        fun appInstalledOrNot(uri: String, pm: PackageManager): Boolean {
            return try {
                pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}