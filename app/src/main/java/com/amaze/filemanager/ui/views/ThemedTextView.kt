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

import android.R
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.Utils

/**
 * Class sets text color based on current theme, without explicit method call in app lifecycle To
 * be used only under themed activity context
 *
 * @deprecated Use [ContrastingTextView.setIntelligentTextColor]
 */
class ThemedTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    companion object {
        /**
         * This updates a [TextView] in [MainActivity] with the correct color based on theme
         *
         * @deprecated Use [ContrastingTextView.setIntelligentTextColor]
         */
        @JvmStatic
        fun setTextViewColor(textView: TextView, context: Context) {
            if ((context as MainActivity).appTheme == AppTheme.LIGHT) {
                textView.setTextColor(Utils.getColor(context, R.color.black))
            } else if (context.appTheme == AppTheme.DARK || context.appTheme == AppTheme.BLACK) {
                textView.setTextColor(Utils.getColor(context, R.color.white))
            }
        }
    }

    init {
        setTextViewColor(this, context)
    }
}
