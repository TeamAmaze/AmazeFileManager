/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
package com.amaze.filemanager.ui.activities.superclasses

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation
import com.amaze.filemanager.utils.Utils
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

open class PermissionsActivity :
    ThemedActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {
    private val permissionCallbacks: Array<(() -> Unit)?> = arrayOfNulls(PERMISSION_LENGTH)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION) {
            if (isGranted(grantResults)) {
                Utils.enableScreenRotation(this)
                permissionCallbacks[STORAGE_PERMISSION]?.invoke()
                permissionCallbacks[STORAGE_PERMISSION] = null
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show()
                requestStoragePermission(permissionCallbacks[STORAGE_PERMISSION]!!, false)
            }
        } else if (requestCode == NOTIFICATION_PERMISSION && VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            if (isGranted(grantResults)) {
                Utils.enableScreenRotation(this)
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show()
                requestNotificationPermission(false)
            }
        } else if (requestCode == INSTALL_APK_PERMISSION) {
            if (isGranted(grantResults)) {
                permissionCallbacks[INSTALL_APK_PERMISSION]?.invoke()
                permissionCallbacks[INSTALL_APK_PERMISSION] = null
            }
        }
    }

    /**
     * Check and prompt user to grant storage permission.
     */
    fun checkStoragePermission(): Boolean {
        // Verify that all required contact permissions have been granted.
        return if (VERSION.SDK_INT >= VERSION_CODES.R) {
            (
                (
                    ActivityCompat.checkSelfPermission(
                        this,
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    )
                        == PackageManager.PERMISSION_GRANTED
                ) ||
                    (
                        ActivityCompat.checkSelfPermission(
                            this,
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                        )
                            == PackageManager.PERMISSION_GRANTED
                    ) ||
                    Environment.isExternalStorageManager()
            )
        } else {
            (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
                    == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    /**
     * Check and prompt user to grant notification permission. For Android >= 8.
     */
    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun checkNotificationPermission(): Boolean {
        return (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    /**
     * Request notification permission.
     */
    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission(isInitialStart: Boolean) {
        Utils.disableScreenRotation(this)
        val materialDialog =
            GeneralDialogCreation.showBasicDialog(
                this,
                R.string.grant_notification_permission,
                R.string.grantper,
                R.string.grant,
                R.string.cancel,
            )
        materialDialog.getActionButton(DialogAction.NEGATIVE)
            .setOnClickListener { v: View? -> finish() }
        materialDialog.setCancelable(false)

        requestPermission(
            Manifest.permission.POST_NOTIFICATIONS,
            NOTIFICATION_PERMISSION,
            materialDialog,
            { },
            isInitialStart,
        )
    }

    /**
     * Request storage permission.
     */
    fun requestStoragePermission(
        onPermissionGranted: (() -> Unit),
        isInitialStart: Boolean,
    ) {
        Utils.disableScreenRotation(this)
        val materialDialog =
            GeneralDialogCreation.showBasicDialog(
                this,
                R.string.grant_storage_permission,
                R.string.grantper,
                R.string.grant,
                R.string.cancel,
            )
        materialDialog.getActionButton(DialogAction.NEGATIVE)
            .setOnClickListener { v: View? -> finish() }
        materialDialog.setCancelable(false)

        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            STORAGE_PERMISSION,
            materialDialog,
            onPermissionGranted,
            isInitialStart,
        )
    }

    /**
     * Request install app permission. For Android >= 6.
     */
    @RequiresApi(api = VERSION_CODES.M)
    fun requestInstallApkPermission(
        onPermissionGranted: (() -> Unit),
        isInitialStart: Boolean,
    ) {
        val materialDialog =
            GeneralDialogCreation.showBasicDialog(
                this,
                R.string.grant_apkinstall_permission,
                R.string.grantper,
                R.string.grant,
                R.string.cancel,
            )
        materialDialog
            .getActionButton(DialogAction.NEGATIVE)
            .setOnClickListener { v: View? -> materialDialog.dismiss() }
        materialDialog.setCancelable(false)

        requestPermission(
            Manifest.permission.REQUEST_INSTALL_PACKAGES,
            INSTALL_APK_PERMISSION,
            materialDialog,
            onPermissionGranted,
            isInitialStart,
        )
    }

    /**
     * Request terminal app permission. Probably dialog won't popup as it's 3rd party permissions,
     * but does prompt user to grant if not granted yet.
     */
    fun requestTerminalPermission(
        permission: String,
        onPermissionGranted: (() -> Unit),
    ) {
        if (ContextCompat.checkSelfPermission(
                this,
                permission,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted.invoke()
        } else {
            val materialDialog =
                GeneralDialogCreation.showBasicDialog(
                    this,
                    R.string.grant_terminal_permission,
                    R.string.grantper,
                    R.string.grant,
                    R.string.cancel,
                )
            materialDialog
                .getActionButton(DialogAction.NEGATIVE)
                .setOnClickListener { v: View? -> materialDialog.dismiss() }
            materialDialog.setCancelable(false)
            requestPermission(
                permission,
                TERMINAL_PERMISSION,
                materialDialog,
                onPermissionGranted,
                false,
            )
        }
    }

    /**
     * Requests permission, overrides {@param rationale}'s POSITIVE button dialog action.
     *
     * @param permission The permission to ask for
     * @param code [.STORAGE_PERMISSION] or [.INSTALL_APK_PERMISSION]
     * @param rationale MaterialLayout to provide an additional rationale to the user if the
     * permission was not granted and the user would benefit from additional context for the use
     * of the permission. For example, if the request has been denied previously.
     * @param isInitialStart is the permission being requested for the first time in the application
     * lifecycle
     */
    private fun requestPermission(
        permission: String,
        code: Int,
        rationale: MaterialDialog,
        onPermissionGranted: (() -> Unit),
        isInitialStart: Boolean,
    ) {
        permissionCallbacks[code] = onPermissionGranted

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            rationale
                .getActionButton(DialogAction.POSITIVE)
                .setOnClickListener { v: View? ->
                    ActivityCompat.requestPermissions(
                        this@PermissionsActivity,
                        arrayOf(permission),
                        code,
                    )
                    rationale.dismiss()
                }
            rationale.show()
        } else if (isInitialStart) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), code)
        } else {
            if (VERSION.SDK_INT >= VERSION_CODES.R) {
                Snackbar.make(
                    findViewById(R.id.content_frame),
                    R.string.grantfailed,
                    BaseTransientBottomBar.LENGTH_INDEFINITE,
                )
                    .setAction(R.string.grant) { v: View? ->
                        requestAllFilesAccessPermission(
                            onPermissionGranted,
                        )
                    }
                    .show()
            } else {
                Snackbar.make(
                    findViewById(R.id.content_frame),
                    R.string.grantfailed,
                    BaseTransientBottomBar.LENGTH_INDEFINITE,
                )
                    .setAction(
                        R.string.grant,
                    ) { v: View? ->
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse(String.format("package:%s", packageName)),
                            ),
                        )
                    }
                    .show()
            }
        }
    }

    /**
     * Request all files access on android 11+
     *
     * @param onPermissionGranted permission granted callback
     */
    fun requestAllFilesAccess(onPermissionGranted: (() -> Unit)) {
        if (VERSION.SDK_INT >= VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val materialDialog =
                GeneralDialogCreation.showBasicDialog(
                    this,
                    R.string.grant_all_files_permission,
                    R.string.grantper,
                    R.string.grant,
                    R.string.cancel,
                )
            materialDialog.getActionButton(DialogAction.NEGATIVE)
                .setOnClickListener { v: View? -> finish() }
            materialDialog
                .getActionButton(DialogAction.POSITIVE)
                .setOnClickListener { v: View? ->
                    requestAllFilesAccessPermission(onPermissionGranted)
                    materialDialog.dismiss()
                }
            materialDialog.setCancelable(false)
            materialDialog.show()
        }
    }

    @RequiresApi(api = VERSION_CODES.R)
    @Suppress("Detekt.TooGenericExceptionCaught")
    private fun requestAllFilesAccessPermission(onPermissionGranted: (() -> Unit)) {
        Utils.disableScreenRotation(this)
        permissionCallbacks[ALL_FILES_PERMISSION] = onPermissionGranted
        try {
            val intent =
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        } catch (anf: ActivityNotFoundException) {
            // fallback
            try {
                val intent =
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        .setData(Uri.parse("package:\$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                AppConfig.toast(this, getString(R.string.grantfailed))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initial activity to grant all files access", e)
            AppConfig.toast(this, getString(R.string.grantfailed))
        }
    }

    private fun isGranted(grantResults: IntArray): Boolean {
        return grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val TAG: String = PermissionsActivity::class.java.simpleName

        const val PERMISSION_LENGTH: Int = 5
        const val STORAGE_PERMISSION: Int = 0
        const val INSTALL_APK_PERMISSION: Int = 1
        const val ALL_FILES_PERMISSION: Int = 2
        const val NOTIFICATION_PERMISSION: Int = 3
        const val TERMINAL_PERMISSION: Int = 4
    }
}
