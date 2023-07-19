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

package com.amaze.filemanager.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test for [UUIDv5].
 */
class UUIDv5Test {

    /**
     * Test UUID generation. Value is based on SHA-1 hash, so it can be expected.
     *
     * Test case taken (again) from
     * https://gist.github.com/icedraco/00118b4d3c91d96d8c58e837a448f1b8
     */
    @Test
    fun testGenerateUUID() {
        val url = "http://www.whatever.com/test/"
        val uuid = UUIDv5.fromString(UUIDv5.URL, url)
        assertEquals("1730930d-a36a-5efd-aa3f-561a164f87a4", uuid.toString())
    }
}
