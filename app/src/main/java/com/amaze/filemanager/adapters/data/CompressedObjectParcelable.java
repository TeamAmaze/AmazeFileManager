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

import java.util.Comparator;

import com.amaze.filemanager.ui.icons.Icons;

import android.os.Parcel;
import android.os.Parcelable;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 20/11/2017, at 15:26. */
public class CompressedObjectParcelable implements Parcelable {
  public static final int TYPE_GOBACK = -1, TYPE_NORMAL = 0;

  public final boolean directory;
  public final int type;
  public final String path;
  public final String name;
  public final long date, size;
  public final int filetype;
  public final IconDataParcelable iconData;

  public CompressedObjectParcelable(String path, long date, long size, boolean directory) {
    this.directory = directory;
    this.type = TYPE_NORMAL;
    this.path = path;
    this.name = getNameForPath(path);
    this.date = date;
    this.size = size;
    this.filetype = Icons.getTypeOfFile(path, directory);
    this.iconData =
        new IconDataParcelable(IconDataParcelable.IMAGE_RES, Icons.loadMimeIcon(path, directory));
  }

  /** TYPE_GOBACK instance */
  public CompressedObjectParcelable() {
    this.directory = true;
    this.type = TYPE_GOBACK;
    this.path = null;
    this.name = null;
    this.date = 0;
    this.size = 0;
    this.filetype = -1;
    this.iconData = null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel p1, int p2) {
    p1.writeInt(type);
    if (type != TYPE_GOBACK) {
      p1.writeInt(directory ? 1 : 0);
      p1.writeString(path);
      p1.writeString(name);
      p1.writeLong(size);
      p1.writeLong(date);
      p1.writeInt(filetype);
      p1.writeParcelable(iconData, 0);
    }
  }

  public static final Parcelable.Creator<CompressedObjectParcelable> CREATOR =
      new Parcelable.Creator<CompressedObjectParcelable>() {
        public CompressedObjectParcelable createFromParcel(Parcel in) {
          return new CompressedObjectParcelable(in);
        }

        public CompressedObjectParcelable[] newArray(int size) {
          return new CompressedObjectParcelable[size];
        }
      };

  private CompressedObjectParcelable(Parcel im) {
    type = im.readInt();
    if (type == TYPE_GOBACK) {
      directory = true;
      path = null;
      name = null;
      date = 0;
      size = 0;
      filetype = -1;
      iconData = null;
    } else {
      directory = im.readInt() == 1;
      path = im.readString();
      name = im.readString();
      size = im.readLong();
      date = im.readLong();
      filetype = im.readInt();
      iconData = im.readParcelable(IconDataParcelable.class.getClassLoader());
    }
  }

  public static class Sorter implements Comparator<CompressedObjectParcelable> {
    @Override
    public int compare(CompressedObjectParcelable file1, CompressedObjectParcelable file2) {
      if (file1.type == CompressedObjectParcelable.TYPE_GOBACK) return -1;
      else if (file2.type == CompressedObjectParcelable.TYPE_GOBACK) return 1;
      else if (file1.directory && !file2.directory) {
        return -1;
      } else if (file2.directory && !(file1).directory) {
        return 1;
      } else return file1.path.compareToIgnoreCase(file2.path);
    }
  }

  private String getNameForPath(String path) {
    if (path.isEmpty()) return "";

    final StringBuilder stringBuilder = new StringBuilder(path);
    if (stringBuilder.charAt(path.length() - 1) == '/')
      stringBuilder.deleteCharAt(path.length() - 1);

    try {
      return stringBuilder.substring(stringBuilder.lastIndexOf("/") + 1);
    } catch (StringIndexOutOfBoundsException e) {
      return path.substring(0, path.lastIndexOf("/"));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CompressedObjectParcelable) {
      CompressedObjectParcelable otherObj = (CompressedObjectParcelable) obj;
      return name.equals(otherObj.name)
          && type == otherObj.type
          && directory == otherObj.directory
          && size == otherObj.size;
    } else return false;
  }
}
