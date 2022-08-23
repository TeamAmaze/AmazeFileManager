/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.test

// import android.content.Intent
// import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
// import android.net.Uri
// import android.os.Build
// import android.os.Build.VERSION.SDK_INT
// import android.os.Environment
// import android.provider.Settings
// import android.widget.Switch
// import androidx.test.platform.app.InstrumentationRegistry
// import androidx.test.uiautomator.UiDevice
// import androidx.test.uiautomator.UiSelector
// import org.junit.Assert.assertTrue

object StoragePermissionHelper {

    /**
     * This method is intended for Android R or above devices to obtain MANAGE_EXTERNAL_STORAGE
     * permission via UI Automator framework when running relevant Espresso tests.
     *
     * This method is flat commented out because UI Automator requires Android SDK 18, while
     * currently we still want to support SDK 14.
     */
    @JvmStatic
    fun obtainManageAppAllFileAccessPermissionAutomatically() {
//        if (!Environment.isExternalStorageManager() && SDK_INT > Build.VERSION_CODES.R) {
//            InstrumentationRegistry.getInstrumentation().run {
//                val device = androidx.test.uiautomator.UiDevice.getInstance(this)
//                val context = this.targetContext
//                device.pressHome()
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                    .setData(Uri.parse("package:${context.packageName}"))
//                    .addFlags(FLAG_ACTIVITY_NEW_TASK)
//                context.startActivity(intent)
//                val switch = device.findObject(
//                    androidx.test.uiautomator.UiSelector()
//                        .packageName("com.android.settings")
//                        .className(Switch::class.java.name)
//                        .resourceId("android:id/switch_widget")
//                )
//                switch.click()
//                assertTrue(switch.isChecked)
//                device.pressHome()
//            }
//        }
//        assertTrue(Environment.isExternalStorageManager())
        return // Try to get codacy happy if they ever check me... pretend I am doing something
    }
}
