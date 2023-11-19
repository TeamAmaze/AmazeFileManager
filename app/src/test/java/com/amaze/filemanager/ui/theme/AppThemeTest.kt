/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.theme

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.PowerManager
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afollestad.materialdialogs.Theme
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [KITKAT, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class]
)
class AppThemeTest {

    /**
     * Test that the theme coincides with the index for [AppTheme.getTheme]
     */
    @Test
    fun testGetTheme() {
        Assert.assertEquals(AppTheme.LIGHT, AppTheme.getTheme(AppTheme.LIGHT_INDEX))

        Assert.assertEquals(AppTheme.DARK, AppTheme.getTheme(AppTheme.DARK_INDEX))

        Assert.assertEquals(AppTheme.TIMED, AppTheme.getTheme(AppTheme.TIME_INDEX))

        Assert.assertEquals(AppTheme.BLACK, AppTheme.getTheme(AppTheme.BLACK_INDEX))

        Assert.assertEquals(AppTheme.SYSTEM, AppTheme.getTheme(AppTheme.SYSTEM_INDEX))
    }

    /**
     * Test [AppTheme.getMaterialDialogTheme]
     */
    @Test
    fun testMaterialDialogTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val getMaterialDialogTheme = { apptheme: Int ->
            AppTheme.getTheme(apptheme).getMaterialDialogTheme(context)
        }

        Assert.assertEquals(Theme.LIGHT, getMaterialDialogTheme(AppTheme.LIGHT_INDEX))

        Assert.assertEquals(Theme.DARK, getMaterialDialogTheme(AppTheme.DARK_INDEX))

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(Theme.DARK, getMaterialDialogTheme(AppTheme.TIME_INDEX))
        } else {
            Assert.assertEquals(Theme.LIGHT, getMaterialDialogTheme(AppTheme.TIME_INDEX))
        }

        Assert.assertEquals(Theme.DARK, getMaterialDialogTheme(AppTheme.BLACK_INDEX))
    }

    /**
     * Test [AppTheme.getSimpleTheme] with mask
     */
    @Test
    fun testSimpleTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(
            AppTheme.LIGHT,
            getSimpleTheme(AppTheme.LIGHT_INDEX, context)
        )

        Assert.assertEquals(
            AppTheme.DARK,
            getSimpleTheme(AppTheme.DARK_INDEX, context)
        )

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(
                AppTheme.DARK,
                getSimpleTheme(AppTheme.TIME_INDEX, context)
            )
        } else Assert.assertEquals(
            AppTheme.LIGHT,
            getSimpleTheme(AppTheme.TIME_INDEX, context)
        )

        Assert.assertEquals(
            AppTheme.BLACK,
            getSimpleTheme(AppTheme.BLACK_INDEX, context)
        )
    }

    /**
     * Tests the "System" theme when night mode is on
     */
    @Test
    @Config(qualifiers = "night")
    fun testSimpleSystemThemeNightModeOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(AppTheme.DARK, getSimpleTheme(AppTheme.SYSTEM_INDEX, context))
    }

    /**
     * Tests the "System" theme when night mode is off
     */
    @Test
    @Config(qualifiers = "notnight")
    fun testSimpleSystemThemeNightModeOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(AppTheme.LIGHT, getSimpleTheme(AppTheme.SYSTEM_INDEX, context))
    }

    /**
     * Tests the themes with "Follow Battery Saver" option selected when battery saver is on
     */
    @Test
    @Config(
        shadows = [ShadowPowerManager::class, ShadowMultiDex::class],
        qualifiers = "notnight",
        minSdk = Build.VERSION_CODES.LOLLIPOP
    )
    fun testSimpleAppThemeWithFollowBatterySaverAndBatterySaverOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setUpForFollowBatterySaverMode(context, true)

        val canBeLightThemes = AppTheme.values().filter { it.canBeLight() }

        for (lightTheme in canBeLightThemes) {
            Assert.assertEquals(
                "For $lightTheme: ",
                AppTheme.DARK,
                getSimpleTheme(lightTheme.id, context)
            )
        }

        Assert.assertEquals(
            AppTheme.DARK,
            getSimpleTheme(AppTheme.DARK_INDEX, context)
        )

        Assert.assertEquals(
            AppTheme.BLACK,
            getSimpleTheme(AppTheme.BLACK_INDEX, context)
        )
    }

    /**
     * Tests the themes with "Follow Battery Saver" option selected when battery saver is off
     */
    @Test
    @Config(
        shadows = [ShadowPowerManager::class, ShadowMultiDex::class],
        qualifiers = "notnight",
        minSdk = Build.VERSION_CODES.LOLLIPOP
    )
    fun testSimpleAppThemeWithFollowBatterySaverAndBatterySaverOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setUpForFollowBatterySaverMode(context, false)

        // Behavior should be like in testSimpleTheme
        testSimpleTheme()
    }

    private fun getSimpleTheme(index: Int, context: Context) =
        AppTheme.getTheme(index).getSimpleTheme(context)

    private fun setUpForFollowBatterySaverMode(context: Context, batterySaverOn: Boolean) {
        // Set Battery saver mode to given state `batterySaverOn`
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val shadowPowerManager = Shadows.shadowOf(powerManager)
        shadowPowerManager.setIsPowerSaveMode(batterySaverOn)

        // Set follow battery saver preference to true
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences
            .edit()
            .putBoolean(PreferencesConstants.FRAGMENT_FOLLOW_BATTERY_SAVER, true)
            .apply()
    }
}
