/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.activities

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.amaze.filemanager.ui.fragments.preferencefragments.PrefsFragment
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.robolectric.shadows.ShadowLooper

/**
 * Base class for preference fragments, started from [PreferencesActivity].
 */
abstract class PreferencesActivityTestBase : AbstractMainActivityTestBase() {

    /**
     * Put in your test here.
     */
    protected open fun doTestPreferenceFragment(
        test: (PreferencesActivity, PrefsFragment) -> Unit
    ) {
        val scenario = ActivityScenario.launch(PreferencesActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity: PreferencesActivity ->
            assertNotNull(activity.supportFragmentManager.fragments)
            assertTrue(activity.supportFragmentManager.fragments.first() is PrefsFragment)
            test.invoke(
                activity,
                activity.supportFragmentManager.fragments.first() as PrefsFragment
            )
        }
    }
}
