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

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.utils.security.SecretKeygen
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

object PasswordUtil {

    // 12 byte long IV supported by android for GCM
    private const val IV = BuildConfig.CRYPTO_IV

    /** Helper method to encrypt plain text password  */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(
        GeneralSecurityException::class,
        IOException::class
    )
    private fun aesEncryptPassword(plainTextPassword: String, base64Options: Int): String? {
        val cipher = Cipher.getInstance(CryptUtil.ALGO_AES)
        val gcmParameterSpec = GCMParameterSpec(128, IV.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeygen.getSecretKey(), gcmParameterSpec)
        val encodedBytes = cipher.doFinal(plainTextPassword.toByteArray())
        return Base64.encodeToString(encodedBytes, base64Options)
    }

    /** Helper method to decrypt cipher text password  */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(
        GeneralSecurityException::class,
        IOException::class
    )
    private fun aesDecryptPassword(cipherPassword: String, base64Options: Int): String {
        val cipher = Cipher.getInstance(CryptUtil.ALGO_AES)
        val gcmParameterSpec = GCMParameterSpec(128, IV.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, SecretKeygen.getSecretKey(), gcmParameterSpec)
        val decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, base64Options))
        return String(decryptedBytes)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Throws(
        GeneralSecurityException::class,
        IOException::class
    )
    private fun rsaEncryptPassword(
        context: Context,
        password: String,
        base64Options: Int
    ): String? {
        val cipher = Cipher.getInstance(CryptUtil.ALGO_AES)
        val ivParameterSpec = IvParameterSpec(IV.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeygen.getSecretKey(), ivParameterSpec)
        return Base64.encodeToString(cipher.doFinal(password.toByteArray()), base64Options)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Throws(
        GeneralSecurityException::class,
        IOException::class
    )
    private fun rsaDecryptPassword(
        context: Context,
        cipherText: String,
        base64Options: Int
    ): String {
        val cipher = Cipher.getInstance(CryptUtil.ALGO_AES)
        val ivParameterSpec = IvParameterSpec(IV.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, SecretKeygen.getSecretKey(), ivParameterSpec)
        val decryptedBytes = cipher.doFinal(Base64.decode(cipherText, base64Options))
        return String(decryptedBytes)
    }

    /** Method handles encryption of plain text on various APIs  */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun encryptPassword(
        context: Context,
        plainText: String,
        base64Options: Int = Base64.URL_SAFE
    ): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aesEncryptPassword(plainText, base64Options)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            rsaEncryptPassword(context, plainText, base64Options)
        } else plainText
    }

    /** Method handles decryption of cipher text on various APIs  */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptPassword(
        context: Context,
        cipherText: String,
        base64Options: Int = Base64.URL_SAFE
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aesDecryptPassword(cipherText, base64Options)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            rsaDecryptPassword(context, cipherText, base64Options)
        } else cipherText
    }
}
