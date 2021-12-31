package com.amaze.filemanager.file_operations.filesystem.encryption;

import static com.amaze.filemanager.file_operations.filesystem.encryption.EncryptDecrypt.ALGO_RSA;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

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
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/** Class responsible for generating key for API lower than M */
public class RsaKeygen {

  public static final String PREFERENCE_KEY = "aes_key";
  @NonNull
  private final Context context;
  @NonNull
  private final String keyStoreName;
  @NonNull
  private final String keyAlias;

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public RsaKeygen(@NonNull Context context, @NonNull String keyStoreName, @NonNull String keyAlias) {

    this.context = context;
    this.keyStoreName = keyStoreName;
    this.keyAlias = keyAlias;

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

    KeyStore keyStore = KeyStore.getInstance(keyStoreName);
    keyStore.load(null);

    if (!keyStore.containsAlias(keyAlias)) {
      // generate a RSA key pair to encrypt/decrypt AES key from preferences
      Calendar start = Calendar.getInstance();
      Calendar end = Calendar.getInstance();
      end.add(Calendar.YEAR, 30);

      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", keyStoreName);

      KeyPairGeneratorSpec spec =
              new KeyPairGeneratorSpec.Builder(context)
                      .setAlias(keyAlias)
                      .setSubject(new X500Principal("CN=" + keyAlias))
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
    KeyStore keyStore = KeyStore.getInstance(keyStoreName);
    keyStore.load(null);
    KeyStore.PrivateKeyEntry keyEntry =
            (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, null);
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
  public Key getSecretKey() throws GeneralSecurityException, IOException {

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

    KeyStore keyStore = KeyStore.getInstance(keyStoreName);
    keyStore.load(null);
    KeyStore.PrivateKeyEntry keyEntry =
            (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, null);
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