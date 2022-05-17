/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.database.models.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Test [Grid] model.
 */
@Suppress("StringLiteralDuplication")
class GridTest {

    /**
     * Test [Grid.equals] for equality.
     */
    @Test
    fun testEquals() {
        val a = Grid("/storage/emulated/0")
        val b = Grid("/storage/emulated/0")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [Grid.equals] for inequality.
     */
    @Test
    fun testNotEquals() {
        val a = Grid("/storage/emulated/0")
        val b = Grid("/storage/emulated/1")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [Grid.equals] for inequality with other class.
     */
    @Test
    fun testForeignClassNotEquals() {
        val a = Grid("/storage/emulated/0")
        val b = List("/storage/emulated/0")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }
}
