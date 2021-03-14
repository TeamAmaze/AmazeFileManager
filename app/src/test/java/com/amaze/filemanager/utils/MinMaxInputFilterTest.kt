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

package com.amaze.filemanager.utils

import android.os.Build.VERSION_CODES.*
import android.text.SpannedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [JELLY_BEAN, KITKAT, P])
class MinMaxInputFilterTest {

    /**
     * Test MinMaxInputFilter functioning
     */
    @Test
    @Suppress("StringLiteralDuplication")
    fun testFilter() {
        val inputFilter = MinMaxInputFilter(1, 65535)
        assertNull(inputFilter.filter("1", 0, 0, SpannedString("1"), 0, 0))
        assertNull(inputFilter.filter("12345", 0, 0, SpannedString(""), 0, 0))
        assertNull(inputFilter.filter("", 0, 0, SpannedString("65535"), 0, 0))
        assertEquals("", inputFilter.filter("", 0, 0, SpannedString("65536"), 0, 0))
        assertEquals("", inputFilter.filter("12345", 0, 0, SpannedString("6"), 0, 0))
        assertEquals("", inputFilter.filter("123456", 0, 0, SpannedString("6"), 0, 0))
        assertEquals("", inputFilter.filter("123456", 0, 0, SpannedString(""), 0, 0))
        assertEquals("", inputFilter.filter(null, 0, 0, null, 0, 0))
        assertEquals("", inputFilter.filter(null, 0, 0, SpannedString("abcdef"), 0, 0))
        assertEquals("", inputFilter.filter("abcdef", 0, 0, null, 0, 0))
        assertEquals("", inputFilter.filter("123456", 0, 0, null, 0, 0))
        assertEquals("", inputFilter.filter("123456", 0, 0, SpannedString("abcd"), 0, 0))
    }
}
