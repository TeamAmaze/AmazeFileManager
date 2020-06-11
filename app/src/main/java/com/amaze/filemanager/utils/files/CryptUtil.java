/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.utils.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.preference_fragments.PrefFrag;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.RequiresApi;

/**
 * Created by vishal on 6/4/17.
 *
 * <p>Class provide helper methods to encrypt/decrypt various type of files, or passwords We take
 * the password from user before encrypting file. First, the password is encrypted against the key
 * created in keystore in android {@see #encryptPassword(String)}. We're using AES encryption with
 * GCM as the processor algorithm. The encrypted password is mapped against the file path to be
 * encrypted in database for later use. This is handled by the service invoking this instance. The
 * service then calls the constructor which fires up the subsequent encryption/decryption process.
 *
 * <p>We differentiate between already encrypted files from <i>new ones</i> by encrypting the
 * plaintext {@link PrefFrag#ENCRYPT_PASSWORD_MASTER} and {@link
 * PrefFrag#ENCRYPT_PASSWORD_FINGERPRINT} against the path in database. At the time of decryption,
 * we check for these values and either retrieve master password from preferences or fire up the
 * fingerprint sensor authentication.
 *
 * <p>From <i>new ones</i> we mean the ones when were encrypted after user changed preference for
 * master password/fingerprint sensor from settings.
 *
 * <p>We use buffered streams to process files, usage of NIO will probably mildly effect the
 * performance.
 *
 * <p>Be sure to use constructors to encrypt/decrypt files only, and to call service through {@link
 * ServiceWatcherUtil} and to initialize watchers beforehand
 */
public class CryptUtil {

  private static final String ALGO_AES = "AES/GCM/NoPadding";
  private static final String ALGO_RSA = "RSA/ECB/PKCS1Padding";
  private static final String KEY_STORE_ANDROID = "AndroidKeyStore";
  private static final String KEY_ALIAS_AMAZE = "AmazeKey";
  private static final String PREFERENCE_KEY = "aes_key";
  // TODO: Generate a random IV every time, and keep track of it (in database against encrypted
  // files)
  private static final String IV = "LxbHiJhhUXcj"; // 12 byte long IV supported by android for GCM

  public static final String CRYPT_EXTENSION = ".aze";

  private ProgressHandler progressHandler;
  private ArrayList<HybridFile> failedOps;

  /**
   * Constructor will start encryption process serially. Make sure to call with background thread.
   * The result file of encryption will be in the same directory with a {@link #CRYPT_EXTENSION}
   * extension
   *
   * <p>Make sure you're done with encrypting password for this file and map it with this file in
   * database
   *
   * <p>Be sure to use constructors to encrypt/decrypt files only, and to call service through
   * {@link ServiceWatcherUtil} and to initialize watchers beforehand
   *
   * @param sourceFile the file to encrypt
   */
  public CryptUtil(
      Context context,
      HybridFileParcelable sourceFile,
      ProgressHandler progressHandler,
      ArrayList<HybridFile> failedOps,
      String targetFilename)
      throws GeneralSecurityException, IOException {

    this.progressHandler = progressHandler;
    this.failedOps = failedOps;

    // target encrypted file
    HybridFile hFile = new HybridFile(sourceFile.getMode(), sourceFile.getParent(context));
    encrypt(context, sourceFile, hFile, targetFilename);
  }

  /**
   * Decrypt the file in specified path. Can be used to open the file (decrypt in cache) or simply
   * decrypt the file in the same (or in a custom preference) directory Make sure to decrypt and
   * check user provided passwords beforehand from database
   *
   * <p>Be sure to use constructors to encrypt/decrypt files only, and to call service through
   * {@link ServiceWatcherUtil} and to initialize watchers beforehand
   *
   * @param baseFile the encrypted file
   * @param targetPath the directory in which file is to be decrypted the source's parent in normal
   *     case
   */
  public CryptUtil(
      Context context,
      HybridFileParcelable baseFile,
      String targetPath,
      ProgressHandler progressHandler,
      ArrayList<HybridFile> failedOps)
      throws GeneralSecurityException, IOException {

    this.progressHandler = progressHandler;
    this.failedOps = failedOps;

    HybridFile targetDirectory = new HybridFile(OpenMode.FILE, targetPath);
    if (!targetPath.equals(context.getExternalCacheDir())) {

      // same file system as of base file
      targetDirectory.setMode(baseFile.getMode());
    }

    decrypt(context, baseFile, targetDirectory);
  }

