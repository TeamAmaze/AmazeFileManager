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

package com.amaze.filemanager.ui.fragments.preferencefragments

import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.children
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.test.getString
import com.amaze.filemanager.ui.activities.PreferencesActivity
import com.amaze.filemanager.ui.activities.PreferencesActivityTestBase
import com.amaze.filemanager.utils.getLocaleListFromXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLocaleManager
import org.robolectric.shadows.ShadowStorageManager

/**
 * Tests for [UiPrefsFragment].
 */
@Config(
    sdk = [
        Build.VERSION_CODES.O,
        Build.VERSION_CODES.P,
        Build.VERSION_CODES.R,
    ],
    shadows = [
        ShadowMultiDex::class,
        ShadowStorageManager::class,
        ShadowPasswordUtil::class,
        ShadowSmbFile::class,
        ShadowLocaleManager::class,
    ],
)
class UiPrefsFragmentTest : PreferencesActivityTestBase() {
    /**
     * Test default behaviour. When no language is selected
     * [AppCompatDelegate.getApplicationLocales] should return an empty
     * [androidx.core.os.LocaleListCompat].
     */
    @Test
    fun testDefaultBehaviour() {
        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
        doPerformTestInternal { activity, prefFragment ->
            prefFragment.findPreference<Preference>("language")!!.performClick()
            assertNotNull(ShadowDialog.getLatestDialog())
            assertTrue(ShadowDialog.getLatestDialog() is MaterialDialog)

            (ShadowDialog.getLatestDialog() as MaterialDialog).run {
                assertEquals(items?.size, activity.getLocaleListFromXml().size() + 1)
                assertEquals(
                    getString(R.string.preference_language_system_default),
                    items?.get(0),
                )
                assertEquals(0, this.selectedIndex)
            }
        }
    }

    /**
     * Test change language. Randomly pick a language on the list, and see if app's locale is
     * changed and matches the same index in locale list XML.
     */
    @Test
    fun testChangeLanguage() {
        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
        doPerformTestInternal { activity, prefFragment ->
            // (x as Nothing) cast tells the kotlin type checker that fail() never returns
            val language = prefFragment.findPreference<Preference>("language") ?: (fail() as Nothing)
            language.performClick()

            val dialog = (ShadowDialog.getLatestDialog() as? MaterialDialog) ?: (fail() as Nothing)
            val items = dialog.items ?: (fail("Fail parsing locale list XML") as Nothing)

            val defaultLanguageIndex = 0

            assertEquals(items.size, activity.getLocaleListFromXml().size() + 1)
            assertEquals(
                getString(R.string.preference_language_system_default),
                items[0],
            )
            assertEquals(defaultLanguageIndex, dialog.selectedIndex)

            // Test all languages except default
            for (languageIndex in ((1..<items.size).toList().shuffled().take(5))) {
                println("Testing language ${items[languageIndex]} at index $languageIndex")
                dialog.selectedIndex = languageIndex

                val languageListView =
                    dialog.view.findViewById<RecyclerView>(
                        com.afollestad.materialdialogs.R.id.md_contentRecyclerView,
                    )

                // Span out the RecyclerView as much as possible so all items are displayed.
                // Trick: https://bterczynski.medium.com/robolectric-tips-testing-recyclerviews-d6e79209a4b
                languageListView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                languageListView.layout(0, 0, 10000, 10000)
                languageListView.children.toList()[languageIndex].performClick()

                assertFalse(AppCompatDelegate.getApplicationLocales().isEmpty)
                assertNotNull(AppCompatDelegate.getApplicationLocales()[0])
                assertEquals(
                    languageIndex,
                    // System Default is index 0 (top of list), hence index needs to + 1
                    activity.getLocaleListFromXml().indexOf(
                        AppCompatDelegate.getApplicationLocales()[0],
                    ) + 1,
                )
            }
        }
    }

    private fun doPerformTestInternal(test: (PreferencesActivity, UiPrefsFragment) -> Unit) {
        doTestPreferenceFragment { activity, prefsFragment ->
            // (x as Nothing) cast tells the kotlin type checker that fail() never returns
            val ui = prefsFragment.findPreference<Preference>("ui") ?: (fail() as Nothing)
            ui.performClick()
            activity.supportFragmentManager.executePendingTransactions()
            assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
            activity.supportFragmentManager.fragments.filterIsInstance<UiPrefsFragment>()
                .first().run {
                    test.invoke(activity, this)
                }
        }
    }
}
