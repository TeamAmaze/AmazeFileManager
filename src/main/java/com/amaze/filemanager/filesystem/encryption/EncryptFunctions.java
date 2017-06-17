package com.amaze.filemanager.filesystem.encryption;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;

/**
 * @author Emmanuel
 *         on 6/6/2017, at 11:25.
 */

public interface EncryptFunctions {

    /**
     * Helper method to encrypt a file
     * @param inputStream stream associated with the file to be encrypted
     * @param outputStream stream associated with new output encrypted file
     * @throws IOException
     * @throws GeneralSecurityException
     */
     void encrypt(Context context, BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws IOException, GeneralSecurityException;

    /**
     * Helper method to decrypt file
     * @param inputStream stream associated with encrypted file
     * @param outputStream stream associated with new output decrypted file
     * @throws IOException
     * @throws GeneralSecurityException
     */
     void decrypt(Context context, BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws IOException, GeneralSecurityException;

    /**
     * Gets a secret key from Android key store.
     * If no key has been generated with a given alias then generate a new one
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
     Key getSecretKey() throws IOException, GeneralSecurityException;
    
    /**
     * Method handles encryption of plain text on various APIs
     * @param context
     * @param plainText
     * @return
     */
     String encryptPassword(Context context, String plainText)
            throws IOException, GeneralSecurityException;
    
    /**
     * Method handles decryption of cipher text on various APIs
     * @param context
     * @param cipherText
     * @return
     */
     String decryptPassword(Context context, String cipherText)
            throws IOException, GeneralSecurityException;

    /**
     * Method initializes a Cipher to be used by {@link android.hardware.fingerprint.FingerprintManager}
     * @param context
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
     Cipher initCipher(Context context) throws IOException, GeneralSecurityException;
}
