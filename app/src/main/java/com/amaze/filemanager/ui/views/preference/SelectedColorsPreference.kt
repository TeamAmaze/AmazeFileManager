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

package com.amaze.filemanager.ui.views.preference

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.preference.DialogPreference
import androidx.preference.PreferenceViewHolder
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.dialogs.ColorPickerDialog
import com.amaze.filemanager.ui.views.CircularColorsView

/**
 * This is the external notification that shows some text and a CircularColorsView.
 *
 * @author Emmanuel on 6/10/2017, at 15:36.
 */
class SelectedColorsPreference(context: Context?, attrs: AttributeSet?) :
    DialogPreference(context, attrs) {
    private var colors = intArrayOf(
        Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT
    )
    private var backgroundColor = 0
    private var visibility = View.VISIBLE
    private var selectedIndex = -1

    init {
        widgetLayoutResource = R.layout.selectedcolors_preference
        dialogLayoutResource = R.layout.dialog_colorpicker
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        dialogIcon = null
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        (holder?.findViewById(R.id.colorsection) as CircularColorsView).let { colorsView ->
            colorsView.setColors(colors[0], colors[1], colors[2], colors[3])
            colorsView.setDividerColor(backgroundColor)
            colorsView.visibility = visibility
        }
    }

    override fun getSummary(): CharSequence = ""

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return a!!.getString(index)!!
    }

    override fun onSaveInstanceState(): Parcelable {
        val myState = ColorPickerDialog.SavedState(super.onSaveInstanceState())
        myState.selectedItem = selectedIndex
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != ColorPickerDialog.SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as ColorPickerDialog.SavedState
        selectedIndex = myState.selectedItem
        super.onRestoreInstanceState(myState.superState) // onBindDialogView(View view)
        // select(selectedItem, true)
    }

    /**
     * Set colours' visibility.
     */
    fun setColorsVisibility(visibility: Int) {
        this.visibility = visibility
        notifyChanged()
    }

    /**
     * Sets the divider's colour.
     */
    fun setDividerColor(color: Int) {
        backgroundColor = color
    }

    /**
     * set colours to specified and notify colour changed.
     */
    fun setColors(color: Int, color1: Int, color2: Int, color3: Int) {
        colors = intArrayOf(color, color1, color2, color3)
        notifyChanged()
    }

    /**
     * notify colour changed.
     */
    fun invalidateColors() {
        notifyChanged()
    }
}
