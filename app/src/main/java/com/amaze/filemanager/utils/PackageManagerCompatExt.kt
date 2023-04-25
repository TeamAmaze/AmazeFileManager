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

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU

/**
 * Wraps [PackageManager.queryIntentActivities] to SDK compatibility.
 */
fun PackageManager.queryIntentActivitiesCompat(
    intent: Intent,
    resolveInfoFlags: Int,
): List<ResolveInfo> {
    return if (SDK_INT >= TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(resolveInfoFlags.toLong()))
    } else {
        queryIntentActivities(intent, resolveInfoFlags)
    }
}

/**
 * Wraps [PackageManager.getPackageInfo] to SDK compatibility.
 */
fun PackageManager.getPackageInfoCompat(
    pkg: String,
    packageInfoFlags: Int,
): PackageInfo {
    return if (SDK_INT >= TIRAMISU) {
        getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong()))
    } else {
        getPackageInfo(pkg, packageInfoFlags)
    }
}

/**
 * Wraps [PackageManager.getApplicationInfo] to SDK compatibility.
 */
fun PackageManager.getApplicationInfoCompat(
    pkg: String,
    applicationInfoFlags: Int,
): ApplicationInfo {
    return if (SDK_INT >= TIRAMISU) {
        getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(applicationInfoFlags.toLong()))
    } else {
        getApplicationInfo(pkg, applicationInfoFlags)
    }
}
