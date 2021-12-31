package com.amaze.filemanager.filesystem.files;

import static com.amaze.filemanager.filesystem.files.CryptUtil.KEY_ALIAS_AMAZE;
import static com.amaze.filemanager.filesystem.files.CryptUtil.KEY_STORE_ANDROID;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

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
  private static String aesEncryptPassword(String iv, String plainTextPassword)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
    byte[] encodedBytes = cipher.doFinal(plainTextPassword.getBytes());

    return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
  }

  /** Helper method to decrypt cipher text password */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private static String aesDecryptPassword(String iv, String cipherPassword)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

    return new String(decryptedBytes);
  }

  /**
   * Gets a secret key from Android key store. If no key has been generated with a given alias then
   * generate a new one
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  public static Key getSecretKey() throws GeneralSecurityException, IOException {

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
  private static String rsaEncryptPassword(Context context, String iv, String password)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RsaKeygen keygen = new RsaKeygen(context);

    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);

    return Base64.encodeToString(cipher.doFinal(password.getBytes()), Base64.DEFAULT);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private static String rsaDecryptPassword(Context context, String iv, String cipherText)
          throws GeneralSecurityException, IOException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    RsaKeygen keygen = new RsaKeygen(context);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));

    return new String(decryptedBytes);
  }

  /** Method handles encryption of plain text on various APIs */
  public static String encryptPassword(Context context, String iv, String plainText)
          throws GeneralSecurityException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return aesEncryptPassword(iv, plainText);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

      return rsaEncryptPassword(context, iv, plainText);
    } else return plainText;
  }

  /** Method handles decryption of cipher text on various APIs */
  public static String decryptPassword(Context context, String iv, String cipherText)
          throws GeneralSecurityException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return aesDecryptPassword(iv, cipherText);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return rsaDecryptPassword(context, iv, cipherText);
    } else return cipherText;
  }

  /**
   * Method initializes a Cipher to be used by {@link
   * android.hardware.fingerprint.FingerprintManager}
   */
  public static Cipher initCipher(Context context, String iv) throws GeneralSecurityException, IOException {
    Cipher cipher = null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      cipher = Cipher.getInstance(ALGO_AES);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
      cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), gcmParameterSpec);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      cipher = Cipher.getInstance(ALGO_AES);
      RsaKeygen keygen = new RsaKeygen(context);

      cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());
    }
    return cipher;
  }
}
