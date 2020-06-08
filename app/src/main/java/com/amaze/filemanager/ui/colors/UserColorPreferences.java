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

package com.amaze.filemanager.ui.colors;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;

public final class UserColorPreferences implements Parcelable {

  public final @ColorInt int primaryFirstTab, primarySecondTab, accent, iconSkin;

  public UserColorPreferences(
      @ColorInt int primaryFirstTab,
      @ColorInt int primarySecondTab,
      @ColorInt int accent,
      @ColorInt int iconSkin) {
    this.primaryFirstTab = primaryFirstTab;
    this.primarySecondTab = primarySecondTab;
    this.accent = accent;
    this.iconSkin = iconSkin;
  }

  private UserColorPreferences(Parcel in) {
    primaryFirstTab = in.readInt();
    primarySecondTab = in.readInt();
    accent = in.readInt();
    iconSkin = in.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(primaryFirstTab);
    dest.writeInt(primarySecondTab);
    dest.writeInt(accent);
    dest.writeInt(iconSkin);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<UserColorPreferences> CREATOR =
      new Creator<UserColorPreferences>() {
        @Override
        public UserColorPreferences createFromParcel(Parcel in) {
          return new UserColorPreferences(in);
        }

        @Override
        public UserColorPreferences[] newArray(int size) {
          return new UserColorPreferences[size];
        }
      };
}
