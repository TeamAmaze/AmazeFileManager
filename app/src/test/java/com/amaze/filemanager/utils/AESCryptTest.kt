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

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random

/**
 * Unit test for [AESCrypt]
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [KITKAT, P, Build.VERSION_CODES.R])
class AESCryptTest {

    /**
     * Simple sanity test on [AESCrypt].
     */
    @Test
    fun testEncryptDecrypt() {
        val contents = Random(System.currentTimeMillis()).nextBytes(256)
        val out = ByteArrayOutputStream()
        var crypter = AESCrypt("12345678")
        crypter.encrypt(
            `in` = ByteArrayInputStream(contents),
            out = out,
            progressHandler = ProgressHandler()
        )
        out.close()
        val encrypted = out.toByteArray()
        val verify = ByteArrayOutputStream()
        crypter = AESCrypt("12345678")
        crypter.decrypt(encrypted.size.toLong(), ByteArrayInputStream(encrypted), verify)
        verify.close()
        val decrypted = verify.toByteArray()
        assertArrayEquals(contents, decrypted)
    }

    /**
     * Test scenario when wrong decrypt password is entered.
     */
    @Test(expected = AESCrypt.DecryptFailureException::class)
    fun testWrongPasswordDecrypt() {
        val contents = Random(System.currentTimeMillis()).nextBytes(256)
        val out = ByteArrayOutputStream()
        var crypter = AESCrypt("password")
        crypter.encrypt(
            `in` = ByteArrayInputStream(contents),
            out = out,
            progressHandler = ProgressHandler()
        )
        out.close()
        val encrypted = out.toByteArray()
        val verify = ByteArrayOutputStream()
        crypter = AESCrypt("foobar")
        crypter.decrypt(encrypted.size.toLong(), ByteArrayInputStream(encrypted), verify)
        verify.close()
//        val decrypted = verify.toByteArray()
//        assertArrayEquals(contents, decrypted)
    }
}
