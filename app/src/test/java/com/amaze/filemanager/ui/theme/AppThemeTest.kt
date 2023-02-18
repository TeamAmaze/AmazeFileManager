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

package com.amaze.filemanager.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afollestad.materialdialogs.Theme
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [JELLY_BEAN, KITKAT, P],
    shadows = [ShadowMultiDex::class]
)
class AppThemeTest {

    /**
     * Test that the theme coincides with the index for [AppTheme.getTheme]
     */
    @Test
    fun testGetTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Assert.assertEquals(AppTheme.LIGHT, AppTheme.getTheme(context, AppTheme.LIGHT_INDEX))

        Assert.assertEquals(AppTheme.DARK, AppTheme.getTheme(context, AppTheme.DARK_INDEX))

        Assert.assertEquals(AppTheme.TIMED, AppTheme.getTheme(context, AppTheme.TIME_INDEX))

        Assert.assertEquals(AppTheme.BLACK, AppTheme.getTheme(context, AppTheme.BLACK_INDEX))
    }

    /**
     * Test [AppTheme.getMaterialDialogTheme]
     */
    @Test
    fun testMaterialDialogTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val getMaterialDialogTheme = { apptheme: Int ->
            AppTheme.getTheme(context, apptheme).getMaterialDialogTheme(context)
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
        val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = mask == Configuration.UI_MODE_NIGHT_YES

        Assert.assertEquals(
            AppTheme.LIGHT,
            AppTheme.getTheme(context, AppTheme.LIGHT_INDEX).getSimpleTheme(isNightMode)
        )

        Assert.assertEquals(
            AppTheme.DARK,
            AppTheme.getTheme(context, AppTheme.DARK_INDEX).getSimpleTheme(isNightMode)
        )

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(
                AppTheme.DARK,
                AppTheme.getTheme(context, AppTheme.TIME_INDEX).getSimpleTheme(isNightMode)
            )
        } else Assert.assertEquals(
            AppTheme.LIGHT,
            AppTheme.getTheme(context, AppTheme.TIME_INDEX).getSimpleTheme(isNightMode)
        )

        Assert.assertEquals(
            AppTheme.BLACK,
            AppTheme.getTheme(context, AppTheme.BLACK_INDEX).getSimpleTheme(isNightMode)
        )
    }
}
