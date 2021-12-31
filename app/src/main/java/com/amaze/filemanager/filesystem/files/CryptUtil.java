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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.file_operations.filesystem.encryption.EncryptDecrypt;
import com.amaze.filemanager.file_operations.filesystem.encryption.RsaKeygen;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.MakeDirectoryOperation;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.utils.ProgressHandler;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

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
 * PreferencesConstants#ENCRYPT_PASSWORD_FINGERPRINT} against the path in database. At the time of decryption,
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

  public static final String KEY_STORE_ANDROID = "AndroidKeyStore";
  public static final String KEY_ALIAS_AMAZE = "AmazeKey";
  // TODO: Generate a random IV every time, and keep track of it (in database against encrypted
  // files)
  public static final String IV =
      BuildConfig.CRYPTO_IV; // 12 byte long IV supported by android for GCM

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
  @WorkerThread
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
  @WorkerThread
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
      MakeDirectoryOperation.mkdirs(context, hFile);

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
      MakeDirectoryOperation.mkdirs(context, hFile);

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

  /**
   * Helper method to encrypt a file
   *
   * @param inputStream stream associated with the file to be encrypted
   * @param outputStream stream associated with new output encrypted file
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private void aesEncrypt(BufferedInputStream inputStream, BufferedOutputStream outputStream)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(EncryptDecrypt.ALGO_AES);

    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());

    cipher.init(Cipher.ENCRYPT_MODE, AmazeSpecificEncryptDecrypt.getSecretKey(), gcmParameterSpec);

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

    Cipher cipher = Cipher.getInstance(EncryptDecrypt.ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());

    cipher.init(Cipher.DECRYPT_MODE, AmazeSpecificEncryptDecrypt.getSecretKey(), gcmParameterSpec);
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


  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void rsaEncrypt(
      Context context, BufferedInputStream inputStream, BufferedOutputStream outputStream)
      throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(EncryptDecrypt.ALGO_AES);
    RsaKeygen keygen = new RsaKeygen(context, KEY_STORE_ANDROID, KEY_ALIAS_AMAZE);

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

    Cipher cipher = Cipher.getInstance(EncryptDecrypt.ALGO_AES);
    RsaKeygen keygen = new RsaKeygen(context, KEY_STORE_ANDROID, KEY_ALIAS_AMAZE);

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

}
