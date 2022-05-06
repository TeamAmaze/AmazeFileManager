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
 * Test [SmbEntry] model.
 */
@Suppress("StringLiteralDuplication")
class SmbEntryTest {

    /**
     * Test [SmbEntry.equals] for equality.
     */
    @Test
    fun testEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [SmbEntry.equals] for inequality by name.
     */
    @Test
    fun testNameNotEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = SmbEntry("SMB connection 2", "smb://root:user@127.0.0.1/root")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [SmbEntry.equals] for inequality by path.
     */
    @Test
    fun testPathNotEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/toor")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [SmbEntry.equals] for inequality with other class.
     */
    @Test
    fun testForeignClassNotEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = Bookmark("SMB connection 1", "smb://root:user@127.0.0.1/root")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }
}
