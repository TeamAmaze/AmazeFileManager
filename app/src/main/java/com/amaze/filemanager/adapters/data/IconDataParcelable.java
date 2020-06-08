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

/**
 * Saves data on what should be loaded as an icon for LayoutElementParcelable
 *
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 6/12/2017, at 17:52.
 */
public class IconDataParcelable implements Parcelable {

  public static final int IMAGE_RES = 0, IMAGE_FROMFILE = 1, IMAGE_FROMCLOUD = 2;

  public final int type;
  public final String path;
  public final @DrawableRes int image;
  public final @DrawableRes int loadingImage;
  private boolean isImageBroken = false;

  public IconDataParcelable(int type, @DrawableRes int img) {
    if (type == IMAGE_FROMFILE) throw new IllegalArgumentException();
    this.type = type;
    this.image = img;
    this.loadingImage = -1;
    this.path = null;
  }

  public IconDataParcelable(int type, String path, @DrawableRes int loadingImages) {
    if (type == IMAGE_RES) throw new IllegalArgumentException();
    this.type = type;
    this.path = path;
    this.loadingImage = loadingImages;
    this.image = -1;
  }

  public boolean isImageBroken() {
    return isImageBroken;
  }

  public void setImageBroken(boolean imageBroken) {
    isImageBroken = imageBroken;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(type);
    parcel.writeString(path);
    parcel.writeInt(image);
    parcel.writeInt(loadingImage);
    parcel.writeInt(isImageBroken ? 1 : 0);
  }

  public IconDataParcelable(Parcel im) {
    type = im.readInt();
    path = im.readString();
    image = im.readInt();
    loadingImage = im.readInt();
    isImageBroken = im.readInt() == 1;
  }

  public static final Parcelable.Creator<IconDataParcelable> CREATOR =
      new Parcelable.Creator<IconDataParcelable>() {
        public IconDataParcelable createFromParcel(Parcel in) {
          return new IconDataParcelable(in);
        }

        public IconDataParcelable[] newArray(int size) {
          return new IconDataParcelable[size];
        }
      };
}
