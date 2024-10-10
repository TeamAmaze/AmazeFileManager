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
package com.amaze.filemanager.ui.dialogs

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import io.mockk.spyk
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowStorageManager

@Config(
    sdk = [KITKAT, P, Build.VERSION_CODES.R],
    shadows = [
        ShadowMultiDex::class,
        ShadowTabHandler::class,
        ShadowStorageManager::class,
        ShadowPackageManager::class,
    ],
)
abstract class AbstractOpenFolderInTerminalTestBase : AbstractMainActivityTestBase() {
    /**
     * Note: this method will provide a MainActivity spy for the Lambda to work with
     */
    protected fun doTestWithMainActivity(withMainActivity: (MainActivity) -> Unit) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            val spy = spyk<MainActivity>(activity)
            withMainActivity.invoke(spy)
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }

    protected fun installApp(
        mainActivity: MainActivity,
        componentName: ComponentName,
    ) {
        shadowOf(mainActivity.packageManager).run {
            val intentFilter: IntentFilter =
                IntentFilter(Intent.ACTION_MAIN).also {
                    it.addCategory(Intent.CATEGORY_LAUNCHER)
                }
            addActivityIfNotPresent(componentName)
            addIntentFilterForActivity(componentName, intentFilter)
        }
    }
}
