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
     * Test that the theme coincides with the index for [AppThemePreference.getTheme]
     */
    @Test
    fun testGetTheme() {
        Assert.assertEquals(
            AppThemePreference.LIGHT,
            AppThemePreference.getTheme(AppThemePreference.LIGHT_INDEX)
        )

        Assert.assertEquals(
            AppThemePreference.DARK,
            AppThemePreference.getTheme(AppThemePreference.DARK_INDEX)
        )

        Assert.assertEquals(
            AppThemePreference.TIMED,
            AppThemePreference.getTheme(AppThemePreference.TIME_INDEX)
        )

        Assert.assertEquals(
            AppThemePreference.BLACK,
            AppThemePreference.getTheme(AppThemePreference.BLACK_INDEX)
        )

        Assert.assertEquals(
            AppThemePreference.SYSTEM,
            AppThemePreference.getTheme(AppThemePreference.SYSTEM_INDEX)
        )
    }

    /**
     * Test [AppTheme.getMaterialDialogTheme]
     */
    @Test
    fun testMaterialDialogTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(
            Theme.LIGHT,
            getMaterialDialogTheme(AppThemePreference.LIGHT_INDEX, context)
        )

        Assert.assertEquals(
            Theme.DARK,
            getMaterialDialogTheme(AppThemePreference.DARK_INDEX, context)
        )

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(
                Theme.DARK,
                getMaterialDialogTheme(AppThemePreference.TIME_INDEX, context)
            )
        } else {
            Assert.assertEquals(
                Theme.LIGHT,
                getMaterialDialogTheme(AppThemePreference.TIME_INDEX, context)
            )
        }

        Assert.assertEquals(
            Theme.DARK,
            getMaterialDialogTheme(AppThemePreference.BLACK_INDEX, context)
        )
    }

    /**
     * Test [AppTheme.getSimpleTheme] with mask
     */
    @Test
    fun testSimpleTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(
            AppTheme.LIGHT,
            getSimpleTheme(AppThemePreference.LIGHT_INDEX, context)
        )

        Assert.assertEquals(
            AppTheme.DARK,
            getSimpleTheme(AppThemePreference.DARK_INDEX, context)
        )

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(
                AppTheme.DARK,
                getSimpleTheme(AppThemePreference.TIME_INDEX, context)
            )
        } else Assert.assertEquals(
            AppTheme.LIGHT,
            getSimpleTheme(AppThemePreference.TIME_INDEX, context)
        )

        Assert.assertEquals(
            AppTheme.BLACK,
            getSimpleTheme(AppThemePreference.BLACK_INDEX, context)
        )
    }

    /**
     * Tests the "System" theme when night mode is on
     */
    @Test
    @Config(qualifiers = "night")
    fun testSimpleSystemThemeNightModeOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(AppTheme.DARK, getSimpleTheme(AppThemePreference.SYSTEM_INDEX, context))
    }

    /**
     * Tests the "System" theme when night mode is off
     */
    @Test
    @Config(qualifiers = "notnight")
    fun testSimpleSystemThemeNightModeOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Assert.assertEquals(
            AppTheme.LIGHT,
            getSimpleTheme(AppThemePreference.SYSTEM_INDEX, context)
        )
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

        val canBeLightThemes = AppThemePreference.values().filter { it.canBeLight }

        for (lightTheme in canBeLightThemes) {
            Assert.assertEquals(
                "For $lightTheme: ",
                AppTheme.DARK,
                getSimpleTheme(lightTheme.id, context)
            )
        }

        Assert.assertEquals(
            AppTheme.DARK,
            getSimpleTheme(AppThemePreference.DARK_INDEX, context)
        )

        Assert.assertEquals(
            AppTheme.BLACK,
            getSimpleTheme(AppThemePreference.BLACK_INDEX, context)
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

    /**
     * Tests the material dialog theme with "Follow Battery Saver" option selected when battery saver is on
     */
    @Test
    @Config(
        shadows = [ShadowPowerManager::class, ShadowMultiDex::class],
        qualifiers = "notnight",
        minSdk = Build.VERSION_CODES.LOLLIPOP
    )
    fun testMaterialDialogThemeWithFollowBatterySaverAndBatterySaverOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setUpForFollowBatterySaverMode(context, true)

        val canBeLightThemes = AppThemePreference.values().filter { it.canBeLight }

        for (lightTheme in canBeLightThemes) {
            Assert.assertEquals(
                "For $lightTheme: ",
                Theme.DARK,
                getMaterialDialogTheme(lightTheme.id, context)
            )
        }

        Assert.assertEquals(
            Theme.DARK,
            getMaterialDialogTheme(AppThemePreference.DARK_INDEX, context)
        )

        Assert.assertEquals(
            Theme.DARK,
            getMaterialDialogTheme(AppThemePreference.BLACK_INDEX, context)
        )
    }

    /**
     * Tests the material dialog theme with "Follow Battery Saver" option selected when battery saver is off
     */
    @Test
    @Config(
        shadows = [ShadowPowerManager::class, ShadowMultiDex::class],
        qualifiers = "notnight",
        minSdk = Build.VERSION_CODES.LOLLIPOP
    )
    fun testMaterialDialogThemeWithFollowBatterySaverAndBatterySaverOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setUpForFollowBatterySaverMode(context, false)

        // Should have the same behavior
        testMaterialDialogTheme()
    }

    /** Shortcut to get the material dialog theme from the theme index */
    private fun getMaterialDialogTheme(apptheme: Int, context: Context): Theme =
        AppThemePreference.getTheme(apptheme).getMaterialDialogTheme(context)

    /** Shortcut to get the simple theme from the theme index */
    private fun getSimpleTheme(index: Int, context: Context): AppTheme =
        AppThemePreference.getTheme(index).getSimpleTheme(context)

    /** Sets the battery saver mode to [batterySaverOn] and the "Follow battery saver" option to true */
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
