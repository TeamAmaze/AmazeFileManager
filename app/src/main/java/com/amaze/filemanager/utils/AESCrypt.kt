/*
 * Copyright (C) 2014-2008 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.util.Log
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_16LE
import kotlin.text.Charsets.UTF_8

/**
 * This is code comment from original AESCrypt.java.
 *
 * This class provides methods to encrypt and decrypt files using
 * [aescrypt file format](http://www.aescrypt.com/aes_file_format.html),
 * version 1 or 2.
 *
 *
 * Requires Java 6 and [Java
 * Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files](http://java.sun.com/javase/downloads/index.jsp).
 *
 *
 * Thread-safety and sharing: this class is not thread-safe.<br></br>
 * <tt>AESCrypt</tt> objects can be used as Commands (create, use once and dispose),
 * or reused to perform multiple operations (not concurrently though).
 *
 * @author Vócali Sistemas Inteligentes
 */

/**
 * A modified version of original AESCrypt and converted to Kotlin.
 *
 * Changes from original version:
 * - only handles and output streams
 * - use [android.util.Log] instead of System.out.println
 * - made protected methods private
 * - not using device MAC address to generate IV1
 * - use of Kotlin shorthands instead of reinventing the wheel
 * - throw more precise [IncorrectEncryptedDataException] and [DecryptFailureException] for better
 * error handling
 *
 * Wishlist:
 * - implement ChaCha20-Poly1305 cipher, which is faster for mobile devices at the cost of only
 *   Amaze and implementations that knows Amaze can decrypt. Shall add flags in extensions header
 *
 * @author TranceLove <airwave209gt@gmail.com>
 *
 */
class AESCrypt(password: String) {

    private lateinit var password: ByteArray
    private val cipher: Cipher
    private val hmac: Mac
    private val random: SecureRandom
    private val digest: MessageDigest
    private lateinit var ivSpec1: IvParameterSpec
    private lateinit var aesKey1: SecretKeySpec
    private lateinit var ivSpec2: IvParameterSpec
    private lateinit var aesKey2: SecretKeySpec

