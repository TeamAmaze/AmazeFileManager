/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters.glide.cloudicon

import android.content.Context
import android.graphics.Bitmap
import com.amaze.filemanager.database.CloudHandler
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey

/** Created by Vishal Nehra on 3/27/2018.  */
class CloudIconModelLoader(private val context: Context) : ModelLoader<String, Bitmap> {
    override fun buildLoadData(
        s: String,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<Bitmap> {
        // we put key as current time since we're not disk caching the images for cloud,
        // as there is no way to differentiate input streams returned by different cloud services
        // for future instances and they don't expose concrete paths either
        return ModelLoader.LoadData(
            ObjectKey(System.currentTimeMillis()),
            CloudIconDataFetcher(context, s, width, height),
        )
    }

    override fun handles(s: String): Boolean {
        return (
            s.startsWith(CloudHandler.CLOUD_PREFIX_BOX) ||
                s.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX) ||
                s.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE) ||
                s.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE) ||
                s.startsWith("smb:/") ||
                s.startsWith("ssh:/")
        )
    }
}
