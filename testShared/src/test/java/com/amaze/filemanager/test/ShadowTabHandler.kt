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

package com.amaze.filemanager.test

import com.amaze.filemanager.database.TabHandler
import com.amaze.filemanager.database.models.explorer.Tab
import io.reactivex.Completable
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers

@Implements(TabHandler::class)
class ShadowTabHandler {

    companion object {
        /**
         * Implements [TabHandler.getInstance]
         */
        @JvmStatic @Implementation
        fun getInstance(): TabHandler = ReflectionHelpers.newInstance(TabHandler::class.java)
    }

    /**
     * Implements [TabHandler.addTab]
     */
    @Implementation
    fun addTab(tab: Tab): Completable {
        return Completable.fromCallable { true }
    }

    /**
     * Implements [TabHandler.update]
     */
    @Implementation
    fun update(tab: Tab) = Unit

    /**
     * For places where Activity is launched, but we are not actually looking at the Tabs loaded.
     *
     * @see TabHandler.getAllTabs
     */
    @Implementation
    fun getAllTabs(): Array<Tab> = emptyArray()
}
