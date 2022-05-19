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
import org.junit.Test

/**
 * Test for [SftpEntry] model.
 */
@Suppress("StringLiteralDuplication")
class SftpEntryTest {

    /**
     * Equality test. equals() check for name, path, host SSH key and private key combinations.
     */
    @Test
    fun testEquals() {
        val a = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH key",
            "abcdefghijkl"
        )
        val b = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH key",
            "abcdefghijkl"
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val c = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH key",
            "abcdefghijkl"
        )
        val d = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH test auth key",
            "abcdefghijkl"
        )
        assertEquals(c, d)
        assertEquals(c.hashCode(), d.hashCode())

        val e = SftpEntry(
            "ssh://root:toor@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            null,
            null
        )
        val f = SftpEntry(
            "ssh://root:toor@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            null,
            null
        )
        assertEquals(e, f)
        assertEquals(e.hashCode(), f.hashCode())
    }
}
