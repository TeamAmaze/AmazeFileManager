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

package com.amaze.filemanager.adapters.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes

/** Identifies a mounted volume  */
data class StorageDirectoryParcelable(
    @JvmField
    val path: String,
    @JvmField
    val name: String,
    @JvmField
    @DrawableRes
    val iconRes: Int
) : Parcelable {

    constructor(im: Parcel) : this(
        path = im.readString()!!,
        name = im.readString()!!,
        iconRes = im.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(path)
        parcel.writeString(name)
        parcel.writeInt(iconRes)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<StorageDirectoryParcelable> {
            override fun createFromParcel(parcel: Parcel): StorageDirectoryParcelable {
                return StorageDirectoryParcelable(parcel)
            }

            override fun newArray(size: Int): Array<StorageDirectoryParcelable?> {
                return arrayOfNulls(size)
            }
        }
    }
}
