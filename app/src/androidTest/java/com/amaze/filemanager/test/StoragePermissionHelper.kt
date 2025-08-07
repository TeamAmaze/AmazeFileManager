/*
 * Copyright (C) 2014-2025 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity

object StoragePermissionHelper {
    /**
     * From https://github.com/android/android-test/issues/1658#issue-1551755250
     * HACK this grants access to external storage "manually" because other solutions don't seem
     * to set the permission.
     */
    @JvmStatic
    fun grantManageStoragePermission() {
        // Ensure that an activity that has the dialog is launched
        ActivityScenario.launch(MainActivity::class.java)

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val amazeResources = context.packageManager.getResourcesForApplication(context.packageName)
        val grantPermissionExplanation = amazeResources.getString(R.string.grant_all_files_permission)

        if (device.hasObject(By.text(grantPermissionExplanation))) {
            // First press Amaze's grant button
            onView(withText(R.string.grant)).perform(click())

            // Identifier names are taken here:
            // https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/res/values/strings.xml
            val resources = context.packageManager.getResourcesForApplication("com.android.settings")
            val resId =
                resources.getIdentifier(
                    "permit_manage_external_storage",
                    "string",
                    "com.android.settings",
                )
            val permitManageExternalStorage = resources.getString(resId)

            val grantToggle =
                device.findObject(UiSelector().textMatches("(?i)$permitManageExternalStorage"))
            grantToggle.click()
            device.pressBack()
        }
    }
}
