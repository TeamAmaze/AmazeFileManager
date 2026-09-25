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

package com.amaze.filemanager.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.P
import android.os.storage.StorageManager
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.test.TestUtils.initializeInternalStorage
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.PreferencesActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.BasePrefsFragment
import com.amaze.filemanager.ui.fragments.preferencefragments.PrefsFragment
import com.amaze.filemanager.ui.views.preference.CheckBox
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Convenient test base for [BasePrefsFragment] subclasses.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [LOLLIPOP, P, VERSION_CODES.R],
    shadows = [ShadowMultiDex::class, ShadowTabHandler::class],
)
abstract class AbstractPreferencesFragmentTest<out T : BasePrefsFragment>(private val key: String) {
    /**
     * MainActivity required setup.
     */
    @Before
    fun setUp() {
        if (SDK_INT >= N) initializeInternalStorage()
    }

    /**
     * Post test teardown.
     */
    @After
    fun tearDown() {
        if (SDK_INT >= N) {
            Shadows.shadowOf(
                ApplicationProvider.getApplicationContext<Context>().getSystemService(
                    StorageManager::class.java,
                ),
            ).resetStorageVolumeList()
        }
    }

    /**
     * Starts [PreferencesActivity], tap on specified preferences and perform test.
     */
    protected fun performTest(
        testContent: (
            prefs: SharedPreferences,
            preferencesActivity: PreferencesActivity,
            prefsFragment: T,
        ) -> Unit,
    ) {
        ActivityScenario.launch(MainActivity::class.java).let { mainScenario ->
            ShadowLooper.idleMainLooper()
            mainScenario.moveToState(Lifecycle.State.STARTED)
            mainScenario.onActivity { mainActivity ->
                ActivityScenario.launch<PreferencesActivity>(
                    Intent(mainActivity, PreferencesActivity::class.java),
                ).moveToState(Lifecycle.State.STARTED).onActivity { preferencesActivity ->
                    mainScenario.moveToState(Lifecycle.State.DESTROYED).close()
                    preferencesActivity.supportFragmentManager.run {
                        val prefs =
                            PreferenceManager.getDefaultSharedPreferences(
                                AppConfig.getInstance(),
                            )
                        val prefsFragment = fragments.first() as PrefsFragment
                        prefsFragment.findPreference<Preference>(key)?.performClick()
                        executePendingTransactions()
                        val targetFragment = fragments.first() as T
                        testContent.invoke(prefs, preferencesActivity, targetFragment)
                    }
                }
            }
        }
    }
}

/**
 * Test-only method for quickly finds specified Preference without worrying about nullability.
 */
fun BasePrefsFragment.requirePreference(key: String): Preference {
    return findPreference(key)
        ?: throw IllegalArgumentException("Preference [$key] not found")
}

/**
 * Test-only method to quickly finds specified Preference and cast it into [CheckBox].
 */
fun BasePrefsFragment.requireCheckboxPreference(key: String): CheckBox = requirePreference(key) as CheckBox
