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
import android.content.res.Configuration
import android.os.Build.VERSION_CODES.*
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
    @Test
    fun testThemeLightTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.LIGHT_INDEX)
        Assert.assertEquals(AppTheme.LIGHT, apptheme)
    }

    @Test
    fun testThemeDARKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.DARK_INDEX)
        Assert.assertEquals(AppTheme.DARK, apptheme)
    }

    @Test
    fun testThemeTIMEDTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.TIME_INDEX)
        Assert.assertEquals(AppTheme.TIMED, apptheme)
    }

    @Test
    fun testThemeBLACKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.BLACK_INDEX)
        Assert.assertEquals(AppTheme.BLACK, apptheme)
    }

    @Test
    fun testMaterialDialogThemeLIGHTTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.LIGHT_INDEX)
        Assert.assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme(context))
    }

    @Test
    fun testMaterialDialogThemeDARKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.DARK_INDEX)
        Assert.assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme(context))
    }

    @Test
    fun testMaterialDialogThemeTIMEDTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.TIME_INDEX)
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme(context))
        } else Assert.assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme(context))
    }

    @Test
    fun testMaterialDialogThemeBLACKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.BLACK_INDEX)
        Assert.assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme(context))
    }

    @Test
    fun testSimpleThemeLIGHTTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.LIGHT_INDEX)
        Assert.assertEquals(
            AppTheme.LIGHT,
            apptheme.getSimpleTheme(
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    == Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun testSimpleThemeDARKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.DARK_INDEX)
        Assert.assertEquals(
            AppTheme.DARK,
            apptheme.getSimpleTheme(
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    == Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun testSimpleThemeTIMEDTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.TIME_INDEX)
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        if (hour <= 6 || hour >= 18) {
            Assert.assertEquals(
                AppTheme.DARK,
                apptheme.getSimpleTheme(
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                        == Configuration.UI_MODE_NIGHT_YES
                )
            )
        } else Assert.assertEquals(
            AppTheme.LIGHT,
            apptheme.getSimpleTheme(
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    == Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun testSimpleThemeBLACKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apptheme = AppTheme.getTheme(context, AppTheme.BLACK_INDEX)
        Assert.assertEquals(
            AppTheme.BLACK,
            apptheme.getSimpleTheme(
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    == Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun testIdLIGHTTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val index = 0
        val apptheme = AppTheme.getTheme(context, index)
        Assert.assertEquals(index.toLong(), apptheme.id.toLong())
    }

    @Test
    fun testIdDARKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val index = 1
        val apptheme = AppTheme.getTheme(context, index)
        Assert.assertEquals(index.toLong(), apptheme.id.toLong())
    }

    @Test
    fun testIdTIMEDTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val index = 2
        val apptheme = AppTheme.getTheme(context, index)
        Assert.assertEquals(index.toLong(), apptheme.id.toLong())
    }

    @Test
    fun testIdBLACKTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val index = 3
        val apptheme = AppTheme.getTheme(context, index)
        Assert.assertEquals(index.toLong(), apptheme.id.toLong())
    }
}
