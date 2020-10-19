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

package com.amaze.filemanager.file_operations.filesystem;

import java.io.File;

import android.content.Context;

import androidx.annotation.IntDef;

public final class StorageNaming {

  public static final int STORAGE_INTERNAL = 0;
  public static final int STORAGE_SD_CARD = 1;
  public static final int ROOT = 2;
  public static final int NOT_KNOWN = 3;

  @IntDef({STORAGE_INTERNAL, STORAGE_SD_CARD, ROOT, NOT_KNOWN})
  public @interface DeviceDescription {}

  /** Retrofit of {@link android.os.storage.StorageVolume#getDescription(Context)} to older apis */
  public static @DeviceDescription int getDeviceDescriptionLegacy(File file) {
    String path = file.getPath();

    switch (path) {
      case "/storage/emulated/legacy":
      case "/storage/emulated/0":
      case "/mnt/sdcard":
        return STORAGE_INTERNAL;
      case "/storage/sdcard":
      case "/storage/sdcard1":
        return STORAGE_SD_CARD;
      case "/":
        return ROOT;
      default:
        return NOT_KNOWN;
    }
  }
}
