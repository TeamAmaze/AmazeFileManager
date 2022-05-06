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

package com.amaze.filemanager.ui.dialogs

import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.test.getString
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.shadows.ShadowDialog

/**
 * Test [EncryptWarningDialog].
 */
class EncryptWarningDialogTest : AbstractEncryptDialogTests() {

    /**
     * Verify dialog behaviour.
     */
    @Test
    fun testDisplayDialog() {
        scenario.onActivity { activity ->
            EncryptWarningDialog.show(activity, activity.appTheme)
            assertEquals(1, ShadowDialog.getShownDialogs().size)
            assertTrue(ShadowDialog.getLatestDialog() is MaterialDialog)
            (ShadowDialog.getLatestDialog() as MaterialDialog).run {
                assertEquals(getString(R.string.warning), titleView.text)
                assertEquals(getString(R.string.crypt_warning_key), contentView?.text.toString())
                assertEquals(
                    getString(R.string.warning_never_show),
                    getActionButton(DialogAction.NEGATIVE).text
                )
                assertEquals(
                    getString(R.string.warning_confirm),
                    getActionButton(DialogAction.POSITIVE).text
                )
                assertTrue(getActionButton(DialogAction.POSITIVE).performClick())
            }
            assertFalse(
                PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
                    .getBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, false)
            )
        }
    }

    /**
     * Test logic if "Never show again" button is tapped.
     */
    @Test
    fun testDisplayDialogNeverShowAgain() {
        scenario.onActivity { activity ->
            EncryptWarningDialog.show(activity, activity.appTheme)
            assertEquals(1, ShadowDialog.getShownDialogs().size)
            assertTrue(ShadowDialog.getLatestDialog() is MaterialDialog)
            (ShadowDialog.getLatestDialog() as MaterialDialog).run {
                assertTrue(getActionButton(DialogAction.NEGATIVE).performClick())
            }
            assertTrue(
                PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
                    .getBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, false)
            )
        }
    }
}
