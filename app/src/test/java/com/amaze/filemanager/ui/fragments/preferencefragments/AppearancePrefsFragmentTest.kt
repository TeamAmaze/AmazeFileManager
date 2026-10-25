/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.fragments.preferencefragments

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.Q
import android.view.View
import android.widget.RadioButton
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.PreferencesActivity
import com.amaze.filemanager.ui.theme.AppThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/**
 * Test the real appearance theme chooser across the Android 10 system-theme boundary.
 */
@Config(sdk = [LOLLIPOP, P, Q, Build.VERSION_CODES.R])
class AppearancePrefsFragmentTest : AbstractMainActivityTestBase() {
    /**
     * Keep every theme index and the existing default while disabling unsupported System rows.
     */
    @Test
    fun testSystemThemeAvailabilityPreservesDefaultAndChoices() {
        withThemeChooser(null) { activity, dialog ->
            assertEquals(
                activity.resources.getStringArray(R.array.theme).toList(),
                dialog.items?.map { it.toString() },
            )
            assertEquals(AppThemePreference.SYSTEM_INDEX, dialog.selectedIndex)
            assertFalse(activity.prefs.contains(PreferencesConstants.FRAGMENT_THEME))

            val systemRow = themeRow(dialog, AppThemePreference.SYSTEM_INDEX)
            val supported = Build.VERSION.SDK_INT >= Q
            assertEquals(supported, systemRow.isEnabled)
            val control =
                systemRow.findViewById<RadioButton>(
                    com.afollestad.materialdialogs.R.id.md_control,
                )
            assertEquals(supported, control.isEnabled)
            assertTrue(control.isChecked)
        }
    }

    /**
     * A real System-row click only updates the saved theme on Android 10 or later.
     */
    @Test
    fun testSystemThemeSelectionFollowsPlatformSupport() {
        withThemeChooser(AppThemePreference.LIGHT_INDEX.toString()) { activity, dialog ->
            themeRow(dialog, AppThemePreference.SYSTEM_INDEX).performClick()

            val expected =
                if (Build.VERSION.SDK_INT >= Q) {
                    AppThemePreference.SYSTEM_INDEX
                } else {
                    AppThemePreference.LIGHT_INDEX
                }
            assertEquals(
                expected.toString(),
                activity.prefs.getString(PreferencesConstants.FRAGMENT_THEME, null),
            )
            assertEquals(expected, dialog.selectedIndex)
        }
    }

    /**
     * Disabling System must leave the ordinary Black-theme row at its original saved index.
     */
    @Test
    fun testExplicitThemeSelectionPreservesStoredIndex() {
        withThemeChooser(AppThemePreference.LIGHT_INDEX.toString()) { activity, dialog ->
            val blackRow = themeRow(dialog, AppThemePreference.BLACK_INDEX)
            assertTrue(blackRow.isEnabled)
            assertTrue(blackRow.performClick())
            assertEquals(
                AppThemePreference.BLACK_INDEX.toString(),
                activity.prefs.getString(PreferencesConstants.FRAGMENT_THEME, null),
            )
            assertEquals(AppThemePreference.BLACK_INDEX, dialog.selectedIndex)
        }
    }

    /**
     * Open Appearance and its actual theme dialog from PreferencesActivity, closing it afterward.
     */
    private fun withThemeChooser(
        storedTheme: String?,
        test: (PreferencesActivity, MaterialDialog) -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(PreferencesConstants.FRAGMENT_THEME, storedTheme).apply()
        val scenario = ActivityScenario.launch(PreferencesActivity::class.java)

        try {
            ShadowLooper.idleMainLooper()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity ->
                val root =
                    activity.supportFragmentManager.fragments.filterIsInstance<PrefsFragment>()
                        .single()
                requireNotNull(root.findPreference<Preference>("appearance")).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                val appearance =
                    activity.supportFragmentManager.fragments
                        .filterIsInstance<AppearancePrefsFragment>().single()
                requireNotNull(
                    appearance.findPreference<Preference>(PreferencesConstants.FRAGMENT_THEME),
                ).performClick()
                val dialog = ShadowDialog.getLatestDialog() as MaterialDialog
                test(activity, dialog)
            }
        } finally {
            ShadowDialog.getLatestDialog()?.dismiss()
            scenario.close()
        }
    }

    /**
     * Lay out the real dialog list and return the bound row at its persisted theme index.
     */
    private fun themeRow(
        dialog: MaterialDialog,
        index: Int,
    ): View {
        val list =
            dialog.view.findViewById<RecyclerView>(
                com.afollestad.materialdialogs.R.id.md_contentRecyclerView,
            )
        list.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        list.layout(0, 0, 10000, 10000)
        return requireNotNull(list.findViewHolderForAdapterPosition(index)).itemView
    }
}
