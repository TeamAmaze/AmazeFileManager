/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters

import com.amaze.filemanager.adapters.RecyclerAdapter.EMPTY_LAST_ITEM
import com.amaze.filemanager.adapters.RecyclerAdapter.ListItem
import com.amaze.filemanager.adapters.RecyclerAdapter.TYPE_HEADER_FILES
import com.amaze.filemanager.adapters.RecyclerAdapter.TYPE_HEADER_FOLDERS
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RecyclerAdapter.ListItem.setAnimate] and [RecyclerAdapter.ListItem.getAnimating].
 *
 * Before the fix, `setAnimate()` contained the guard `if (specialType == -1)`.  Since no
 * [ListItem] type constant equals -1, this condition was **always false** and the animate flag
 * was never set.  As a result, `getAnimating()` always returned `false`, causing the fade-in
 * animation to fire on every single `onBindViewHolder` call (even for already-visible rows).
 *
 * After the fix the guard is `if (specialType == TYPE_ITEM || specialType == TYPE_BACK)`, which
 * is the correct set of item types that represent real files and should animate.
 */
class RecyclerAdapterListItemTest {
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun makeItem(): ListItem {
        val parcelable = mockk<LayoutElementParcelable>(relaxed = true)
        return ListItem(parcelable) // TYPE_ITEM
    }

    private fun makeBackItem(): ListItem {
        val parcelable = mockk<LayoutElementParcelable>(relaxed = true)
        return ListItem(true, parcelable) // TYPE_BACK
    }

    private fun makeSpecialItem(type: Int): ListItem = ListItem(type)

    /**
     * A fresh [ListItem] must have `getAnimating() == false` before any call to `setAnimate`.
     */
    @Test
    fun testGetAnimating_defaultFalse_typeItem() {
        assertFalse(makeItem().animating)
    }

    /**
     * The ".." (back) entry is a real navigable item and must support animation,
     * but it should start with the flag unset until `setAnimate(true)` is called.
     */
    @Test
    fun testGetAnimating_defaultFalse_typeBack() {
        assertFalse(makeBackItem().animating)
    }

    /**
     * For a regular file item (TYPE_ITEM), `setAnimate(true)` must enable the flag.
     */
    @Test
    fun testSetAnimate_true_typeItem_flagIsTrue() {
        val item = makeItem()
        item.setAnimate(true)
        assertTrue(
            "setAnimate(true) on TYPE_ITEM must set the animate flag",
            item.animating,
        )
    }

    /**
     * After setting to true, `setAnimate(false)` must reset the flag.
     */
    @Test
    fun testSetAnimate_falseAfterTrue_typeItem_flagIsFalse() {
        val item = makeItem()
        item.setAnimate(true)
        item.setAnimate(false)
        assertFalse(
            "setAnimate(false) on TYPE_ITEM must clear the animate flag",
            item.animating,
        )
    }

    /**
     * The ".." (back) entry is a real navigable item and must support animation.
     */
    @Test
    fun testSetAnimate_true_typeBack_flagIsTrue() {
        val item = makeBackItem()
        item.setAnimate(true)
        assertTrue(
            "setAnimate(true) on TYPE_BACK must set the animate flag",
            item.animating,
        )
    }

    /**
     * After setting to true, `setAnimate(false)` must reset the flag for TYPE_BACK as well.
     */
    @Test
    fun testSetAnimate_falseAfterTrue_typeBack_flagIsFalse() {
        val item = makeBackItem()
        item.setAnimate(true)
        item.setAnimate(false)
        assertFalse(item.animating)
    }

    /**
     * Section headers (TYPE_HEADER_FOLDERS) are not real file entries; they must
     * never carry an animation flag regardless of what is passed to `setAnimate`.
     *
     * This is the regression test for the original bug: before the fix the guard was
     * `specialType == -1` (always false), meaning headers *would* have had their flag
     * set if the check was intended to restrict rather than allow.  The corrected guard
     * restricts animation to TYPE_ITEM and TYPE_BACK only.
     */
    @Test
    fun testSetAnimate_true_typeHeaderFolders_remainsFalse() {
        val item = makeSpecialItem(TYPE_HEADER_FOLDERS)
        item.setAnimate(true)
        assertFalse(
            "setAnimate(true) on TYPE_HEADER_FOLDERS must be a no-op",
            item.animating,
        )
    }

    @Test
    fun testSetAnimate_true_typeHeaderFiles_remainsFalse() {
        val item = makeSpecialItem(TYPE_HEADER_FILES)
        item.setAnimate(true)
        assertFalse(
            "setAnimate(true) on TYPE_HEADER_FILES must be a no-op",
            item.animating,
        )
    }

    /**
     * The empty last item is a special non-file entry that must never carry an animation flag.
     */
    @Test
    fun testSetAnimate_true_emptyLastItem_remainsFalse() {
        val item = makeSpecialItem(EMPTY_LAST_ITEM)
        item.setAnimate(true)
        assertFalse(
            "setAnimate(true) on EMPTY_LAST_ITEM must be a no-op",
            item.animating,
        )
    }

    /**
     * The types for which [ListItem.setAnimate] works must be exactly those for which
     * [ListItem.specialTypeHasFile] returns `true`.  This confirms the two methods
     * share the same allowed set (TYPE_ITEM and TYPE_BACK).
     */
    @Test
    fun testSetAnimate_andSpecialTypeHasFile_sameTypesAllowed() {
        val fileItem = makeItem()
        val backItem = makeBackItem()
        val headerFolders = makeSpecialItem(TYPE_HEADER_FOLDERS)
        val headerFiles = makeSpecialItem(TYPE_HEADER_FILES)
        val emptyLast = makeSpecialItem(EMPTY_LAST_ITEM)

        // Both animate and specialTypeHasFile return true for TYPE_ITEM and TYPE_BACK
        for (item in listOf(fileItem, backItem)) {
            item.setAnimate(true)
            assertTrue(
                "Items that specialTypeHasFile() should also support setAnimate()",
                item.specialTypeHasFile() && item.animating,
            )
        }

        // Neither animate nor specialTypeHasFile is true for headers and empty items
        for (item in listOf(headerFolders, headerFiles, emptyLast)) {
            item.setAnimate(true)
            assertFalse(
                "Items that !specialTypeHasFile() should not support setAnimate()",
                item.animating,
            )
            assertFalse(item.specialTypeHasFile())
        }
    }
}
