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

package com.amaze.filemanager.ui.views.drawer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View

class GestureExclusionView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val gestureExclusionRects = mutableListOf<Rect>()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            updateGestureExclusion()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateGestureExclusion()
    }

    private fun updateGestureExclusion() {
        // Skip this call if we're not running on Android 10+
        if (Build.VERSION.SDK_INT < 29) {
            visibility = GONE
            return
        }
        visibility = VISIBLE
        setBackgroundColor(resources.getColor(android.R.color.transparent))
        gestureExclusionRects.clear()
        val rect = Rect()
        this.getGlobalVisibleRect(rect)
        gestureExclusionRects += rect
        systemGestureExclusionRects = gestureExclusionRects
    }
}
