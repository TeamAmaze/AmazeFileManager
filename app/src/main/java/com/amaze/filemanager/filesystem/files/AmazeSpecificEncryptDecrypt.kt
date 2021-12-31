package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.amaze.filemanager.file_operations.filesystem.encryption.EncryptDecrypt
import com.amaze.filemanager.file_operations.filesystem.encryption.EncryptDecrypt.decryptPassword
import com.amaze.filemanager.file_operations.filesystem.encryption.EncryptDecrypt.encryptPassword
import com.amaze.filemanager.filesystem.files.CryptUtil.*
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.Key
import javax.crypto.Cipher

object AmazeSpecificEncryptDecrypt {
    /**
     * Gets a secret key from Android key store. If no key has been generated with a given alias then
     * generate a new one
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(GeneralSecurityException::class, IOException::class)
    fun getSecretKey(): Key = EncryptDecrypt.getSecretKey(KEY_STORE_ANDROID, KEY_ALIAS_AMAZE)

    /** Method handles encryption of plain text on various APIs */
    @JvmStatic
    @Throws(GeneralSecurityException::class, IOException::class)
    fun encryptPassword(context: Context, plainText: String): String = encryptPassword(context, IV, KEY_STORE_ANDROID, KEY_ALIAS_AMAZE, plainText)

    /** Method handles decryption of cipher text on various APIs  */
    @JvmStatic
    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptPassword(context: Context, cipherText: String): String = decryptPassword(context, IV, KEY_STORE_ANDROID, KEY_ALIAS_AMAZE, cipherText)

    /**
     * Method initializes a Cipher to be used by [android.hardware.fingerprint.FingerprintManager]
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class, IOException::class)
    fun initCipher(context: Context): Cipher? = EncryptDecrypt.initCipher(context, IV, KEY_STORE_ANDROID, KEY_ALIAS_AMAZE)
}