    /*******************
     * PRIVATE METHODS *
     */
    /**
     * Generates a pseudo-random byte array.
     * @return pseudo-random byte array of <tt>len</tt> bytes.
     */
    private fun generateRandomBytes(len: Int): ByteArray {
        val bytes = ByteArray(len)
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * SHA256 digest over given byte array and random bytes.<br></br>
     * <tt>bytes.length</tt> * <tt>num</tt> random bytes are added to the digest.
     *
     *
     * The generated hash is saved back to the original byte array.<br></br>
     * Maximum array size is [.SHA_SIZE] bytes.
     */
    private fun digestRandomBytes(bytes: ByteArray, num: Int) {
        require(bytes.size <= SHA_SIZE)
        digest.reset()
        digest.update(bytes)
        for (i in 0 until num) {
            random.nextBytes(bytes)
            digest.update(bytes)
        }
        digest.digest().copyInto(bytes, endIndex = bytes.size)
    }

    /**
     * Generates a pseudo-random IV based on time and 8 more random bytes.
     *
     * Changes from original implementation: it is never a good idea to get hardware MAC address
     * anyway, and Android effectively prevented this since Marshmallow. So why not just make all
     * 8 bytes completely random? At the end it is embedded into the AESCrypted file, and in fact
     * it may further reduce the possibility of IV being guessed if generated from the same device.
     *
     * The first 8 bytes is generated using the original method, and the remaining 8 bytes are
     * generated using the [random] we have here. - TranceLove
     *
     * This IV is used to crypt IV 2 and AES key 2 in the file.
     * @return IV.
     */
    private fun generateIv1(): ByteArray {
        val iv = ByteArray(BLOCK_SIZE)
        val time = System.currentTimeMillis()
        for (i in 0..7) {
            iv[i] = (time shr i * 8).toByte()
        }
        ByteArray(8).apply {
            random.nextBytes(this)
            copyInto(iv, destinationOffset = 8, startIndex = 0, endIndex = this.size)
        }
        digestRandomBytes(iv, 256)
        return iv
    }

    /**
     * Generates an AES key starting with an IV and applying the supplied user password.
     *
     *
     * This AES key is used to crypt IV 2 and AES key 2.
     * @return AES key of [.KEY_SIZE] bytes.
     */
    private fun generateAESKey1(iv: ByteArray, password: ByteArray): ByteArray {
        var aesKey = ByteArray(KEY_SIZE)
        iv.copyInto(aesKey, endIndex = iv.size)
        for (i in 0..8191) {
            digest.reset()
            digest.update(aesKey)
            digest.update(password)
            aesKey = digest.digest()
        }
        return aesKey
    }

    /**
     * Generates the random IV used to crypt file contents.
     * @return IV 2.
     */
    private fun generateIV2(): ByteArray {
        val iv = generateRandomBytes(BLOCK_SIZE)
        digestRandomBytes(iv, 256)
        return iv
    }

    /**
     * Generates the random AES key used to crypt file contents.
     * @return AES key of [.KEY_SIZE] bytes.
     */
    private fun generateAESKey2(): ByteArray {
        val aesKey = generateRandomBytes(KEY_SIZE)
        digestRandomBytes(aesKey, 32)
        return aesKey
    }

    /**
     * Changes the password this object uses to encrypt and decrypt.
     */
    private fun setPassword(password: String) {
        this.password = password.toByteArray(UTF_16LE)
        Log.v(TAG, "Using password: $password")
    }

    /**************
     * PUBLIC API *
     */

    /**
     * The input stream is encrypted and saved to the output stream.
     *
     *
     * <tt>version</tt> can be either 1 or 2.<br></br>
     * None of the streams are closed.
     * @throws IOException when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic methods.
     */
    @Suppress("LongMethod", "ComplexMethod")
    @Throws(IOException::class, GeneralSecurityException::class)
    fun encrypt(
        version: Int = AESCRYPT_SPEC_VERSION,
        `in`: InputStream,
        out: OutputStream,
        progressHandler: ProgressHandler
    ) {
        var text: ByteArray?
        ivSpec1 = IvParameterSpec(generateIv1())
        aesKey1 = SecretKeySpec(generateAESKey1(ivSpec1.iv, password), CRYPT_ALG)
        ivSpec2 = IvParameterSpec(generateIV2())
        aesKey2 = SecretKeySpec(generateAESKey2(), CRYPT_ALG)
        Log.v(TAG, "IV1: ${ivSpec1.iv.toHex()}")
        Log.v(TAG, "AES1: ${aesKey1.encoded.toHex()}")
        Log.v(TAG, "IV2: ${ivSpec2.iv.toHex()}")
        Log.v(TAG, "AES2: ${aesKey2.encoded.toHex()}")
        out.write(AESCRYPT_HEADER.toByteArray(UTF_8)) // Heading.
        out.write(version) // Version.
        out.write(0) // Reserved.
        if (version == AESCRYPT_SPEC_VERSION) { // No extensions.
            out.write(0)
            out.write(0)
        }
        out.write(ivSpec1.iv) // Initialization Vector.
        text = ByteArray(BLOCK_SIZE + KEY_SIZE)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey1, ivSpec1)
        cipher.update(ivSpec2.iv, 0, BLOCK_SIZE, text)
        cipher.doFinal(aesKey2.encoded, 0, KEY_SIZE, text, BLOCK_SIZE)
        out.write(text) // Crypted IV and key.
        Log.v(TAG, "IV2 + AES2 ciphertext: ${text.toHex()}")
        hmac.init(SecretKeySpec(aesKey1.encoded, HMAC_ALG))
        text = hmac.doFinal(text)
        out.write(text) // HMAC from previous cyphertext.
        Log.v(TAG, "HMAC1: ${text.toHex()}")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey2, ivSpec2)
        hmac.init(SecretKeySpec(aesKey2.encoded, HMAC_ALG))
        text = ByteArray(BLOCK_SIZE)
        var len: Int
        var last = 0
        while (`in`.read(text).also { len = it } > 0) {
            if (!progressHandler.cancelled) {
                cipher.update(text, 0, BLOCK_SIZE, text)
                hmac.update(text)
                out.write(text) // Crypted file data block.
                last = len
                ServiceWatcherUtil.position += len
            }
        }
        last = last and 0x0f
        out.write(last) // Last block size mod 16.
        Log.v(TAG, "Last block size mod 16: $last")
        text = hmac.doFinal()
        out.write(text) // HMAC from previous cyphertext.
        Log.v(TAG, "HMAC2: ${text.toHex()}")

