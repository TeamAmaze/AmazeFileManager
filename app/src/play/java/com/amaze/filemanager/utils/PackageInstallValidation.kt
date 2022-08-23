/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.files.FileUtils
import java.io.File

/**
 * Additional checks for package validity for installation via Amaze.
 *
 */
object PackageInstallValidation {

    /**
     * Perform validation by getting [PackageInfo] of file at specified path, then see if it's us.
     * If yes, throw [PackageCannotBeInstalledException] and [FileUtils.installApk] should exit
     * with Toast.
     */
    @JvmStatic
    @Throws(PackageCannotBeInstalledException::class, IllegalStateException::class)
    fun validatePackageInstallability(f: File) {
        AppConfig.getInstance().run {
            val packageInfo: PackageInfo? =
                packageManager.getPackageArchiveInfo(
                    f.absolutePath,
                    PackageManager.GET_ACTIVITIES
                )
            if (packageInfo == null) {
                throw IllegalStateException("Cannot get package info")
            } else {
                if (packageInfo.packageName == this.packageName) {
                    throw PackageCannotBeInstalledException(
                        "Cannot update myself per Google Play policy"
                    )
                }
            }
        }
    }

    /**
     * Exception indicating specified package cannot be installed
     */
    class PackageCannotBeInstalledException : Exception {
        constructor(reason: Throwable) : super(reason)
        constructor(reason: String) : super(reason)
    }
}
