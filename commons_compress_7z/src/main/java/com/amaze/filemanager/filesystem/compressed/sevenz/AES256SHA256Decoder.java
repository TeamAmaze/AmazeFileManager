/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.compressed.sevenz;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.compress.PasswordRequiredException;

class AES256SHA256Decoder extends CoderBase {
  @Override
  InputStream decode(
      final String archiveName,
      final InputStream in,
      final long uncompressedLength,
      final Coder coder,
      final byte[] passwordBytes,
      final int maxMemoryLimitInKb) {
    return new InputStream() {
      private boolean isInitialized;
      private CipherInputStream cipherInputStream;

      private CipherInputStream init() throws IOException {
        if (isInitialized) {
          return cipherInputStream;
        }
        if (coder.properties == null) {
          throw new IOException("Missing AES256 properties in " + archiveName);
        }
        if (coder.properties.length < 2) {
          throw new IOException("AES256 properties too short in " + archiveName);
        }
        final int byte0 = 0xff & coder.properties[0];
        final int numCyclesPower = byte0 & 0x3f;
        final int byte1 = 0xff & coder.properties[1];
        final int ivSize = ((byte0 >> 6) & 1) + (byte1 & 0x0f);
        final int saltSize = ((byte0 >> 7) & 1) + (byte1 >> 4);
        if (2 + saltSize + ivSize > coder.properties.length) {
          throw new IOException("Salt size + IV size too long in " + archiveName);
        }
        final byte[] salt = new byte[saltSize];
        System.arraycopy(coder.properties, 2, salt, 0, saltSize);
        final byte[] iv = new byte[16];
        System.arraycopy(coder.properties, 2 + saltSize, iv, 0, ivSize);

        if (passwordBytes == null) {
          throw new PasswordRequiredException(archiveName);
        }
        final byte[] aesKeyBytes;
        if (numCyclesPower == 0x3f) {
          aesKeyBytes = new byte[32];
          System.arraycopy(salt, 0, aesKeyBytes, 0, saltSize);
          System.arraycopy(
              passwordBytes,
              0,
              aesKeyBytes,
              saltSize,
              Math.min(passwordBytes.length, aesKeyBytes.length - saltSize));
        } else {
          final MessageDigest digest;
          try {
            digest = MessageDigest.getInstance("SHA-256");
          } catch (final NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IOException(
                "SHA-256 is unsupported by your Java implementation", noSuchAlgorithmException);
          }
          final byte[] extra = new byte[8];
          for (long j = 0; j < (1L << numCyclesPower); j++) {
            digest.update(salt);
            digest.update(passwordBytes);
            digest.update(extra);
            for (int k = 0; k < extra.length; k++) {
              ++extra[k];
              if (extra[k] != 0) {
                break;
              }
            }
          }
          aesKeyBytes = digest.digest();
        }

        final SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        try {
          final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
          cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
          cipherInputStream = new CipherInputStream(in, cipher);
          isInitialized = true;
          return cipherInputStream;
        } catch (final GeneralSecurityException generalSecurityException) {
          throw new IOException(
              "Decryption error "
                  + "(do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)",
              generalSecurityException);
        }
      }

      @Override
      public int read() throws IOException {
        return init().read();
      }

      @Override
      public int read(final byte[] b, final int off, final int len) throws IOException {
        return init().read(b, off, len);
      }

      @Override
      public void close() throws IOException {
        if (cipherInputStream != null) {
          cipherInputStream.close();
        }
      }
    };
  }
}
