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

package com.amaze.filemanager.utils

import android.app.Activity
import android.util.DisplayMetrics
import kotlin.math.roundToInt

class ScreenUtils(private val activity: Activity) {
    /**
     * Converts Density Pixels to real Pixels in screen
     * It uses context to retrieve the density.
     */
    fun convertDbToPx(dp: Float): Int =
        (activity.resources.displayMetrics.density * dp).roundToInt()

    /**
     * Converts real Pixels in screen to Density Pixels
     * It uses context to retrieve the density.
     */
    fun convertPxToDb(px: Float): Int =
        (px / activity.resources.displayMetrics.density).roundToInt()

    private val screenWidthInPx: Int
        get() {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }

    private val screenHeightInPx: Int
        get() {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.heightPixels
        }

    val screenWidthInDp: Int
        get() = convertPxToDb(screenWidthInPx.toFloat())
    val screeHeightInDb: Int
        get() = convertPxToDb(screenHeightInPx.toFloat())

    companion object {
        const val TOOLBAR_HEIGHT_IN_DP = 128 // 160 dpi
    }
}
