package com.amaze.filemanager.filesystem.encryption;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import static com.amaze.filemanager.filesystem.encryption.CryptUtil.ALGO_AES;
import static com.amaze.filemanager.filesystem.encryption.CryptUtil.IV;
import static com.amaze.filemanager.filesystem.encryption.CryptUtil.KEY_ALIAS_AMAZE;
import static com.amaze.filemanager.filesystem.encryption.CryptUtil.KEY_STORE_ANDROID;

/**
 * @author Emmanuel
 *         on 6/6/2017, at 11:31.
 */
@RequiresApi(Build.VERSION_CODES.M)
class AESEncryption implements EncryptFunctions {
    @Override
    public void encrypt(Context context, BufferedInputStream inputStream,
                        BufferedOutputStream outputStream) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGO_AES);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());

        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);

        byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        int count;

        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);

        try {

            while ((count = inputStream.read(buffer)) != -1) {

                cipherOutputStream.write(buffer, 0, count);
                ServiceWatcherUtil.POSITION+=count;
            }
        } finally {

            cipherOutputStream.flush();
            cipherOutputStream.close();
            inputStream.close();
        }
    }

    @Override
    public void decrypt(Context context, BufferedInputStream inputStream,
                        BufferedOutputStream outputStream) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGO_AES);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());

        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
        CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);

        byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        int count;

        try {

            while ((count = cipherInputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, count);
                ServiceWatcherUtil.POSITION+=count;
            }
        } finally {

            outputStream.flush();
            cipherInputStream.close();
            outputStream.close();
        }
    }

    @Override
    public Key getSecretKey() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS_AMAZE)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_ANDROID);

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_ALIAS_AMAZE,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
            builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM);
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
            builder.setRandomizedEncryptionRequired(false);

            keyGenerator.init(builder.build());
            return keyGenerator.generateKey();
        } else {
            return keyStore.getKey(KEY_ALIAS_AMAZE, null);
        }
    }

    @Override
    public String encryptPassword(Context context, String plainText) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGO_AES);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
        byte[] encodedBytes = cipher.doFinal(plainText.getBytes());

        return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    }

    @Override
    public String decryptPassword(Context context, String cipherText) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGO_AES);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));

        return new String(decryptedBytes);
    }

    @Override
    public Cipher initCipher(Context context) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGO_AES);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
        return cipher;
    }
}