  /**
   * Wrapper around handling decryption for directory tree
   *
   * @param sourceFile the source file to decrypt
   * @param targetDirectory the target directory inside which we're going to decrypt
   */
  private void decrypt(
      final Context context, HybridFileParcelable sourceFile, HybridFile targetDirectory)
      throws GeneralSecurityException, IOException {
    if (progressHandler.getCancelled()) return;
    if (sourceFile.isDirectory()) {

      final HybridFile hFile =
          new HybridFile(
              targetDirectory.getMode(),
              targetDirectory.getPath(),
              sourceFile.getName(context).replace(CRYPT_EXTENSION, ""),
              sourceFile.isDirectory());
      FileUtil.mkdirs(context, hFile);

      sourceFile.forEachChildrenFile(
          context,
          sourceFile.isRoot(),
          file -> {
            try {
              decrypt(context, file, hFile);
            } catch (IOException | GeneralSecurityException e) {
              throw new IllegalStateException(e); // throw unchecked exception, no throws needed
            }
          });
    } else {

      if (!sourceFile.getPath().endsWith(CRYPT_EXTENSION)) {
        failedOps.add(sourceFile);
        return;
      }

      BufferedInputStream inputStream =
          new BufferedInputStream(
              sourceFile.getInputStream(context), GenericCopyUtil.DEFAULT_BUFFER_SIZE);

      HybridFile targetFile =
          new HybridFile(
              targetDirectory.getMode(),
              targetDirectory.getPath(),
              sourceFile.getName(context).replace(CRYPT_EXTENSION, ""),
              sourceFile.isDirectory());

      progressHandler.setFileName(sourceFile.getName(context));

      BufferedOutputStream outputStream =
          new BufferedOutputStream(
              targetFile.getOutputStream(context), GenericCopyUtil.DEFAULT_BUFFER_SIZE);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        aesDecrypt(inputStream, outputStream);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        rsaDecrypt(context, inputStream, outputStream);
      }
    }
  }

  /**
   * Wrapper around handling encryption in directory tree
   *
   * @param sourceFile the source file to encrypt
   * @param targetDirectory the target directory in which we're going to encrypt
   */
  private void encrypt(
      final Context context,
      HybridFileParcelable sourceFile,
      HybridFile targetDirectory,
      String targetFilename)
      throws GeneralSecurityException, IOException {

    if (progressHandler.getCancelled()) return;
    if (sourceFile.isDirectory()) {

      // succeed #CRYPT_EXTENSION at end of directory/file name
      final HybridFile hFile =
          new HybridFile(
              targetDirectory.getMode(),
              targetDirectory.getPath(),
              targetFilename,
              sourceFile.isDirectory());
      FileUtil.mkdirs(context, hFile);

      sourceFile.forEachChildrenFile(
          context,
          sourceFile.isRoot(),
          file -> {
            try {
              encrypt(context, file, hFile, file.getName(context).concat(CRYPT_EXTENSION));
            } catch (IOException | GeneralSecurityException e) {
              throw new IllegalStateException(e); // throw unchecked exception, no throws needed
            }
          });
    } else {

      if (sourceFile.getName(context).endsWith(CRYPT_EXTENSION)) {
        failedOps.add(sourceFile);
        return;
      }

      BufferedInputStream inputStream =
          new BufferedInputStream(
              sourceFile.getInputStream(context), GenericCopyUtil.DEFAULT_BUFFER_SIZE);

      // succeed #CRYPT_EXTENSION at end of directory/file name
      HybridFile targetFile =
          new HybridFile(
              targetDirectory.getMode(),
              targetDirectory.getPath(),
              targetFilename,
              sourceFile.isDirectory());

      progressHandler.setFileName(sourceFile.getName(context));

      BufferedOutputStream outputStream =
          new BufferedOutputStream(
              targetFile.getOutputStream(context), GenericCopyUtil.DEFAULT_BUFFER_SIZE);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        aesEncrypt(inputStream, outputStream);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        rsaEncrypt(context, inputStream, outputStream);
      }
    }
  }

  /** Helper method to encrypt plain text password */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private static String aesEncryptPassword(String plainTextPassword)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
    byte[] encodedBytes = cipher.doFinal(plainTextPassword.getBytes());

    return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
  }

  /** Helper method to decrypt cipher text password */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private static String aesDecryptPassword(String cipherPassword)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

    return new String(decryptedBytes);
  }

  /**
   * Helper method to encrypt a file
   *
   * @param inputStream stream associated with the file to be encrypted
   * @param outputStream stream associated with new output encrypted file
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private void aesEncrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);

    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());

    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);

    byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    int count;

    CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);

    try {

      while ((count = inputStream.read(buffer)) != -1) {
        if (!progressHandler.getCancelled()) {
          cipherOutputStream.write(buffer, 0, count);
          ServiceWatcherUtil.position += count;
        } else break;
      }
    } finally {

      cipherOutputStream.flush();
      cipherOutputStream.close();
      inputStream.close();
    }
  }

  /**
   * Helper method to decrypt file
   *
   * @param inputStream stream associated with encrypted file
   * @param outputStream stream associated with new output decrypted file
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private void aesDecrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());

    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
    CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);

    byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    int count;

    try {

      while ((count = cipherInputStream.read(buffer)) != -1) {
        if (!progressHandler.getCancelled()) {
          outputStream.write(buffer, 0, count);
          ServiceWatcherUtil.position += count;
        } else break;
      }
    } finally {

      outputStream.flush();
      cipherInputStream.close();
      outputStream.close();
    }
  }

  /**
   * Gets a secret key from Android key store. If no key has been generated with a given alias then
   * generate a new one
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private static Key getSecretKey() throws GeneralSecurityException, IOException {

    KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
    keyStore.load(null);

    if (!keyStore.containsAlias(KEY_ALIAS_AMAZE)) {
      KeyGenerator keyGenerator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_ANDROID);

      KeyGenParameterSpec.Builder builder =
          new KeyGenParameterSpec.Builder(
              KEY_ALIAS_AMAZE, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
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
  private void rsaEncrypt(
      Context context, BufferedInputStream inputStream, BufferedOutputStream outputStream)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RSAKeygen keygen = new RSAKeygen(context);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);

    byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    int count;

    CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
    try {

      while ((count = inputStream.read(buffer)) != -1) {
        if (!progressHandler.getCancelled()) {
          cipherOutputStream.write(buffer, 0, count);
          ServiceWatcherUtil.position += count;
        } else break;
      }
    } finally {

      cipherOutputStream.flush();
      cipherOutputStream.close();
      inputStream.close();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void rsaDecrypt(
      Context context, BufferedInputStream inputStream, BufferedOutputStream outputStream)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RSAKeygen keygen = new RSAKeygen(context);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);
    CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);

    byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    int count;

    try {

      while ((count = cipherInputStream.read(buffer)) != -1) {
        if (!progressHandler.getCancelled()) {
          outputStream.write(buffer, 0, count);
          ServiceWatcherUtil.position += count;
        } else break;
      }
    } finally {

      outputStream.flush();
      outputStream.close();
      cipherInputStream.close();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private static String rsaEncryptPassword(Context context, String password)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RSAKeygen keygen = new RSAKeygen(context);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);

    return Base64.encodeToString(cipher.doFinal(password.getBytes()), Base64.DEFAULT);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private static String rsaDecryptPassword(Context context, String cipherText)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RSAKeygen keygen = new RSAKeygen(context);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));

    return new String(decryptedBytes);
  }

  /** Method handles encryption of plain text on various APIs */
  public static String encryptPassword(Context context, String plainText)
      throws GeneralSecurityException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return aesEncryptPassword(plainText);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

      return rsaEncryptPassword(context, plainText);
    } else return plainText;
  }

  /** Method handles decryption of cipher text on various APIs */
  public static String decryptPassword(Context context, String cipherText)
      throws GeneralSecurityException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return aesDecryptPassword(cipherText);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return rsaDecryptPassword(context, cipherText);
    } else return cipherText;
  }

  /**
   * Method initializes a Cipher to be used by {@link
   * android.hardware.fingerprint.FingerprintManager}
   */
  public static Cipher initCipher(Context context) throws GeneralSecurityException, IOException {
    Cipher cipher = null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      cipher = Cipher.getInstance(ALGO_AES);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
      cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      cipher = Cipher.getInstance(ALGO_AES);
      RSAKeygen keygen = new RSAKeygen(context);

      cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());
    }
    return cipher;
  }

  /** Class responsible for generating key for API lower than M */
  static class RSAKeygen {

    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    RSAKeygen(Context context) {

      this.context = context;

      try {
        generateKeyPair(context);
        setKeyPreference();
      } catch (GeneralSecurityException | IOException e) {
        e.printStackTrace();
      }
    }

    /** Generates a RSA public/private key pair to encrypt AES key */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void generateKeyPair(Context context) throws GeneralSecurityException, IOException {

      KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
      keyStore.load(null);

      if (!keyStore.containsAlias(KEY_ALIAS_AMAZE)) {
        // generate a RSA key pair to encrypt/decrypt AES key from preferences
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", KEY_STORE_ANDROID);

        KeyPairGeneratorSpec spec =
            new KeyPairGeneratorSpec.Builder(context)
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

    /** Encrypts AES key and set into preference */
    private void setKeyPreference() throws GeneralSecurityException, IOException {

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
      String encodedAesKey = preferences.getString(PREFERENCE_KEY, null);

      if (encodedAesKey == null) {
        // generate encrypted aes key and save to preference

        byte[] key = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);

        byte[] encryptedKey = encryptAESKey(key);
        encodedAesKey = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
        preferences.edit().putString(PREFERENCE_KEY, encodedAesKey).apply();
      }
    }

    /** Encrypts randomly generated AES key using RSA public key */
    private byte[] encryptAESKey(byte[] secretKey) throws GeneralSecurityException, IOException {

      KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
      keyStore.load(null);
      KeyStore.PrivateKeyEntry keyEntry =
          (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS_AMAZE, null);
      Cipher cipher = Cipher.getInstance(ALGO_RSA, "AndroidOpenSSL");
      cipher.init(Cipher.ENCRYPT_MODE, keyEntry.getCertificate().getPublicKey());

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      CipherOutputStream outputStream = new CipherOutputStream(byteArrayOutputStream, cipher);
      outputStream.write(secretKey);
      outputStream.close();

      return byteArrayOutputStream.toByteArray();
    }

    /** Decodes encrypted AES key from preference and decrypts using RSA private key */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private Key getSecretKey() throws GeneralSecurityException, IOException {

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
      String encodedString = preferences.getString(PREFERENCE_KEY, null);
      if (encodedString != null) {

        return new SecretKeySpec(
            decryptAESKey(Base64.decode(encodedString, Base64.DEFAULT)), "AES");
      } else {
        generateKeyPair(context);
        setKeyPreference();
        return getSecretKey();
      }
    }

    /** Decrypts AES decoded key from preference using RSA private key */
    private byte[] decryptAESKey(byte[] encodedBytes) throws GeneralSecurityException, IOException {

      KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ANDROID);
      keyStore.load(null);
      KeyStore.PrivateKeyEntry keyEntry =
          (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS_AMAZE, null);
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
      for (int i = 0; i < bytes.size(); i++) {

        decryptedBytes[i] = bytes.get(i).byteValue();
      }
      return decryptedBytes;
    }
  }
}
