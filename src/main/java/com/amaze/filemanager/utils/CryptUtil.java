package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Created by vishal on 6/4/17.
 *
 * Class provide helper methods to encrypt/decrypt various type of files, or passwords
 * We take the password from user before encrypting file. First, the password is encrypted against
 * the key created in keystore in android {@see #encryptPassword(String)}.
 * We're using AES encryption with GCM as the processor algorithm.
 * The encrypted password is mapped against the file path to be encrypted in database for later use.
 * This is handled by the service invoking this instance.
 * The service then calls the constructor which fires up the subsequent encryption/decryption process.
 *
 * We differentiate between already encrypted files from <i>new ones</i> by encrypting the plaintext
 * {@link com.amaze.filemanager.fragments.preference_fragments.Preffrag#ENCRYPT_PASSWORD_MASTER}
 * and {@link com.amaze.filemanager.fragments.preference_fragments.Preffrag#ENCRYPT_PASSWORD_FINGERPRINT}
 * against the path in database. At the time of decryption, we check for these values
 * and either retrieve master password from preferences or fire up the fingerprint sensor authentication.
 *
 * From <i>new ones</i> we mean the ones when were encrypted after user changed preference
 * for master password/fingerprint sensor from settings.
 *
 * We use buffered streams to process files, usage of NIO will probably mildly effect the performance.
 *
 * Be sure to use constructors to encrypt/decrypt files only, and to call service through
 * {@link ServiceWatcherUtil} and to initialize watchers beforehand
 */

public class CryptUtil {

    private static final String ALGO_AES = "AES/GCM/NoPadding";
    private static final String ALGO_RSA = "RSA/ECB/PKCS1Padding";
    private static final String KEY_STORE_ANDROID = "AndroidKeyStore";
    private static final String KEY_ALIAS_AMAZE = "AmazeKey";
    private static final String PREFERENCE_KEY = "aes_key";
    // TODO: Generate a random IV every time, and keep track of it (in database against encrypted files)
    private static final String IV = "LxbHiJhhUXcj";    // 12 byte long IV supported by android for GCM

    public static final String CRYPT_EXTENSION = ".aze";

    private ProgressHandler progressHandler;
    private ArrayList<HFile> failedOps;

    /**
     * Constructor will start encryption process serially. Make sure to call with background thread.
     * The result file of encryption will be in the same directory with a {@link #CRYPT_EXTENSION} extension
     *
     * Make sure you're done with encrypting password for this file and map it with this file in database
     *
     * Be sure to use constructors to encrypt/decrypt files only, and to call service through
     * {@link ServiceWatcherUtil} and to initialize watchers beforehand
     *
     * @param context
     * @param sourceFile the file to encrypt
     */
    public CryptUtil(Context context, BaseFile sourceFile, ProgressHandler progressHandler,
                     ArrayList<HFile> failedOps)
            throws IOException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableEntryException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchProviderException,
            BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        this.progressHandler = progressHandler;
        this.failedOps = failedOps;

        // target encrypted file
        HFile hFile = new HFile(sourceFile.getMode(), sourceFile.getParent(context));

        encrypt(context, sourceFile, hFile);
    }

    /**
     * Decrypt the file in specified path. Can be used to open the file (decrypt in cache) or
     * simply decrypt the file in the same (or in a custom preference) directory
     * Make sure to decrypt and check user provided passwords beforehand from database
     *
     * Be sure to use constructors to encrypt/decrypt files only, and to call service through
     * {@link ServiceWatcherUtil} and to initialize watchers beforehand
     *
     * @param context
     * @param baseFile the encrypted file
     * @param targetPath the directory in which file is to be decrypted
     *                   the source's parent in normal case
     */
    public CryptUtil(Context context, BaseFile baseFile, String targetPath,
                     ProgressHandler progressHandler, ArrayList<HFile> failedOps)
            throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        this.progressHandler = progressHandler;
        this.failedOps = failedOps;

        HFile targetDirectory = new HFile(OpenMode.FILE, targetPath);
        if (!targetPath.equals(context.getExternalCacheDir())) {

            // same file system as of base file
            targetDirectory.setMode(baseFile.getMode());
        }

        decrypt(context, baseFile, targetDirectory);
    }

    /**
     * Wrapper around handling decryption for directory tree
     * @param context
     * @param sourceFile        the source file to decrypt
     * @param targetDirectory   the target directory inside which we're going to decrypt
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableEntryException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws NoSuchProviderException
     * @throws BadPaddingException
     * @throws KeyStoreException
     * @throws IllegalBlockSizeException
     */
    private void decrypt(Context context, BaseFile sourceFile, HFile targetDirectory) throws IOException,
            CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        if (sourceFile.isDirectory()) {

            HFile hFile = new HFile(targetDirectory.getMode(), targetDirectory.getPath(),
                    sourceFile.getName().replace(CRYPT_EXTENSION, ""), sourceFile.isDirectory());
            FileUtil.mkdirs(context, hFile);

            for (BaseFile baseFile : sourceFile.listFiles(context, sourceFile.isRoot())) {
                decrypt(context, baseFile, hFile);
            }
        } else {

            if (!sourceFile.getPath().endsWith(CRYPT_EXTENSION)) {
                failedOps.add(sourceFile);
                return;
            }

            BufferedInputStream inputStream = new BufferedInputStream(sourceFile.getInputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            HFile targetFile = new HFile(targetDirectory.getMode(),
                    targetDirectory.getPath(), sourceFile.getName().replace(CRYPT_EXTENSION, ""),
                    sourceFile.isDirectory());

            progressHandler.setFileName(sourceFile.getName());

            BufferedOutputStream outputStream = new BufferedOutputStream(targetFile.getOutputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            if (progressHandler.getCancelled()) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                aesDecrypt(inputStream, outputStream);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                rsaDecrypt(context, inputStream, outputStream);
            }
        }
    }

    /**
     * Wrapper around handling encryption in directory tree
     * @param context
     * @param sourceFile        the source file to encrypt
     * @param targetDirectory   the target directory in which we're going to encrypt
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableEntryException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws NoSuchProviderException
     * @throws BadPaddingException
     * @throws KeyStoreException
     * @throws IllegalBlockSizeException
     */
    private void encrypt(Context context, BaseFile sourceFile, HFile targetDirectory) throws IOException,
            CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        if (sourceFile.isDirectory()) {

            // succeed #CRYPT_EXTENSION at end of directory/file name
            HFile hFile = new HFile(targetDirectory.getMode(),
                    targetDirectory.getPath(), sourceFile.getName() + CRYPT_EXTENSION,
                    sourceFile.isDirectory());
            FileUtil.mkdirs(context, hFile);

            for (BaseFile baseFile : sourceFile.listFiles(context, sourceFile.isRoot())) {
                encrypt(context, baseFile, hFile);
            }
        } else {

            if (sourceFile.getName().endsWith(CRYPT_EXTENSION)) {
                failedOps.add(sourceFile);
                return;
            }

            BufferedInputStream inputStream = new BufferedInputStream(sourceFile.getInputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            // succeed #CRYPT_EXTENSION at end of directory/file name
            HFile targetFile = new HFile(targetDirectory.getMode(),
                    targetDirectory.getPath(), sourceFile.getName() + CRYPT_EXTENSION,
                    sourceFile.isDirectory());

            progressHandler.setFileName(sourceFile.getName());

            BufferedOutputStream outputStream = new BufferedOutputStream(targetFile.getOutputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            if (progressHandler.getCancelled()) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                aesEncrypt(inputStream, outputStream);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                rsaEncrypt(context, inputStream, outputStream);
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static String aesEncryptPassword(String plainTextPassword)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            NoSuchProviderException, InvalidAlgorithmParameterException, IOException,
            NoSuchPaddingException, UnrecoverableKeyException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ALGO_AES);
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static String aesDecryptPassword(String cipherPassword) throws NoSuchPaddingException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException,
            KeyStoreException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(ALGO_AES);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

        return new String(decryptedBytes);
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void aesEncrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            NoSuchProviderException, InvalidAlgorithmParameterException, IOException,
            NoSuchPaddingException, UnrecoverableKeyException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void aesDecrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws NoSuchPaddingException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyStoreException, NoSuchProviderException,
            InvalidAlgorithmParameterException, IOException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

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
    @RequiresApi(api = Build.VERSION_CODES.M)
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void rsaEncrypt(Context context, BufferedInputStream inputStream, BufferedOutputStream outputStream)
            throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException,
            CertificateException, BadPaddingException, InvalidAlgorithmParameterException,
            KeyStoreException, UnrecoverableEntryException, IllegalBlockSizeException,
            InvalidKeyException, IOException {

        Cipher cipher = Cipher.getInstance(ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());

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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void rsaDecrypt(Context context, BufferedInputStream inputStream,
                                   BufferedOutputStream outputStream) throws NoSuchPaddingException,
            NoSuchAlgorithmException, NoSuchProviderException, CertificateException,
            BadPaddingException, InvalidAlgorithmParameterException, KeyStoreException,
            UnrecoverableEntryException, IllegalBlockSizeException, InvalidKeyException, IOException {

        Cipher cipher = Cipher.getInstance(ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey());
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
            outputStream.close();
            cipherInputStream.close();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static String rsaEncryptPassword(Context context, String password) throws
            NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException,
            CertificateException, BadPaddingException, InvalidAlgorithmParameterException,
            KeyStoreException, UnrecoverableEntryException, IllegalBlockSizeException,
            InvalidKeyException, IOException {

        Cipher cipher = Cipher.getInstance(ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());

        return Base64.encodeToString(cipher.doFinal(password.getBytes()), Base64.DEFAULT);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static String rsaDecryptPassword(Context context, String cipherText) throws
            NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException,
            CertificateException, BadPaddingException, InvalidAlgorithmParameterException,
            KeyStoreException, UnrecoverableEntryException, IllegalBlockSizeException,
            InvalidKeyException, IOException {

        Cipher cipher = Cipher.getInstance(ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);
        cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey());
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));

        return decryptedBytes.toString();
    }

    /**
     * Method handles encryption of plain text on various APIs
     * @param context
     * @param plainText
     * @return
     */
    public static String encryptPassword(Context context, String plainText) throws IOException,
            CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            return CryptUtil.aesEncryptPassword(plainText);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

            return CryptUtil.rsaEncryptPassword(context, plainText);
        } else return plainText;
    }

    /**
     * Method handles decryption of cipher text on various APIs
     * @param context
     * @param cipherText
     * @return
     */
    public static String decryptPassword(Context context, String cipherText) throws IOException,
            CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            return CryptUtil.aesDecryptPassword(cipherText);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

            return CryptUtil.rsaDecryptPassword(context, cipherText);
        } else return cipherText;
    }

    /**
     * Method initializes a Cipher to be used by {@link android.hardware.fingerprint.FingerprintManager}
     * @param context
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableEntryException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static Cipher initCipher(Context context) throws NoSuchPaddingException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableEntryException, KeyStoreException,
            NoSuchProviderException, InvalidAlgorithmParameterException, IOException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            cipher = Cipher.getInstance(ALGO_AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            cipher = Cipher.getInstance(ALGO_AES, "BC");
            RSAKeygen keygen = new RSAKeygen(context);

            cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());
        }
        return cipher;
    }

    /**
     * Class responsible for generating key for API lower than M
     */
    static class RSAKeygen {

        private Context context;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        RSAKeygen(Context context) {

            this.context = context;

            try {
                generateKeyPair(context);
                setKeyPreference();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (UnrecoverableEntryException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
        }

        /**
         * Generates a RSA public/private key pair to encrypt AES key
         * @param context
         * @throws KeyStoreException
         * @throws CertificateException
         * @throws NoSuchAlgorithmException
         * @throws IOException
         * @throws NoSuchProviderException
         * @throws InvalidAlgorithmParameterException
         */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        private void generateKeyPair(Context context) throws KeyStoreException,
                CertificateException, NoSuchAlgorithmException, IOException, NoSuchProviderException,
                InvalidAlgorithmParameterException {

            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS_AMAZE)) {
                // generate a RSA key pair to encrypt/decrypt AES key from preferences
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGO_RSA, KEY_STORE_ANDROID);

                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(KEY_ALIAS_AMAZE)
                        .setSubject(new X500Principal("CN=" + KEY_ALIAS_AMAZE))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();

                keyPairGenerator.initialize(spec);
                keyPairGenerator.generateKeyPair();
            }
        }

        /**
         * Encrypts AES key and set into preference
         */
        private void setKeyPreference() throws IOException, CertificateException,
                NoSuchAlgorithmException, InvalidKeyException, UnrecoverableEntryException,
                NoSuchPaddingException, NoSuchProviderException, BadPaddingException,
                KeyStoreException, IllegalBlockSizeException {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String encodedAesKey = preferences.getString(PREFERENCE_KEY, null);

            if (encodedAesKey==null) {
                // generate encrypted aes key and save to preference

                byte[] key = new byte[16];
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextBytes(key);

                byte[] encryptedKey = encryptAESKey(key);
                encodedAesKey = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
                preferences.edit().putString(PREFERENCE_KEY, encodedAesKey).apply();
            }
        }

        /**
         * Encrypts randomly generated AES key using RSA public key
         * @param secretKey
         * @return
         */
        private byte[] encryptAESKey(byte[] secretKey) throws KeyStoreException,
                UnrecoverableEntryException, NoSuchAlgorithmException, IOException,
                CertificateException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(KEY_ALIAS_AMAZE, null);
            Cipher cipher = Cipher.getInstance(ALGO_RSA, "AndroidOpenSSL");
            cipher.init(Cipher.ENCRYPT_MODE, keyEntry.getCertificate().getPublicKey());

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            CipherOutputStream outputStream = new CipherOutputStream(byteArrayOutputStream, cipher);
            outputStream.write(secretKey);
            outputStream.close();

            return byteArrayOutputStream.toByteArray();
        }

        /**
         * Decodes encrypted AES key from preference and decrypts using RSA private key
         * @return
         * @throws CertificateException
         * @throws NoSuchPaddingException
         * @throws InvalidKeyException
         * @throws NoSuchAlgorithmException
         * @throws KeyStoreException
         * @throws NoSuchProviderException
         * @throws UnrecoverableEntryException
         * @throws IOException
         * @throws InvalidAlgorithmParameterException
         * @throws BadPaddingException
         * @throws IllegalBlockSizeException
         */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public Key getSecretKey() throws CertificateException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, UnrecoverableEntryException, IOException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String encodedString = preferences.getString(PREFERENCE_KEY, null);
            if (encodedString != null) {

                return new SecretKeySpec(decryptAESKey(Base64.decode(encodedString, Base64.DEFAULT)),
                        "AES");
            } else {
                generateKeyPair(context);
                setKeyPreference();
                return getSecretKey();
            }
        }

        /**
         * Decrypts AES decoded key from preference using RSA private key
         * @param encodedBytes
         * @return
         * @throws KeyStoreException
         * @throws CertificateException
         * @throws NoSuchAlgorithmException
         * @throws IOException
         * @throws UnrecoverableEntryException
         * @throws NoSuchProviderException
         * @throws NoSuchPaddingException
         * @throws InvalidKeyException
         */
        private byte[] decryptAESKey(byte[] encodedBytes) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {

            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(KEY_ALIAS_AMAZE, null);
            Cipher cipher = Cipher.getInstance(ALGO_RSA, "AndroidOpenSSL");
            cipher.init(Cipher.DECRYPT_MODE, keyEntry.getPrivateKey());

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedBytes);
            CipherInputStream inputStream = new CipherInputStream(byteArrayInputStream, cipher);
            ArrayList<Byte> bytes = new ArrayList<>();
            int nextByte;
            while ((nextByte = inputStream.read()) != -1) {
                bytes.add((byte) nextByte);
            }

            byte[] decryptedBytes = new byte[bytes.size()];
            for (int i=0; i<bytes.size(); i++) {

                decryptedBytes[i] = bytes.get(i).byteValue();
            }
            return decryptedBytes;
        }
    }
}
