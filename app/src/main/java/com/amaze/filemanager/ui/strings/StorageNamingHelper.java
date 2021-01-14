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

package com.amaze.filemanager.ui.strings;

import java.io.File;

import com.amaze.filemanager.R;
import com.amaze.filemanager.file_operations.filesystem.StorageNaming;

import android.content.Context;

import androidx.annotation.NonNull;

public final class StorageNamingHelper {
  private StorageNamingHelper() {}

  @NonNull
  public static String getNameForDeviceDescription(
      @NonNull Context context,
      @NonNull File file,
      @StorageNaming.DeviceDescription int deviceDescription) {
    switch (deviceDescription) {
      case StorageNaming.STORAGE_INTERNAL:
        return context.getString(R.string.storage_internal);
      case StorageNaming.STORAGE_SD_CARD:
        return context.getString(R.string.storage_sd_card);
      case StorageNaming.ROOT:
        return context.getString(R.string.root_directory);
      case StorageNaming.NOT_KNOWN:
      default:
        return file.getName();
    }
  }
}