        out.flush()
        `in`.close()
        out.close()
    }

    /**
     * The input stream is decrypted and saved to the output stream.
     *
     * The input file size is needed in advance.<br></br>
     * The input stream can be encrypted using version 1 or 2 of aescrypt.<br></br>
     * None of the streams are closed.
     *
     * Changes from original implementation: will flush and close input and output streams
     * gracefully, in align with our own encryption routine. - TranceLove
     *
     * @param inSize input stream size, for sanity checking
     * @param `in` AESCrypted source stream
     * @param out decrypted data output stream
     * @throws IncorrectEncryptedDataException if provided encrypted data cannot be parsed correctly
     * @throws DecryptFailureException if there is any problem during decryption
     * @throws GeneralSecurityException if the platform does not support the required cryptographic methods.
     */
    @Suppress("LongMethod", "ComplexMethod")
    @Throws(GeneralSecurityException::class)
    fun decrypt(inSize: Long, `in`: InputStream, out: OutputStream) {
        var text: ByteArray
        var total =
            (3 + 1 + 1 + BLOCK_SIZE + BLOCK_SIZE + KEY_SIZE + SHA_SIZE + 1 + SHA_SIZE).toLong()
        text = ByteArray(3)
        `in`.read(text) // Heading.
        if (text.toString(UTF_8) != "AES") {
            throw IncorrectEncryptedDataException("Invalid file header")
        }
        val version: Int = `in`.read() // Version.
        if (version < 1 || version > 2) {
            throw IncorrectEncryptedDataException("Unsupported version number: $version")
        }
        Log.v(TAG, "Version: $version")
        `in`.read() // Reserved.
        if (version == 2) { // Extensions.
            text = ByteArray(2)
            var len: Int
            do {
                `in`.read(text)
                len = 0xff and text[0].toInt() shl 8 or (0xff and text[1].toInt())
                if (`in`.skip(len.toLong()) != len.toLong()) {
                    throw IncorrectEncryptedDataException("Unexpected end of extension")
                }
                total += (2 + len).toLong()
                Log.i(TAG, "Skipped extension sized: $len")
            } while (len != 0)
        }
        text = ByteArray(BLOCK_SIZE)
        `in`.read(text) // Initialization Vector.
        ivSpec1 = IvParameterSpec(text)
        aesKey1 = SecretKeySpec(generateAESKey1(ivSpec1.iv, password), CRYPT_ALG)
        Log.v(TAG, "IV1: ${ivSpec1.iv.toHex()}")
        Log.v(TAG, "AES1: ${aesKey1.encoded.toHex()}")
        cipher.init(Cipher.DECRYPT_MODE, aesKey1, ivSpec1)
        var backup = ByteArray(BLOCK_SIZE + KEY_SIZE)
        `in`.read(backup) // IV and key to decrypt file contents.
        Log.v(TAG, "IV2 + AES2 ciphertext: ${backup.toHex()}")
        text = cipher.doFinal(backup)
        ivSpec2 = IvParameterSpec(text, 0, BLOCK_SIZE)
        aesKey2 = SecretKeySpec(text, BLOCK_SIZE, KEY_SIZE, CRYPT_ALG)
        Log.v(TAG, "IV2: ${ivSpec2.iv.toHex()}")
        Log.v(TAG, "AES2: ${aesKey2.encoded.toHex()}")
        hmac.init(SecretKeySpec(aesKey1.encoded, HMAC_ALG))
        backup = hmac.doFinal(backup)
        text = ByteArray(SHA_SIZE)
        `in`.read(text) // HMAC and authenticity test.
        if (!backup.contentEquals(text)) {
            throw DecryptFailureException("Message has been altered or password incorrect")
        }
        Log.v(TAG, "HMAC1: ${text.toHex()}")
        total = inSize - total // Payload size.
        if (total % BLOCK_SIZE != 0L) {
            throw DecryptFailureException(
                "Input file is corrupt. BLOCK_SIZE = $BLOCK_SIZE, total was $total"
            )
        }
        if (total == 0L) { // Hack: empty files won't enter block-processing for-loop below.
            `in`.read() // Skip last block size mod 16.
        }
        Log.v(TAG, "Payload size: $total")
        cipher.init(Cipher.DECRYPT_MODE, aesKey2, ivSpec2)
        hmac.init(SecretKeySpec(aesKey2.encoded, HMAC_ALG))
        backup = ByteArray(BLOCK_SIZE)
        text = ByteArray(BLOCK_SIZE)
        for (block in (total / BLOCK_SIZE).toInt() downTo 1) {
            var len = BLOCK_SIZE
            if (`in`.read(backup, 0, len) != len) { // Cyphertext block.
                throw DecryptFailureException("Unexpected end of file contents")
            }
            cipher.update(backup, 0, len, text)
            hmac.update(backup, 0, len)
            if (block == 1) {
                val last = `in`.read() // Last block size mod 16.
                Log.i(TAG, "Last block size mod 16: $last")
                len = if (last > 0) last else BLOCK_SIZE
            }
            out.write(text, 0, len)
        }
        out.write(cipher.doFinal())
        backup = hmac.doFinal()
        text = ByteArray(SHA_SIZE)
        `in`.read(text) // HMAC and authenticity test.
        if (!backup.contentEquals(text)) {
            throw DecryptFailureException("Message has been altered or password incorrect")
        }
        Log.v(TAG, "HMAC2: ${text.toHex()}")
        out.flush()
        `in`.close()
        out.close()
    }

    companion object {
        @JvmStatic
        private val TAG = AESCrypt::class.java.simpleName
        const val AESCRYPT_SPEC_VERSION = 2
        private const val AESCRYPT_HEADER = "AES"
        private const val RANDOM_ALG = "SHA1PRNG"
        private const val DIGEST_ALG = "SHA-256"
        private const val HMAC_ALG = "HmacSHA256"
        private const val CRYPT_ALG = "AES"
        private const val CRYPT_TRANS = "AES/CBC/NoPadding"
        private const val KEY_SIZE = 32
        private const val BLOCK_SIZE = 16
        private const val SHA_SIZE = 32
    }

    /**
     * Builds an object to encrypt or decrypt files with the given password.
     */
    init {
        setPassword(password)
        random = SecureRandom.getInstance(RANDOM_ALG)
        digest = MessageDigest.getInstance(DIGEST_ALG)
        cipher = Cipher.getInstance(CRYPT_TRANS)
        hmac = Mac.getInstance(HMAC_ALG)
    }

    /**
     * Exception representing provided encrypted data is incorrect
     */
    class IncorrectEncryptedDataException(message: String) : GeneralSecurityException(message)

    /**
     * Exception representing decryption errors
     */
    class DecryptFailureException(message: String) : GeneralSecurityException(message)
}
