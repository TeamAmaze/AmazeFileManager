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

package com.amaze.filemanager.adapters.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/** Identifies a mounted volume */
public class StorageDirectoryParcelable implements Parcelable {
  @NonNull public final String path;
  @NonNull public final String name;
  public final @DrawableRes int iconRes;

  public StorageDirectoryParcelable(@NonNull String path, @NonNull String name, int iconRes) {
    this.path = path;
    this.name = name;
    this.iconRes = iconRes;
  }

  public StorageDirectoryParcelable(@NonNull Parcel im) {
    path = im.readString();
    name = im.readString();
    iconRes = im.readInt();
  }

  @NonNull
  @Override
  public String toString() {
    return "StorageDirectory(path=" + path + ", name=" + name + ", icon=" + iconRes + ")";
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(path);
    parcel.writeString(name);
    parcel.writeInt(iconRes);
  }

  public static final Creator<StorageDirectoryParcelable> CREATOR =
      new Creator<StorageDirectoryParcelable>() {
        public StorageDirectoryParcelable createFromParcel(Parcel in) {
          return new StorageDirectoryParcelable(in);
        }

        public StorageDirectoryParcelable[] newArray(int size) {
          return new StorageDirectoryParcelable[size];
        }
      };
}
