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

package com.amaze.filemanager.filesystem;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Special immutable class for handling cut/copy operations.
 *
 * @author Emmanuel on 5/9/2017, at 09:59.
 */
public final class PasteHelper implements Parcelable {

  public static final int OPERATION_COPY = 0, OPERATION_CUT = 1;

  public final int operation;
  public final HybridFileParcelable[] paths;

  public PasteHelper(int op, HybridFileParcelable[] paths) {
    if (paths == null || paths.length == 0) throw new IllegalArgumentException();
    operation = op;
    this.paths = paths;
  }

  private PasteHelper(Parcel in) {
    operation = in.readInt();
    paths =
        (HybridFileParcelable[])
            in.readParcelableArray(HybridFileParcelable.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(operation);
    dest.writeParcelableArray(paths, 0);
  }

  public static final Parcelable.Creator CREATOR =
      new Parcelable.Creator() {
        public PasteHelper createFromParcel(Parcel in) {
          return new PasteHelper(in);
        }

        public PasteHelper[] newArray(int size) {
          return new PasteHelper[size];
        }
      };
}
