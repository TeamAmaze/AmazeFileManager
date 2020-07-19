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

package com.amaze.filemanager.filesystem.ssh;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.database.UtilitiesDatabase;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.test.ShadowCryptUtil;
import com.amaze.filemanager.test.TestUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.schmizz.sshj.common.SecurityUtils;

@RunWith(RobolectricTestRunner.class)
@Config(
    shadows = {ShadowMultiDex.class, ShadowCryptUtil.class},
    maxSdk = 27)
public class SshConnectionPoolTest {

  private SshServer server;

  private UtilitiesDatabase utilitiesDatabase;

  private UtilsHandler utilsHandler;

  private static TestKeyProvider hostKeyProvider, userKeyProvider;

  @BeforeClass
  public static void bootstrap() throws Exception {
    hostKeyProvider = new TestKeyProvider();
    userKeyProvider = new TestKeyProvider();
  }

  @After
  public void tearDown() {
    if (server != null && server.isOpen()) server.close(true);
    if (utilitiesDatabase != null && utilitiesDatabase.isOpen()) utilitiesDatabase.close();
  }

  @Test
  public void testGetConnectionWithUsernameAndPassword() throws IOException {
    createSshServer("testuser", "testpassword");
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection(
                "127.0.0.1",
                22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "testuser",
                "testpassword",
                null));

    assertNull(
        SshConnectionPool.getInstance()
            .getConnection(
                "127.0.0.1",
                22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "invaliduser",
                "invalidpassword",
                null));
  }

  @Test
  public void testGetConnectionWithUsernameAndKey() throws IOException {
    createSshServer("testuser", null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection(
                "127.0.0.1",
                22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "testuser",
                null,
                userKeyProvider.getKeyPair()));

    assertNull(
        SshConnectionPool.getInstance()
            .getConnection(
                "127.0.0.1",
                22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "invaliduser",
                null,
                userKeyProvider.getKeyPair()));
  }

  @Test
  public void testGetConnectionWithUrl() throws IOException {
    String validPassword = "testpassword";
    createSshServer("testuser", validPassword);
    saveSshConnectionSettings("testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://testuser:testpassword@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlAndKeyAuth() throws IOException {
    createSshServer("testuser", null);
    saveSshConnectionSettings("testuser", null, userKeyProvider.getKeyPair().getPrivate());
    assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser@127.0.0.1:22222"));
    assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexPassword1() throws IOException {
    String validPassword = "testP@ssw0rd";
    createSshServer("testuser", validPassword);
    saveSshConnectionSettings("testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://testuser:testP@ssw0rd@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexPassword2() throws IOException {
    String validPassword = "testP@##word";
    createSshServer("testuser", validPassword);
    saveSshConnectionSettings("testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential1() throws IOException {
    String validPassword = "testP@##word";
    createSshServer("testuser", validPassword);
    saveSshConnectionSettings("testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential2() throws IOException {
    String validPassword = "testP@##word";
    createSshServer("testuser", validPassword);
    saveSshConnectionSettings("testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential3() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "testP@ssw0rd";
    createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://test@example.com:testP@ssw0rd@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential4() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "testP@ssw0##$";
    createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://test@example.com:testP@ssw0##$@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword1() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "abcd-efgh";
    createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://test@example.com:abcd-efgh@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword2() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "---------------";
    createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://test@example.com:---------------@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword3() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "--agdiuhdpost15";
    createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://test@example.com:--agdiuhdpost15@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword4() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "t-h-i-s-i-s-p-a-s-s-w-o-r-d-";
    createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://test@example.com:t-h-i-s-i-s-p-a-s-s-w-o-r-d-@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.getInstance()
            .getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
  }

  private void createSshServer(@NonNull String validUsername, @Nullable String validPassword)
      throws IOException {
    server = SshServer.setUpDefaultServer();
    server.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
    server.setPort(22222);
    server.setHost("127.0.0.1");
    server.setKeyPairProvider(hostKeyProvider);
    if (validPassword != null)
      server.setPasswordAuthenticator(
          ((username, password, session) ->
              username.equals(validUsername) && password.equals(validPassword)));
    server.setPublickeyAuthenticator(
        (username, key, session) ->
            username.equals(validUsername) && key.equals(userKeyProvider.getKeyPair().getPublic()));
    server.start();
  }

  private void saveSshConnectionSettings(
      @NonNull String validUsername,
      @Nullable String validPassword,
      @Nullable PrivateKey privateKey) {
    utilitiesDatabase = UtilitiesDatabase.initialize(RuntimeEnvironment.application);
    utilsHandler = new UtilsHandler(RuntimeEnvironment.application, utilitiesDatabase);

    String privateKeyContents = null;
    if (privateKey != null) {
      StringWriter writer = new StringWriter();
      JcaPEMWriter jw = new JcaPEMWriter(writer);
      try {
        jw.writeObject(userKeyProvider.getKeyPair().getPrivate());
        jw.flush();
        jw.close();
      } catch (IOException shallNeverHappen) {
      }
      privateKeyContents = writer.toString();
    }

    StringBuilder fullUri = new StringBuilder().append("ssh://").append(validUsername);

    if (validPassword != null) fullUri.append(':').append(validPassword);

    fullUri.append("@127.0.0.1:22222");

    if (validPassword != null)
      utilsHandler.saveToDatabase(
          new OperationData(
              UtilsHandler.Operation.SFTP,
              SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()),
              "Test",
              SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
              null,
              null));
    else
      utilsHandler.saveToDatabase(
          new OperationData(
              UtilsHandler.Operation.SFTP,
              SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()),
              "Test",
              SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
              "id_rsa",
              privateKeyContents));

    TestUtils.flushAppConfigHandlerThread();
  }
}
