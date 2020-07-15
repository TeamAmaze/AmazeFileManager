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

package com.amaze.filemanager.test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import com.amaze.filemanager.filesystem.files.CryptUtil;

import android.content.Context;
import android.util.Base64;

@Implements(CryptUtil.class)
public class ShadowCryptUtil {

  private static final String ALGO_AES = "AES/GCM/NoPadding";
  private static final String IV = "LxbHiJhhUXcj"; // 12 byte long IV supported by android for GCM

  private static SecretKey secretKey = null;

  static {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(128);
      secretKey = keyGen.generateKey();
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    }
  }

  /** Method handles encryption of plain text on various APIs */
  @Implementation
  public static String encryptPassword(Context context, String plainText)
      throws GeneralSecurityException, IOException {
    return aesEncryptPassword(plainText);
  }

  /** Method handles decryption of cipher text on various APIs */
  @Implementation
  public static String decryptPassword(Context context, String cipherText)
      throws GeneralSecurityException, IOException {
    return aesDecryptPassword(cipherText);
  }

  /** Helper method to encrypt plain text password */
  private static String aesEncryptPassword(String plainTextPassword)
      throws GeneralSecurityException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
    byte[] encodedBytes = cipher.doFinal(plainTextPassword.getBytes());

    return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
  }

  /** Helper method to decrypt cipher text password */
  private static String aesDecryptPassword(String cipherPassword) throws GeneralSecurityException {

    Cipher cipher = Cipher.getInstance(ALGO_AES);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV.getBytes());
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
    byte[] decryptedBytes = cipher.doFinal(Base64.decode(cipherPassword, Base64.DEFAULT));

    return new String(decryptedBytes);
  }
}
