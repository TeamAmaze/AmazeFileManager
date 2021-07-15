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

package com.amaze.filemanager.ui.fragments.quickview.types

import android.os.Parcelable
import com.amaze.filemanager.adapters.data.IconDataParcelable
import com.amaze.filemanager.ui.fragments.quickview.HasQuickView
import com.amaze.filemanager.ui.fragments.quickview.QuickViewFragment
import kotlinx.parcelize.Parcelize

/**
 * This class represents any filetype that is openable in a [QuickViewFragment]
 * and contains all information to show it in one
 *
 * Check [HasQuickView] to see if the filetype is openable in a [QuickViewFragment]
 */
@Parcelize
open class QuickViewType(open val name: String) : Parcelable

data class QuickViewImage(
    val reference: IconDataParcelable,
    override val name: String
) : QuickViewType(name)
