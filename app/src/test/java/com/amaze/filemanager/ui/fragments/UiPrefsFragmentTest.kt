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

import android.text.format.Formatter
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_THUMB
import com.amaze.filemanager.ui.fragments.preferencefragments.UiPrefsFragment
import com.amaze.filemanager.utils.AppConstants.MEGABYTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.shadows.ShadowDialog

/**
 * Test [UiPrefsFragment].
 */
class UiPrefsFragmentTest : AbstractPreferencesFragmentTest<UiPrefsFragment>("ui") {
    /**
     * Verify default values.
     */
    @Test
    fun testDefaultStatuses() {
        performTest { _, preferencesActivity, uiPrefsFragment ->
            val disabledString = preferencesActivity.getString(R.string.disable)
            val noLimitString = preferencesActivity.getString(R.string.no_limit)
            uiPrefsFragment.run {
                assertTrue(requireCheckboxPreference(PREFERENCE_SHOW_THUMB).isChecked)
                requirePreference(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE).run {
                    assertTrue(this.isEnabled)
                    assertEquals(
                        noLimitString,
                        this.summary.toString(),
                    )
                }
                assertFalse(requireCheckboxPreference(PREFERENCE_SHOW_HIDDENFILES).isChecked)
                assertTrue(requireCheckboxPreference(PREFERENCE_SHOW_LAST_MODIFIED).isChecked)
                assertTrue(requireCheckboxPreference(PREFERENCE_SHOW_FILE_SIZE).isChecked)
                assertFalse(requireCheckboxPreference(PREFERENCE_SHOW_GOBACK_BUTTON).isChecked)
                assertEquals(
                    disabledString,
                    requirePreference(PREFERENCE_DRAG_AND_DROP_PREFERENCE).summary.toString(),
                )
                assertFalse(requireCheckboxPreference(PREFERENCE_SHOW_PERMISSIONS).isChecked)
            }
        }
    }

    /**
     * Verify that enabling/disabling show thumbnails also enables/disables the max size option
     */
    @Test
    fun testShowThumbnailsCheckbox() {
        performTest { prefs, preferencesActivity, prefsFragment ->
            prefsFragment.requireCheckboxPreference(PREFERENCE_SHOW_THUMB).performClick()
            assertFalse(prefs.getBoolean(PREFERENCE_SHOW_THUMB, true))
            assertFalse(
                prefsFragment.requirePreference(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE).isEnabled,
            )
        }
    }

    /**
     * Verify that the max size options are displayed correctly and have the correct values.
     */
    @Test
    fun testShowRemoteThumbnailsMaxSizeOptions() {
        val presetItems =
            AppConfig.getInstance().resources
                .getIntArray(R.array.thumbnailDisplaySizeLimitPreference)
        performTest { prefs, preferencesActivity, prefsFragment ->
            prefsFragment.requirePreference(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE).performClick()
            assertEquals(1, ShadowDialog.getShownDialogs().size)
            assertTrue(ShadowDialog.getLatestDialog() is MaterialDialog)
            (ShadowDialog.getLatestDialog() as MaterialDialog).let { dialog ->
                assertEquals(presetItems.size, dialog.items?.size)
                dialog.items?.forEachIndexed { index, value ->
                    if (index == 0) {
                        assertEquals(AppConfig.getInstance().getString(R.string.no_limit), value)
                    } else {
                        assertEquals(
                            Formatter.formatShortFileSize(
                                AppConfig.getInstance(),
                                (presetItems[index] * MEGABYTE).toLong(),
                            ),
                            value,
                        )
                    }
                } ?: fail("No item available!?")
                dialog.view
            }
        }
    }
}
