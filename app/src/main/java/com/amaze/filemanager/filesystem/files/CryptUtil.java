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

package com.amaze.filemanager.filesystem.files;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.MakeDirectoryOperation;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.utils.AESCrypt;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.security.SecretKeygen;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.io.ByteStreamsKt;
import kotlin.io.ConstantsKt;

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
 * plaintext {@link PreferencesConstants#ENCRYPT_PASSWORD_MASTER} and {@link
 * PreferencesConstants#ENCRYPT_PASSWORD_FINGERPRINT} against the path in database. At the time of
 * decryption, we check for these values and either retrieve master password from preferences or
 * fire up the fingerprint sensor authentication.
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

  public static final String KEY_STORE_ANDROID = "AndroidKeyStore";
  public static final String KEY_ALIAS_AMAZE = "AmazeKey";
  public static final String ALGO_AES = "AES/GCM/NoPadding";
  // TODO: Generate a random IV every time, and keep track of it (in database against encrypted
  // files)
  private static final String IV =
      BuildConfig.CRYPTO_IV; // 12 byte long IV supported by android for GCM
  private static final int GCM_TAG_LENGTH = 128;
  private final Logger LOG = LoggerFactory.getLogger(CryptUtil.class);

  public static final String CRYPT_EXTENSION = ".aze";
  public static final String AESCRYPT_EXTENSION = ".aes";

  private final ProgressHandler progressHandler;
  private final ArrayList<HybridFile> failedOps;

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
      @NonNull Context context,
      @NonNull HybridFileParcelable sourceFile,
      @NonNull ProgressHandler progressHandler,
      @NonNull ArrayList<HybridFile> failedOps,
      @NonNull String targetFilename,
      boolean useAesCrypt,
      @Nullable String password)
      throws GeneralSecurityException, IOException {

    this.progressHandler = progressHandler;
    this.failedOps = failedOps;

    // target encrypted file
    HybridFile hFile = new HybridFile(sourceFile.getMode(), sourceFile.getParent(context));
    encrypt(context, sourceFile, hFile, targetFilename, useAesCrypt, password);
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
      @NonNull Context context,
      @NonNull HybridFileParcelable baseFile,
      @NonNull String targetPath,
      @NonNull ProgressHandler progressHandler,
      @NonNull ArrayList<HybridFile> failedOps,
      @Nullable String password)
      throws GeneralSecurityException, IOException {

    this.progressHandler = progressHandler;
    this.failedOps = failedOps;
    boolean useAesCrypt = baseFile.getName().endsWith(AESCRYPT_EXTENSION);

    HybridFile targetDirectory = new HybridFile(OpenMode.FILE, targetPath);
    if (!targetPath.equals(context.getExternalCacheDir())) {

      // same file system as of base file
      targetDirectory.setMode(baseFile.getMode());
    }
    decrypt(context, baseFile, targetDirectory, useAesCrypt, password);
  }

  /**
   * Wrapper around handling decryption for directory tree
   *
   * @param sourceFile the source file to decrypt
   * @param targetDirectory the target directory inside which we're going to decrypt
   */
  private void decrypt(
      @NonNull final Context context,
      @NonNull HybridFileParcelable sourceFile,
      @NonNull HybridFile targetDirectory,
      boolean useAescrypt,
      @Nullable String password)
      throws GeneralSecurityException, IOException {

    if (progressHandler.getCancelled()) return;
    if (sourceFile.isDirectory()) {

      final HybridFile hFile =
          new HybridFile(
              targetDirectory.getMode(),
              targetDirectory.getPath(),
              sourceFile
                  .getName(context)
                  .replace(CRYPT_EXTENSION, "")
                  .replace(AESCRYPT_EXTENSION, ""),
              sourceFile.isDirectory());
      MakeDirectoryOperation.mkdirs(context, hFile);

      sourceFile.forEachChildrenFile(
          context,
          sourceFile.isRoot(),
          file -> {
            try {
              decrypt(context, file, hFile, useAescrypt, password);
            } catch (IOException | GeneralSecurityException e) {
              throw new IllegalStateException(e); // throw unchecked exception, no throws needed
            }
          });
    } else {

      if (!sourceFile.getPath().endsWith(CRYPT_EXTENSION)
          && !sourceFile.getPath().endsWith(AESCRYPT_EXTENSION)) {
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
              sourceFile
                  .getName(context)
                  .replace(CRYPT_EXTENSION, "")
                  .replace(AESCRYPT_EXTENSION, ""),
              sourceFile.isDirectory());

      progressHandler.setFileName(sourceFile.getName(context));

      BufferedOutputStream outputStream =
          new BufferedOutputStream(
              targetFile.getOutputStream(context), GenericCopyUtil.DEFAULT_BUFFER_SIZE);

      if (useAescrypt) {
        new AESCrypt(password).decrypt(sourceFile.getSize(), inputStream, outputStream);
      } else {
        doEncrypt(inputStream, outputStream, Cipher.DECRYPT_MODE);
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
      @NonNull final Context context,
      @NonNull HybridFileParcelable sourceFile,
      @NonNull HybridFile targetDirectory,
      @NonNull String targetFilename,
      boolean useAesCrypt,
      @Nullable String password)
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
      MakeDirectoryOperation.mkdirs(context, hFile);

      sourceFile.forEachChildrenFile(
          context,
          sourceFile.isRoot(),
          file -> {
            try {
              encrypt(
                  context,
                  file,
                  hFile,
                  file.getName(context).concat(useAesCrypt ? AESCRYPT_EXTENSION : CRYPT_EXTENSION),
                  useAesCrypt,
                  password);
            } catch (IOException | GeneralSecurityException e) {
              throw new IllegalStateException(e); // throw unchecked exception, no throws needed
            }
          });
    } else {

      if (sourceFile.getName(context).endsWith(CRYPT_EXTENSION)
          || sourceFile.getName(context).endsWith(AESCRYPT_EXTENSION)) {
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

      if (useAesCrypt) {
        new AESCrypt(password)
            .encrypt(
                AESCrypt.AESCRYPT_SPEC_VERSION,
                sourceFile.getInputStream(AppConfig.getInstance()),
                targetFile.getOutputStream(AppConfig.getInstance()),
                progressHandler);
      } else {
        doEncrypt(inputStream, outputStream, Cipher.ENCRYPT_MODE);
      }
    }
  }

  /**
   * Core encryption/decryption routine.
   *
   * @param inputStream stream associated with the file to be encrypted
   * @param outputStream stream associated with new output encrypted file
   * @param operationMode either <code>Cipher.ENCRYPT_MODE</code> or <code>Cipher.DECRYPT_MODE
   *     </code>
   */
  private void doEncrypt(
      BufferedInputStream inputStream, BufferedOutputStream outputStream, int operationMode)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    AlgorithmParameterSpec parameterSpec;
    if (SDK_INT >= KITKAT) {
      parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, IV.getBytes());
    } else {
      parameterSpec = new IvParameterSpec(IV.getBytes());
    }

    Key secretKey = SecretKeygen.INSTANCE.getSecretKey();
    if (secretKey == null) {
      // Discard crypto setup objects and just pipe input to output
      parameterSpec = null;
      cipher = null;
      ByteStreamsKt.copyTo(inputStream, outputStream, ConstantsKt.DEFAULT_BUFFER_SIZE);
      inputStream.close();
      outputStream.close();
    } else {
      cipher.init(operationMode, SecretKeygen.INSTANCE.getSecretKey(), parameterSpec);

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
      } catch (Exception x) {
        LOG.error("I/O error writing output", x);
      } finally {
        cipherOutputStream.flush();
        cipherOutputStream.close();
        inputStream.close();
        outputStream.close();
      }
    }
  }

  /**
   * Method initializes a Cipher to be used by {@link
   * android.hardware.fingerprint.FingerprintManager}
   */
  public static Cipher initCipher() throws GeneralSecurityException {
    Cipher cipher = null;
    if (SDK_INT >= KITKAT) {
      cipher = Cipher.getInstance(ALGO_AES);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, IV.getBytes());
      cipher.init(Cipher.ENCRYPT_MODE, SecretKeygen.INSTANCE.getSecretKey(), gcmParameterSpec);
    } else if (SDK_INT >= JELLY_BEAN_MR2) {
      cipher = Cipher.getInstance(ALGO_AES);
      cipher.init(Cipher.ENCRYPT_MODE, SecretKeygen.INSTANCE.getSecretKey());
    }
    return cipher;
  }
}
