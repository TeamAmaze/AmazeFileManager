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

package com.amaze.filemanager.utils.security

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.M
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.files.CryptUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

object SecretKeygen {

    private const val PREFERENCE_KEY = "aes_key"
    private const val ALGO_RSA = "RSA/ECB/PKCS1Padding"

    /**
     * Return [Key] in application. Generate one if it doesn't exist in AndroidKeyStore.
     *
     * @return AES key for API 23 or above, RSA key for API 18 or above, or else null
     */
    fun getSecretKey(): Key? {
        return if (SDK_INT >= M) {
            getAesSecretKey()
        } else if (SDK_INT >= JELLY_BEAN_MR2) {
            getRsaSecretKey()
        } else {
            null
        }
    }

    /**
     * Gets a secret key from Android key store. If no key has been generated with a given alias then
     * generate a new one
     */
    @RequiresApi(api = M)
    @Throws(
        GeneralSecurityException::class,
        IOException::class
    )
    private fun getAesSecretKey(): Key {
        val keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID)
        keyStore.load(null)
        return if (!keyStore.containsAlias(CryptUtil.KEY_ALIAS_AMAZE)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                CryptUtil.KEY_STORE_ANDROID
            )
            val builder = KeyGenParameterSpec.Builder(
                CryptUtil.KEY_ALIAS_AMAZE,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            builder.setRandomizedEncryptionRequired(false)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        } else {
            keyStore.getKey(CryptUtil.KEY_ALIAS_AMAZE, null)
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    @RequiresApi(JELLY_BEAN_MR2)
    private fun getRsaSecretKey(): Key {
        val preferences = PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
        val encodedString = preferences.getString(PREFERENCE_KEY, null)
        return if (encodedString != null) {
            SecretKeySpec(
                decryptAESKey(Base64.decode(encodedString, Base64.DEFAULT)),
                "AES"
            )
        } else {
            generateRsaKeyPair(AppConfig.getInstance())
            setKeyPreference()
            getRsaSecretKey()
        }
    }

    /** Generates a RSA public/private key pair to encrypt AES key  */
    @RequiresApi(api = JELLY_BEAN_MR2)
    private fun generateRsaKeyPair(context: Context) {
        val keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID)
        keyStore.load(null)
        if (!keyStore.containsAlias(CryptUtil.KEY_ALIAS_AMAZE)) {
            // generate a RSA key pair to encrypt/decrypt AES key from preferences
            val start = Calendar.getInstance()
            val end = Calendar.getInstance()
            end.add(Calendar.YEAR, 30)
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", CryptUtil.KEY_STORE_ANDROID)
            val spec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(CryptUtil.KEY_ALIAS_AMAZE)
                .setSubject(X500Principal("CN=" + CryptUtil.KEY_ALIAS_AMAZE))
                .setSerialNumber(BigInteger.TEN)
                .setStartDate(start.time)
                .setEndDate(end.time)
                .build()
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
        }
    }

    /** Encrypts AES key and set into preference  */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun setKeyPreference() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance()).run {
            var encodedAesKey = getString(PREFERENCE_KEY, null)
            if (encodedAesKey == null) {
                // generate encrypted aes key and save to preference
                val key = ByteArray(16)
                val secureRandom = SecureRandom()
                secureRandom.nextBytes(key)
                val encryptedKey: ByteArray = encryptAESKey(key)
                encodedAesKey = Base64.encodeToString(encryptedKey, Base64.DEFAULT)
                edit().putString(PREFERENCE_KEY, encodedAesKey).apply()
            }
        }
    }

    /** Encrypts randomly generated AES key using RSA public key  */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun encryptAESKey(secretKey: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID)
        keyStore.load(null)
        val keyEntry =
            keyStore.getEntry(CryptUtil.KEY_ALIAS_AMAZE, null) as KeyStore.PrivateKeyEntry
        val cipher = Cipher.getInstance(ALGO_RSA, "AndroidOpenSSL")
        cipher.init(Cipher.ENCRYPT_MODE, keyEntry.certificate.publicKey)
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream = CipherOutputStream(byteArrayOutputStream, cipher)
        outputStream.write(secretKey)
        outputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    /** Decrypts AES decoded key from preference using RSA private key  */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun decryptAESKey(encodedBytes: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID)
        keyStore.load(null)
        val keyEntry =
            keyStore.getEntry(CryptUtil.KEY_ALIAS_AMAZE, null) as KeyStore.PrivateKeyEntry
        val cipher = Cipher.getInstance(ALGO_RSA, "AndroidOpenSSL")
        cipher.init(Cipher.DECRYPT_MODE, keyEntry.privateKey)
        val byteArrayInputStream = ByteArrayInputStream(encodedBytes)
        val inputStream = CipherInputStream(byteArrayInputStream, cipher)
        val bytes = ArrayList<Byte>()
        var nextByte: Int
        while (inputStream.read().also { nextByte = it } != -1) {
            bytes.add(nextByte.toByte())
        }
        val decryptedBytes = ByteArray(bytes.size)
        for (i in bytes.indices) {
            decryptedBytes[i] = bytes[i]
        }
        return decryptedBytes
    }
}
