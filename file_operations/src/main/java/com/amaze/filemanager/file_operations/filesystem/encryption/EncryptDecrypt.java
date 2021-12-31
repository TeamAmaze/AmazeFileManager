package com.amaze.filemanager.file_operations.filesystem.encryption;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class EncryptDecrypt {
  public static final String ALGO_AES = "AES/GCM/NoPadding";
  public static final String ALGO_RSA = "RSA/ECB/PKCS1Padding";

  /** Helper method to encrypt plain text password */
  @RequiresApi(api = Build.VERSION_CODES.M)
  @NonNull
  private static String aesEncryptPassword(@NonNull String iv, @NonNull String keyStoreName, @NonNull String keyAlias, @NonNull String plainTextPassword)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
    Key secretKey = EncryptDecrypt.getSecretKey(keyStoreName, keyAlias);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
    byte[] encodedBytes = cipher.doFinal(plainTextPassword.getBytes());

    return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
  }

  /** Helper method to decrypt cipher text password */
  @RequiresApi(api = Build.VERSION_CODES.M)
  @NonNull
  private static String aesDecryptPassword(@NonNull String iv, @NonNull String keyStoreName, @NonNull String keyAlias, @NonNull String cipherPassword)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
    Key secretKey = EncryptDecrypt.getSecretKey(keyStoreName, keyAlias);
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

    return new String(decryptedBytes);
  }

  /**
   * Gets a secret key from Android key store. If no key has been generated with a given alias then
   * generate a new one
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  @NonNull
  public static Key getSecretKey(@NonNull String keyStoreName, @NonNull String keyAlias) throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance(keyStoreName);
    keyStore.load(null);

    if (!keyStore.containsAlias(keyAlias)) {
      KeyGenerator keyGenerator =
              KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreName);

      KeyGenParameterSpec.Builder builder =
              new KeyGenParameterSpec.Builder(
                      keyAlias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
      builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM);
      builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
      builder.setRandomizedEncryptionRequired(false);

      keyGenerator.init(builder.build());
      return keyGenerator.generateKey();
    } else {
      return keyStore.getKey(keyAlias, null);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  @NonNull
  private static String rsaEncryptPassword(@NonNull Context context, @NonNull String iv, @NonNull String keyStoreName, @NonNull String keyAlias, @NonNull String password)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RsaKeygen keygen = new RsaKeygen(context, keyStoreName, keyAlias);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);

    return Base64.encodeToString(cipher.doFinal(password.getBytes()), Base64.DEFAULT);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  @NonNull
  private static String rsaDecryptPassword(@NonNull Context context, @NonNull String iv, @NonNull String keyStoreName, @NonNull String keyAlias, @NonNull String cipherText)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RsaKeygen keygen = new RsaKeygen(context, keyStoreName, keyAlias);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));

    return new String(decryptedBytes);
  }

  /** Method handles encryption of plain text on various APIs */
  @NonNull
  public static String encryptPassword(@NonNull Context context, @NonNull String iv, @NonNull String keyStoreName, @NonNull String keyAlias, @NonNull String plainText)
          throws GeneralSecurityException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return aesEncryptPassword(iv, keyStoreName, keyAlias, plainText);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

      return rsaEncryptPassword(context, iv, keyStoreName, keyAlias, plainText);
    } else return plainText;
  }

  /** Method handles decryption of cipher text on various APIs */
  @NonNull
  public static String decryptPassword(@NonNull Context context, @NonNull String iv, @NonNull String keyStoreName, @NonNull String keyAlias, @NonNull String cipherText)
          throws GeneralSecurityException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return aesDecryptPassword(iv, keyStoreName, keyAlias, cipherText);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return rsaDecryptPassword(context, iv, keyStoreName, keyAlias, cipherText);
    } else return cipherText;
  }

  /**
   * Method initializes a Cipher to be used by {@link
   * android.hardware.fingerprint.FingerprintManager}
   */
  @Nullable
  public static Cipher initCipher(Context context, String iv, String keyStoreName, String keyAlias) throws GeneralSecurityException, IOException {
    Cipher cipher;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      cipher = Cipher.getInstance(ALGO_AES);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
      Key secretKey = EncryptDecrypt.getSecretKey(keyStoreName, keyAlias);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      cipher = Cipher.getInstance(ALGO_AES);
      RsaKeygen keygen = new RsaKeygen(context, keyStoreName, keyAlias);

      cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());
    } else {
      cipher = null;
    }

    return cipher;
  }
}
