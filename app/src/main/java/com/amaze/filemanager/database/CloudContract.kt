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
package com.amaze.filemanager.database

import android.net.Uri
import androidx.core.net.toUri
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.utils.cloud.CloudPluginUtil.resolvePluginIdFrom

/** Created by vishal on 19/4/17.  */
object CloudContract {
    const val CLOUD_PREFIX_BOX = "box:/"
    const val CLOUD_PREFIX_DROPBOX = "dropbox:/"
    const val CLOUD_PREFIX_GOOGLE_DRIVE = "gdrive:/"
    const val CLOUD_PREFIX_ONE_DRIVE = "onedrive:/"

    const val CLOUD_NAME_GOOGLE_DRIVE = "Google Drive™"
    const val CLOUD_NAME_DROPBOX = "Dropbox"
    const val CLOUD_NAME_ONE_DRIVE = "One Drive"
    const val CLOUD_NAME_BOX = "Box"

    const val APP_PACKAGE_NAME: String = "com.amaze.cloud"
    const val PROVIDER_AUTHORITY: String = "com.amaze.cloud.provider"
    const val PERMISSION_PROVIDER: String = "com.amaze.cloud.permission.ACCESS_PROVIDER"

    const val DATABASE_NAME: String = "keys.db"
    const val TABLE_NAME: String = "secret_keys"
    const val COLUMN_ID: String = "_id"
    const val COLUMN_CLIENT_ID: String = "client_id"
    const val COLUMN_CLIENT_SECRET_KEY: String = "client_secret"

    val URI: Uri =
        Uri.withAppendedPath(
            "content://$PROVIDER_AUTHORITY".toUri(),
            "keys.db/secret_keys",
        )

    val PROJECTION = arrayOf(COLUMN_ID, COLUMN_CLIENT_ID, COLUMN_CLIENT_SECRET_KEY)

    val ENABLED_PROVIDERS =
        arrayOf(
            OpenMode.GDRIVE,
            OpenMode.DROPBOX,
            OpenMode.ONEDRIVE,
            OpenMode.BOX,
        )

    val ENABLED_PROVIDER_IDS: Array<String> =
        ENABLED_PROVIDERS.map {
            resolvePluginIdFrom(it).toString()
        }.toTypedArray()
}
