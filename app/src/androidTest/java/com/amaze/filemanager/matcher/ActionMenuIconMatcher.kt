/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.matcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * This matcher allows to select by [ActionMenuItemView] icon
 *
 * From https://stackoverflow.com/a/70108466/3124150
 */
object ActionMenuIconMatcher {
    /**
     * A [Matcher] that will match against an [ActionMenuItemView] view with a specific icon
     * resource
     */
    @JvmStatic
    fun withActionIconDrawable(
        @DrawableRes resourceId: Int,
    ): Matcher<View?> {
        return object : BoundedMatcher<View?, ActionMenuItemView>(ActionMenuItemView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has image drawable resource $resourceId")
            }

            override fun matchesSafely(actionMenuItemView: ActionMenuItemView): Boolean {
                val iconDrawable = actionMenuItemView.itemData.icon ?: return false

                return sameBitmap(
                    actionMenuItemView.context,
                    iconDrawable,
                    resourceId,
                    actionMenuItemView,
                )
            }
        }
    }

    /**
     * Compares a [Drawable] against a resource id, returns if they are identical
     */
    @JvmStatic
    private fun sameBitmap(
        context: Context,
        drawable: Drawable,
        @DrawableRes resourceId: Int,
        view: View,
    ): Boolean {
        var drawable = drawable
        val otherDrawable: Drawable = context.resources.getDrawable(resourceId) ?: return false

        if (drawable is StateListDrawable) {
            val getStateDrawableIndex =
                StateListDrawable::class.java.getMethod(
                    "getStateDrawableIndex",
                    IntArray::class.java,
                )
            val getStateDrawable =
                StateListDrawable::class.java.getMethod(
                    "getStateDrawable",
                    Int::class.javaPrimitiveType,
                )
            val index = getStateDrawableIndex.invoke(drawable, view.drawableState)
            drawable = getStateDrawable.invoke(drawable, index) as Drawable
        }

        val bitmap = getBitmapFromDrawable(drawable)
        val otherBitmap = getBitmapFromDrawable(otherDrawable)
        return bitmap.sameAs(otherBitmap)
    }

    /**
     * Convert a [Bitmap] to a [Drawable] by drawing to a canvas
     */
    @JvmStatic
    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bitmap: Bitmap =
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
