package com.amaze.filemanager.activities.superclasses;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;

public class PermissionsActivity extends ThemedActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int PERMISSION_LENGTH = 2;
    public static final int STORAGE_PERMISSION = 0, INSTALL_APK_PERMISSION = 1;

    private OnPermissionGranted[] permissionCallbacks = new OnPermissionGranted[PERMISSION_LENGTH];

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION) {
            if (isGranted(grantResults)) {
                permissionCallbacks[STORAGE_PERMISSION].onPermissionGranted();
                permissionCallbacks[STORAGE_PERMISSION] = null;
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show();
                requestStoragePermission(permissionCallbacks[STORAGE_PERMISSION]);
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

    public void requestStoragePermission(@NonNull final OnPermissionGranted onPermissionGranted) {
        final MaterialDialog materialDialog = GeneralDialogCreation.showBasicDialog(this,
                R.string.grant_storage_permission, R.string.grantper, R.string.grant, R.string.cancel);
        materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(v -> finish());
        materialDialog.setCancelable(false);

        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION,
                materialDialog, onPermissionGranted);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestInstallApkPermission(@NonNull final OnPermissionGranted onPermissionGranted) {
        final MaterialDialog materialDialog = GeneralDialogCreation.showBasicDialog(this,
                R.string.grant_apkinstall_permission, R.string.grantper, R.string.grant, R.string.cancel);
        materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(v -> materialDialog.dismiss());
        materialDialog.setCancelable(false);

        requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES, INSTALL_APK_PERMISSION,
                materialDialog, onPermissionGranted);
    }

    /**
     * Requests permission, overrides {@param rationale}'s POSITIVE button dialog action.
     * @param permission The permission to ask for
     * @param code {@link #STORAGE_PERMISSION} or {@link #INSTALL_APK_PERMISSION}
     * @param rationale MaterialLayout to provide an additional rationale to the user if the permission was not granted
     *                  and the user would benefit from additional context for the use of the permission.
     *                  For example, if the request has been denied previously.
     */
    public void requestPermission(final String permission, final int code, @NonNull final MaterialDialog rationale,
                                  @NonNull final OnPermissionGranted onPermissionGranted) {
        permissionCallbacks[code] = onPermissionGranted;

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            rationale.getActionButton(DialogAction.POSITIVE).setOnClickListener(v -> {
                ActivityCompat.requestPermissions(PermissionsActivity.this, new String[]{permission}, code);
                rationale.dismiss();
            });
            rationale.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, code);
        }
    }

    private boolean isGranted(int[] grantResults) {
        return grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    public interface OnPermissionGranted {
        void onPermissionGranted();
    }

}
