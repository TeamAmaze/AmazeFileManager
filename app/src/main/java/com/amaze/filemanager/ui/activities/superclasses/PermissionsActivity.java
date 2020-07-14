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

package com.amaze.filemanager.ui.activities.superclasses;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class PermissionsActivity extends ThemedActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback {

  public static final int PERMISSION_LENGTH = 2;
  public static final int STORAGE_PERMISSION = 0, INSTALL_APK_PERMISSION = 1;

  private OnPermissionGranted[] permissionCallbacks = new OnPermissionGranted[PERMISSION_LENGTH];

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == STORAGE_PERMISSION) {
      if (isGranted(grantResults)) {
        Utils.enableScreenRotation(this);
        permissionCallbacks[STORAGE_PERMISSION].onPermissionGranted();
        permissionCallbacks[STORAGE_PERMISSION] = null;
      } else {
        Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show();
        requestStoragePermission(permissionCallbacks[STORAGE_PERMISSION], false);
      }

    } else if (requestCode == INSTALL_APK_PERMISSION) {
      if (isGranted(grantResults)) {
        permissionCallbacks[INSTALL_APK_PERMISSION].onPermissionGranted();
        permissionCallbacks[INSTALL_APK_PERMISSION] = null;
      }
    }
  }

  public boolean checkStoragePermission() {
    // Verify that all required contact permissions have been granted.
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED;
  }

  public void requestStoragePermission(
      @NonNull final OnPermissionGranted onPermissionGranted, boolean isInitialStart) {
    Utils.disableScreenRotation(this);
    final MaterialDialog materialDialog =
        GeneralDialogCreation.showBasicDialog(
            this,
            R.string.grant_storage_permission,
            R.string.grantper,
            R.string.grant,
            R.string.cancel);
    materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(v -> finish());
    materialDialog.setCancelable(false);

    requestPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        STORAGE_PERMISSION,
        materialDialog,
        onPermissionGranted,
        isInitialStart);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public void requestInstallApkPermission(
      @NonNull final OnPermissionGranted onPermissionGranted, boolean isInitialStart) {
    final MaterialDialog materialDialog =
        GeneralDialogCreation.showBasicDialog(
            this,
            R.string.grant_apkinstall_permission,
            R.string.grantper,
            R.string.grant,
            R.string.cancel);
    materialDialog
        .getActionButton(DialogAction.NEGATIVE)
        .setOnClickListener(v -> materialDialog.dismiss());
    materialDialog.setCancelable(false);

    requestPermission(
        Manifest.permission.REQUEST_INSTALL_PACKAGES,
        INSTALL_APK_PERMISSION,
        materialDialog,
        onPermissionGranted,
        isInitialStart);
  }

  /**
   * Requests permission, overrides {@param rationale}'s POSITIVE button dialog action.
   *
   * @param permission The permission to ask for
   * @param code {@link #STORAGE_PERMISSION} or {@link #INSTALL_APK_PERMISSION}
   * @param rationale MaterialLayout to provide an additional rationale to the user if the
   *     permission was not granted and the user would benefit from additional context for the use
   *     of the permission. For example, if the request has been denied previously.
   * @param isInitialStart is the permission being requested for the first time in the application
   *     lifecycle
   */
  public void requestPermission(
      final String permission,
      final int code,
      @NonNull final MaterialDialog rationale,
      @NonNull final OnPermissionGranted onPermissionGranted,
      boolean isInitialStart) {
    permissionCallbacks[code] = onPermissionGranted;

    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
      rationale
          .getActionButton(DialogAction.POSITIVE)
          .setOnClickListener(
              v -> {
                ActivityCompat.requestPermissions(
                    PermissionsActivity.this, new String[] {permission}, code);
                rationale.dismiss();
              });
      rationale.show();
    } else if (isInitialStart) {
      ActivityCompat.requestPermissions(this, new String[] {permission}, code);
    } else {
      Snackbar.make(
              findViewById(R.id.content_frame),
              R.string.grantfailed,
              BaseTransientBottomBar.LENGTH_INDEFINITE)
          .setAction(
              R.string.grant,
              v ->
                  startActivity(
                      new Intent(
                          android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                          Uri.parse(String.format("package:%s", getPackageName())))))
          .show();
    }
  }

  private boolean isGranted(int[] grantResults) {
    return grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
  }

  public interface OnPermissionGranted {
    void onPermissionGranted();
  }
}
