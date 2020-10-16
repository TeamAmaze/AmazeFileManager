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

package com.amaze.filemanager.test;

import static android.os.Build.VERSION_CODES.P;
import static org.robolectric.Shadows.shadowOf;

import java.io.File;

import org.robolectric.shadows.ShadowStorageManager;

import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

public class TestUtils {

  /**
   * Populate "internal device storage" to StorageManager with directory as provided by Robolectric.
   *
   * <p>Tests need storage access must call this on test case setup for SDK >= N to work.
   */
  public static void initializeInternalStorage() {
    Parcel parcel = Parcel.obtain();
    File dir = Environment.getExternalStorageDirectory();
    parcel.writeString("FS-internal");
    if (Build.VERSION.SDK_INT < P) parcel.writeInt(0);
    parcel.writeString(dir.getAbsolutePath());
    if (Build.VERSION.SDK_INT >= P) parcel.writeString(dir.getAbsolutePath());
    parcel.writeString("robolectric internal storage");
    parcel.writeInt(1);
    parcel.writeInt(0);
    parcel.writeInt(1);
    if (Build.VERSION.SDK_INT < P) parcel.writeLong(1024 * 1024);
    parcel.writeInt(0);
    parcel.writeLong(1024 * 1024);
    parcel.writeParcelable(UserHandle.getUserHandleForUid(0), 0);
    parcel.writeString("1234-5678");
    parcel.writeString(Environment.MEDIA_MOUNTED);
    addVolumeToStorageManager(parcel);
  }

  /**
   * Populate "external device storage" to StorageManager with directory as provided by Robolectric.
   *
   * <p>Tests need storage access must call this on test case setup for SDK >= N to work.
   */
  public static void initializeExternalStorage() {
    Parcel parcel = Parcel.obtain();
    File dir = Environment.getExternalStoragePublicDirectory("external");
    parcel.writeString("FS-external");
    if (Build.VERSION.SDK_INT < P) parcel.writeInt(0);
    parcel.writeString(dir.getAbsolutePath());
    if (Build.VERSION.SDK_INT >= P) parcel.writeString(dir.getAbsolutePath());
    parcel.writeString("robolectric external storage");
    parcel.writeInt(0);
    parcel.writeInt(1);
    parcel.writeInt(0);
    if (Build.VERSION.SDK_INT < P) parcel.writeLong(1024 * 1024);
    parcel.writeInt(0);
    parcel.writeLong(1024 * 1024);
    parcel.writeParcelable(UserHandle.getUserHandleForUid(0), 0);
    parcel.writeString("ABCD-EFGH");
    parcel.writeString(Environment.MEDIA_MOUNTED);
    addVolumeToStorageManager(parcel);
  }

  private static void addVolumeToStorageManager(@NonNull Parcel parcel) {
    parcel.setDataPosition(0);
    ShadowStorageManager storageManager =
        shadowOf(
            ApplicationProvider.getApplicationContext().getSystemService(StorageManager.class));
    StorageVolume volume = StorageVolume.CREATOR.createFromParcel(parcel);
    storageManager.addStorageVolume(volume);
  }
}
