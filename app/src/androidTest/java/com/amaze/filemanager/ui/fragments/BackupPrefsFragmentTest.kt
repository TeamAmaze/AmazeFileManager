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

package com.amaze.filemanager.ui.fragments

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.ui.activities.PreferencesActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.BackupPrefsFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupPrefsFragmentTest {

    @Test
    fun testExport() {
        val backupPrefsFragment = BackupPrefsFragment()

        val activityScenario = ActivityScenario.launch(PreferencesActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.STARTED)

        activityScenario.onActivity {
            it.supportFragmentManager.beginTransaction()
                .add(backupPrefsFragment, null)
                .commitNow()

            backupPrefsFragment.exportPrefs()
        }

        val file =
            File(
                ApplicationProvider.getApplicationContext<Context>().cacheDir.absolutePath +
                    File.separator +
                    "amaze_backup.json"
            )

        Assert.assertTrue(file.exists())
    }
}
