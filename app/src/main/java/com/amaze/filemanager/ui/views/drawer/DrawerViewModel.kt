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

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import java.util.*

class DrawerViewModel : ViewModel() {
    private val menuMetadataMap = HashMap<HasherOfMenuItem, MenuMetadata>()

    fun getDrawerMetadata(item: MenuItem): MenuMetadata {
        return requireNotNull(menuMetadataMap[item.toNonLeaking()])
    }

    fun putDrawerMetadata(item: MenuItem, metadata: MenuMetadata) {
        menuMetadataMap[item.toNonLeaking()] = metadata
    }
}
