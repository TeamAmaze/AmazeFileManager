package com.amaze.filemanager.filesystem.encryption;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import static com.amaze.filemanager.filesystem.encryption.CryptUtil.IV;

/**
 * @author Emmanuel
 *         on 6/6/2017, at 11:31.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class RSAEncryption implements EncryptFunctions {

    @Override
    public String encryptPassword(Context context, String plainTextPassword) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(CryptUtil.IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);

        return Base64.encodeToString(cipher.doFinal(plainTextPassword.getBytes()), Base64.DEFAULT);
    }

    @Override
    public String decryptPassword(Context context, String cipherPassword) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(CryptUtil.IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

        return decryptedBytes.toString();
    }

    @Override
    public void encrypt(Context context, BufferedInputStream inputStream,
                        BufferedOutputStream outputStream) throws IOException, GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);

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
        Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, keygen.getSecretKey(), ivParameterSpec);
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

    @Override
    public Key getSecretKey() throws IOException, GeneralSecurityException {
        return null;
    }

    @Override
    public Cipher initCipher(Context context) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_AES, "BC");
        RSAKeygen keygen = new RSAKeygen(context);

        cipher.init(Cipher.ENCRYPT_MODE, keygen.getSecretKey());
        return cipher;
    }

    /**
     * Class responsible for generating key for API lower than M
     */
    private static class RSAKeygen {

        private Context context;


        RSAKeygen(Context context) {

            this.context = context;

            try {
                generateKeyPair(context);
                setKeyPreference();
            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        /**
         * Generates a RSA public/private key pair to encrypt AES key
         * @param context
         * @throws IOException
         * @throws GeneralSecurityException
         */
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private void generateKeyPair(Context context) throws IOException, GeneralSecurityException {

            KeyStore keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID);
            keyStore.load(null);

            if (!keyStore.containsAlias(CryptUtil.KEY_ALIAS_AMAZE)) {
                // generate a RSA key pair to encrypt/decrypt AES key from preferences
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", CryptUtil.KEY_STORE_ANDROID);

                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(CryptUtil.KEY_ALIAS_AMAZE)
                        .setSubject(new X500Principal("CN=" + CryptUtil.KEY_ALIAS_AMAZE))
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
        private void setKeyPreference() throws IOException, GeneralSecurityException {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String encodedAesKey = preferences.getString(CryptUtil.PREFERENCE_KEY, null);

            if (encodedAesKey==null) {
                // generate encrypted aes key and save to preference

                byte[] key = new byte[16];
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextBytes(key);

                byte[] encryptedKey = encryptAESKey(key);
                encodedAesKey = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
                preferences.edit().putString(CryptUtil.PREFERENCE_KEY, encodedAesKey).apply();
            }
        }

        /**
         * Encrypts randomly generated AES key using RSA public key
         * @param secretKey
         * @return
         */
        private byte[] encryptAESKey(byte[] secretKey) throws IOException, GeneralSecurityException {

            KeyStore keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(CryptUtil.KEY_ALIAS_AMAZE, null);
            Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_RSA, "AndroidOpenSSL");
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
         * @throws IOException
         * @throws GeneralSecurityException
         */
        private Key getSecretKey() throws IOException, GeneralSecurityException {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String encodedString = preferences.getString(CryptUtil.PREFERENCE_KEY, null);
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
         * @throws IOException
         * @throws GeneralSecurityException
         */
        private byte[] decryptAESKey(byte[] encodedBytes) throws IOException, GeneralSecurityException {

            KeyStore keyStore = KeyStore.getInstance(CryptUtil.KEY_STORE_ANDROID);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(CryptUtil.KEY_ALIAS_AMAZE, null);
            Cipher cipher = Cipher.getInstance(CryptUtil.ALGO_RSA, "AndroidOpenSSL");
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
