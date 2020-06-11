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

package com.amaze.filemanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

/** Created by arpitkh996 on 16-01-2016. */
public class ComputerParcelable implements Parcelable {

  public final String addr;
  public final String name;

  public ComputerParcelable(String str, String str2) {
    this.name = str;
    this.addr = str2;
  }

  public static final Creator<ComputerParcelable> CREATOR =
      new Creator<ComputerParcelable>() {
        @Override
        public ComputerParcelable createFromParcel(Parcel in) {
          return new ComputerParcelable(in);
        }

        @Override
        public ComputerParcelable[] newArray(int size) {
          return new ComputerParcelable[size];
        }
      };

  public String toString() {
    return String.format("%s [%s]", this.name, this.addr);
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(this.name);
    parcel.writeString(this.addr);
  }

  public boolean equals(Object obj) {
    return obj instanceof ComputerParcelable
        && (this == obj
            || (this.name.equals(((ComputerParcelable) obj).name)
                && this.addr.equals(((ComputerParcelable) obj).addr)));
  }

  public int hashCode() {
    return this.name.hashCode() + this.addr.hashCode();
  }

  private ComputerParcelable(Parcel parcel) {
    this.name = parcel.readString();
    this.addr = parcel.readString();
  }
}
