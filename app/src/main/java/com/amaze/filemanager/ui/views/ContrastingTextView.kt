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
import com.amaze.filemanager.utils.Utils

object ContrastingTextView {
    /**
     * Correctly sets text color based on a given background color so that the
     * user can see the text correctly
     */
    @JvmStatic
    fun setIntelligentTextColor(context: Context, textView: TextView, backgroundColor: Int) {
        val red = Color.red(backgroundColor) * 0.299f
        val green = Color.green(backgroundColor) * 0.587f
        val blue = Color.blue(backgroundColor) * 0.114f

        val luma = (red + green + blue) / 255.0f

        val color = if (luma > 0.5) android.R.color.black else android.R.color.white

        textView.setTextColor(Utils.getColor(context, color))
    }
}
