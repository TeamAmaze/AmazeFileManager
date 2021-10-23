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

package com.amaze.filemanager.ui.views

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.FloatRange
import com.amaze.filemanager.utils.Utils
import java.lang.Math.pow
import kotlin.math.pow

object ContrastingTextView {
    /**
     * Correctly sets text color based on a given background color so that the
     * user can see the text correctly
     */
    @JvmStatic
    fun setIntelligentTextColor(context: Context, textView: TextView, backgroundColor: Int) {
        val red = Color.red(backgroundColor) / 255.0
        val green = Color.green(backgroundColor) / 255.0
        val blue = Color.blue(backgroundColor) / 255.0

        val linearRed = computeLinearValueForChannel(red)
        val linearGreen = computeLinearValueForChannel(green)
        val linearBlue = computeLinearValueForChannel(blue)

        val luminance = (0.2126 * linearRed + 0.7152 * linearGreen + 0.0722 * linearBlue)

        val perceivedLuminance = computePerceivedLuminance(luminance)

        val color = if (perceivedLuminance > 50) android.R.color.black else android.R.color.white

        textView.setTextColor(Utils.getColor(context, color))
    }

    @FloatRange(from = 0.0, to = 1.0)
    private fun computeLinearValueForChannel(value: Double): Double {
        return if (value <= 0.04045) {
            value / 12.92
        } else {
            ((value + 0.055) / 1.055).pow(2.4)
        }
    }

    @FloatRange(from = 0.0, to = 100.0)
    private fun computePerceivedLuminance(luminance: Double): Double {
        return if (luminance <= 216.0 / 24389.0) {
            // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            luminance * (24389.0 / 27.0)
            // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            luminance.pow(1.0 / 3.0) * 116 - 16
        }
    }
}
