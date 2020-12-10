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

package com.amaze.filemanager.filesystem.ssh.test;

import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_URI_PREFIX;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.schmizz.sshj.common.SecurityUtils;

public abstract class TestUtils {

  public static KeyPair createKeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(1024, new SecureRandom());
      return keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static void saveSshConnectionSettings(
      @NonNull KeyPair hostKeyPair,
      @NonNull String validUsername,
      @Nullable String validPassword,
      @Nullable PrivateKey privateKey) {

    UtilsHandler utilsHandler = AppConfig.getInstance().getUtilsHandler();

    String privateKeyContents = null;
    if (privateKey != null) {
      StringWriter writer = new StringWriter();
      JcaPEMWriter jw = new JcaPEMWriter(writer);
      try {
        jw.writeObject(privateKey);
        jw.flush();
        jw.close();
      } catch (IOException shallNeverHappen) {
      }
      privateKeyContents = writer.toString();
    }

    StringBuilder fullUri = new StringBuilder().append(SSH_URI_PREFIX).append(validUsername);

    if (validPassword != null) fullUri.append(':').append(validPassword);

    fullUri.append("@127.0.0.1:22222");

    if (validPassword != null)
      utilsHandler.saveToDatabase(
          new OperationData(
              UtilsHandler.Operation.SFTP,
              SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()),
              "Test",
              SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
              null,
              null));
    else
      utilsHandler.saveToDatabase(
          new OperationData(
              UtilsHandler.Operation.SFTP,
              SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()),
              "Test",
              SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
              "id_rsa",
              privateKeyContents));

    shadowOf(Looper.getMainLooper()).idle();
  }
}
