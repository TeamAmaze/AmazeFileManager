package com.amaze.filemanager.utils;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Created by vishal on 6/4/17.
 *
 * Class provide helper methods to encrypt/decrypt various type of files, or passwords
 * We take the password from user before encrypting file. First, the password is first encrypted against
 * the key created in keystore in android {@see #encryptPassword(String)}.
 * We're using AES encryption with GCM as the processor algorithm.
 * The encrypted password is mapped against the file path to be encrypted in database for later use.
 * This is handled by the service invoking this instance.
 * The service then calls the constructor which fires up the subsequent encryption/decryption process.
 *
 * We use buffered streams to process files, usage of NIO will probably mildly effect the performance.
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class CryptUtil {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final String KEY_STORE_ANDROID = "AndroidKeyStore";
    private static final String KEY_ALIAS_AMAZE = "AmazeKey";
    // TODO: Generate a random IV every time, and keep track of it (in database against encrypted files)
    private static final String IV = "uoRxVW";    // 12 byte long IV supported by android for GCM

    /**
     * Constructor will start encryption process serially. Make sure to call with background thread.
     * The result file of encryption will be in the same directory with a .sec extension
     * Make sure you're done with encrypting password for this file and map it with this file in database
     * @param context
     * @param sourceFile the file to encrypt
     */
    public CryptUtil(Context context, BaseFile sourceFile) {

        BufferedInputStream inputStream = new BufferedInputStream(sourceFile.getInputStream(context),
                GenericCopyUtil.DEFAULT_BUFFER_SIZE);

        // target encrypted file
        HFile hFile = new HFile(sourceFile.getMode(),
                sourceFile.getParent(context), sourceFile.getName(context) + ".sec",
                sourceFile.isDirectory(context));

        BufferedOutputStream outputStream = new BufferedOutputStream(hFile.getOutputStream(context),
                GenericCopyUtil.DEFAULT_BUFFER_SIZE);

        try {
            encrypt(inputStream, outputStream);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Decrypt the file in specified path. Can be used to open the file (decrypt in cache) or
     * simply decrypt the file in the same (or in a custom preference) directory
     * @param context
     * @param baseFile the encrypted file
     * @param targetPath the directory in which file is to be decrypted
     */
    public CryptUtil(Context context, BaseFile baseFile, BaseFile targetPath) {

        BufferedInputStream inputStream =  new BufferedInputStream(baseFile.getInputStream(context),
                GenericCopyUtil.DEFAULT_BUFFER_SIZE);

        // target decrypted file
        HFile targetFile = new HFile(targetPath.getMode(), targetPath.getPath(),
                baseFile.getName(), baseFile.isDirectory());

        BufferedOutputStream outputStream = new BufferedOutputStream(targetFile.getOutputStream(context),
                GenericCopyUtil.DEFAULT_BUFFER_SIZE);

        try {
            decrypt(inputStream, outputStream);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to encrypt plain text password
     * @param plainTextPassword
     * @return
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws NoSuchPaddingException
     * @throws UnrecoverableKeyException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String encryptPassword(String plainTextPassword)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            NoSuchProviderException, InvalidAlgorithmParameterException, IOException,
            NoSuchPaddingException, UnrecoverableKeyException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
        byte[] encodedBytes = cipher.doFinal(plainTextPassword.getBytes());

        return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    }

    /**
     * Helper method to decrypt cipher text password
     * @param cipherPassword
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String decryptPassword(String cipherPassword) throws NoSuchPaddingException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException,
            KeyStoreException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

        return decryptedBytes.toString();
    }

    /**
     * Helper method to encrypt a file
     * @param inputStream stream associated with the file to be encrypted
     * @param outputStream stream associated with new output encrypted file
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws NoSuchPaddingException
     * @throws UnrecoverableKeyException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private static void encrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            NoSuchProviderException, InvalidAlgorithmParameterException, IOException,
            NoSuchPaddingException, UnrecoverableKeyException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);

        byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        int count;

        while ((count = inputStream.read(buffer)) != -1) {
            if (count != -1) {
                byte[] encodedBytes = cipher.doFinal(buffer, 0, count);
                outputStream.write(encodedBytes, 0, count);
                ServiceWatcherUtil.POSITION+=count;
            }
        }
        outputStream.flush();
    }

    /**
     * Helper method to decrypt file
     * @param inputStream stream associated with encrypted file
     * @param outputStream stream associated with new output decrypted file
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private static void decrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws NoSuchPaddingException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyStoreException, NoSuchProviderException,
            InvalidAlgorithmParameterException, IOException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);

        byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        int count;

        while ((count = inputStream.read(buffer)) != -1) {
            if (count != -1) {
                byte[] decodedBytes = cipher.doFinal(buffer, 0, count);
                outputStream.write(decodedBytes, 0, count);
                ServiceWatcherUtil.POSITION+=count;
            }
        }
        outputStream.flush();
    }

    /**
     * Gets a secret key from Android key store.
     * If no key has been generated with a given alias then generate a new one
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws UnrecoverableKeyException
     */
    private static Key getSecretKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, NoSuchProviderException,
            InvalidAlgorithmParameterException,
            UnrecoverableKeyException {

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
}
