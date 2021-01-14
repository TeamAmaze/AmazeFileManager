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

import java.io.File;
import java.util.Calendar;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.utils.Utils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class LayoutElementParcelable implements Parcelable {

  private static final String CURRENT_YEAR =
      String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

  public final boolean isBack;
  public final int filetype;
  public final IconDataParcelable iconData;
  public final String title;
  public final String desc;
  public final String permissions;
  public final String symlink;
  public final String size;
  public final boolean isDirectory;
  public final long date, longSize;
  public final String dateModification;
  public final boolean header;

  // same as hfile.modes but different than openmode in Main.java
  private OpenMode mode = OpenMode.FILE;

  public LayoutElementParcelable(
      @NonNull Context c, boolean isBack, String goback, boolean showThumbs) {
    this(
        c,
        true,
        new File("..").getName(),
        "..",
        "",
        "",
        goback,
        0,
        false,
        "",
        true,
        showThumbs,
        OpenMode.UNKNOWN);
  }

  public LayoutElementParcelable(
      @NonNull Context c,
      String path,
      String permissions,
      String symlink,
      String size,
      long longSize,
      boolean header,
      String date,
      boolean isDirectory,
      boolean useThumbs,
      OpenMode openMode) {
    this(
        c,
        new File(path).getName(),
        path,
        permissions,
        symlink,
        size,
        longSize,
        header,
        date,
        isDirectory,
        useThumbs,
        openMode);
  }

  public LayoutElementParcelable(
      @NonNull Context c,
      String title,
      String path,
      String permissions,
      String symlink,
      String size,
      long longSize,
      boolean header,
      String date,
      boolean isDirectory,
      boolean useThumbs,
      OpenMode openMode) {
    this(
        c,
        false,
        title,
        path,
        permissions,
        symlink,
        size,
        longSize,
        header,
        date,
        isDirectory,
        useThumbs,
        openMode);
  }

  public LayoutElementParcelable(
      @NonNull Context c,
      boolean isBack,
      String title,
      String path,
      String permissions,
      String symlink,
      String size,
      long longSize,
      boolean header,
      String date,
      boolean isDirectory,
      boolean useThumbs,
      OpenMode openMode) {
    filetype = Icons.getTypeOfFile(path, isDirectory);
    @DrawableRes int fallbackIcon = Icons.loadMimeIcon(path, isDirectory);
    this.mode = openMode;
    if (useThumbs) {
      switch (mode) {
        case SMB:
        case SFTP:
        case DROPBOX:
        case GDRIVE:
        case ONEDRIVE:
        case BOX:
          if (!isDirectory
              && (filetype == Icons.IMAGE || filetype == Icons.VIDEO || filetype == Icons.APK)) {
            this.iconData =
                new IconDataParcelable(IconDataParcelable.IMAGE_FROMCLOUD, path, fallbackIcon);
          } else {
            this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_RES, fallbackIcon);
          }
          break;
        default:
          if (filetype == Icons.IMAGE || filetype == Icons.VIDEO || filetype == Icons.APK) {
            this.iconData =
                new IconDataParcelable(IconDataParcelable.IMAGE_FROMFILE, path, fallbackIcon);
          } else {
            this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_RES, fallbackIcon);
          }
      }
    } else {
      this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_RES, fallbackIcon);
    }

    this.title = title;
    this.desc = path;
    this.permissions = permissions.trim();
    this.symlink = symlink.trim();
    this.size = size;
    this.header = header;
    this.longSize = longSize;
    this.isDirectory = isDirectory;
    if (!date.trim().equals("")) {
      this.date = Long.parseLong(date);
      this.dateModification = Utils.getDate(c, this.date);
    } else {
      this.date = 0;
      this.dateModification = "";
    }
    this.isBack = isBack;
  }

  public OpenMode getMode() {
    return mode;
  }

  public void setMode(OpenMode mode) {
    this.mode = mode;
  }

  public HybridFileParcelable generateBaseFile() {
    HybridFileParcelable baseFile =
        new HybridFileParcelable(desc, permissions, date, longSize, isDirectory);
    baseFile.setMode(mode);
    baseFile.setName(title);
    return baseFile;
  }

  public boolean hasSymlink() {
    return symlink != null && symlink.length() != 0;
  }

  @Override
  public String toString() {
    return title + "\n" + desc;
  }

  // Hopefully it should be safe - nobody else is using this
  public LayoutElementParcelable(Parcel im) {
    filetype = im.readInt();
    iconData = im.readParcelable(IconDataParcelable.class.getClassLoader());
    title = im.readString();
    desc = im.readString();
    permissions = im.readString();
    symlink = im.readString();
    int j = im.readInt();
    date = im.readLong();
    int i = im.readInt();
    header = i != 0;
    isDirectory = j != 0;
    dateModification = im.readString();
    size = im.readString();
    longSize = im.readLong();
    isBack = im.readInt() != 0;
  }

  @Override
  public int describeContents() {
    // TODO: Implement this method
    return 0;
  }

  @Override
  public void writeToParcel(Parcel p1, int p2) {
    p1.writeInt(filetype);
    p1.writeParcelable(iconData, 0);
    p1.writeString(title);
    p1.writeString(desc);
    p1.writeString(permissions);
    p1.writeString(symlink);
    p1.writeInt(isDirectory ? 1 : 0);
    p1.writeLong(date);
    p1.writeInt(header ? 1 : 0);
    p1.writeString(dateModification);
    p1.writeString(size);
    p1.writeLong(longSize);
    p1.writeInt(isBack ? 1 : 0);
  }

  public static final Parcelable.Creator<LayoutElementParcelable> CREATOR =
      new Parcelable.Creator<LayoutElementParcelable>() {
        public LayoutElementParcelable createFromParcel(Parcel in) {
          return new LayoutElementParcelable(in);
        }

        public LayoutElementParcelable[] newArray(int size) {
          return new LayoutElementParcelable[size];
        }
      };
}
