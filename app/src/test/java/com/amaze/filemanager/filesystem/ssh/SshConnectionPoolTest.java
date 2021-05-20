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

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.filesystem.ssh.test.TestUtils.saveSshConnectionSettings;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSQLiteConnection;

import com.amaze.filemanager.filesystem.ssh.test.TestUtils;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.test.ShadowCryptUtil;
import com.amaze.filemanager.utils.Utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class, ShadowCryptUtil.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class SshConnectionPoolTest {

  private static KeyPair hostKeyPair;

  private static KeyPair userKeyPair;

  private static KeyProvider sshKeyProvider;

  @BeforeClass
  public static void bootstrap() throws Exception {
    hostKeyPair = TestUtils.createKeyPair();
    userKeyPair = TestUtils.createKeyPair();
    sshKeyProvider =
        new KeyProvider() {
          @Override
          public PrivateKey getPrivate() throws IOException {
            return userKeyPair.getPrivate();
          }

          @Override
          public PublicKey getPublic() throws IOException {
            return userKeyPair.getPublic();
          }

          @Override
          public KeyType getType() throws IOException {
            return KeyType.RSA;
          }

          @Override
          public boolean equals(@Nullable Object obj) {
            if (obj == null || !(obj instanceof KeyProvider)) return false;
            else {
              KeyProvider other = (KeyProvider) obj;
              try {
                return other.getPrivate().equals(getPrivate())
                    && other.getPublic().equals(getPublic());
              } catch (IOException shallNeverHappenHere) {
                return false;
              }
            }
          }
        };
    RxJavaPlugins.reset();
    RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    RxAndroidPlugins.reset();
    RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
  }

  @After
  public void tearDown() {
    SshConnectionPool.INSTANCE.shutdown();
    ShadowSQLiteConnection.reset();
  }

  @Test
  public void testGetConnectionWithUsernameAndPassword() throws IOException {
    SSHClient mock = createSshServer("testuser", "testpassword");
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "127.0.0.1",
            22222,
            SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
            "testuser",
            "testpassword",
            null));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "127.0.0.1",
            22222,
            SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
            "invaliduser",
            "invalidpassword",
            null));

    verify(mock, times(2))
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, times(2)).connect("127.0.0.1", 22222);
    verify(mock).authPassword("testuser", "testpassword");
    verify(mock).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUsernameAndKey() throws IOException {
    SSHClient mock = createSshServer("testuser", null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "127.0.0.1",
            22222,
            SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
            "testuser",
            null,
            userKeyPair));
    SshConnectionPool.INSTANCE.shutdown();
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "127.0.0.1",
            22222,
            SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
            "invaliduser",
            null,
            userKeyPair));

    verify(mock, times(2))
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, times(2)).connect("127.0.0.1", 22222);
    verify(mock).authPublickey("testuser", sshKeyProvider);
    verify(mock).authPublickey("invaliduser", sshKeyProvider);
  }

  @Test
  public void testGetConnectionWithUrl() throws IOException {
    String validPassword = "testpassword";
    SSHClient mock = createSshServer("testuser", validPassword);
    saveSshConnectionSettings(hostKeyPair, "testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection("ssh://testuser:testpassword@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);
    verify(mock).authPassword("testuser", "testpassword");
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlAndKeyAuth() throws IOException {
    SSHClient mock = createSshServer("testuser", null);
    saveSshConnectionSettings(hostKeyPair, "testuser", null, userKeyPair.getPrivate());
    assertNotNull(SshConnectionPool.INSTANCE.getConnection("ssh://testuser@127.0.0.1:22222"));
    assertNull(SshConnectionPool.INSTANCE.getConnection("ssh://invaliduser@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPublickey("testuser", sshKeyProvider);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPublickey("invaliduser", sshKeyProvider);
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexPassword1() throws IOException {
    String validPassword = "testP@ssw0rd";
    SSHClient mock = createSshServer("testuser", validPassword);
    saveSshConnectionSettings(hostKeyPair, "testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection("ssh://testuser:testP@ssw0rd@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword("testuser", validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexPassword2() throws IOException {
    String validPassword = "testP@##word";
    SSHClient mock = createSshServer("testuser", validPassword);
    saveSshConnectionSettings(hostKeyPair, "testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword("testuser", validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential1() throws IOException {
    String validPassword = "testP@##word";
    SSHClient mock = createSshServer("testuser", validPassword);
    saveSshConnectionSettings(hostKeyPair, "testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword("testuser", validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential2() throws IOException {
    String validPassword = "testP@##word";
    SSHClient mock = createSshServer("testuser", validPassword);
    saveSshConnectionSettings(hostKeyPair, "testuser", validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword("testuser", validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential3() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "testP@ssw0rd";
    SSHClient mock = createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(hostKeyPair, validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://test@example.com:testP@ssw0rd@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword(validUsername, validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingComplexCredential4() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "testP@ssw0##$";
    SSHClient mock = createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(hostKeyPair, validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://test@example.com:testP@ssw0##$@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword(validUsername, validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword1() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "abcd-efgh";
    SSHClient mock = createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(hostKeyPair, validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://test@example.com:abcd-efgh@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword(validUsername, validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword2() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "---------------";
    SSHClient mock = createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(hostKeyPair, validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://test@example.com:---------------@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword(validUsername, validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword3() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "--agdiuhdpost15";
    SSHClient mock = createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(hostKeyPair, validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://test@example.com:--agdiuhdpost15@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword(validUsername, validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  @Test
  public void testGetConnectionWithUrlHavingMinusSignInPassword4() throws IOException {
    String validUsername = "test@example.com";
    String validPassword = "t-h-i-s-i-s-p-a-s-s-w-o-r-d-";
    SSHClient mock = createSshServer(validUsername, validPassword);
    saveSshConnectionSettings(hostKeyPair, validUsername, validPassword, null);
    assertNotNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://test@example.com:t-h-i-s-i-s-p-a-s-s-w-o-r-d-@127.0.0.1:22222"));
    assertNull(
        SshConnectionPool.INSTANCE.getConnection(
            "ssh://invaliduser:invalidpassword@127.0.0.1:22222"));

    verify(mock, atLeastOnce())
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    verify(mock, atLeastOnce()).setConnectTimeout(SshConnectionPool.SSH_CONNECT_TIMEOUT);
    verify(mock, atLeastOnce()).connect("127.0.0.1", 22222);

    verify(mock).authPassword(validUsername, validPassword);
    // invalid username won't give host key. Should never called this
    verify(mock, never()).authPassword("invaliduser", "invalidpassword");
  }

  private SSHClient createSshServer(@NonNull String validUsername, @Nullable String validPassword)
      throws IOException {

    SSHClient mock = mock(SSHClient.class);
    doNothing().when(mock).connect("127.0.0.1", 22222);
    doNothing()
        .when(mock)
        .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
    doNothing().when(mock).disconnect();
    if (!Utils.isNullOrEmpty(validPassword)) {
      doNothing().when(mock).authPassword(validUsername, validPassword);
      doThrow(new UserAuthException("Invalid login/password"))
          .when(mock)
          .authPassword(not(eq(validUsername)), not(eq(validPassword)));
    } else {
      doNothing().when(mock).authPublickey(validUsername, sshKeyProvider);
      doThrow(new UserAuthException("Invalid key"))
          .when(mock)
          .authPublickey(not(eq(validUsername)), eq(sshKeyProvider));
    }
    // reset(mock);
    SshConnectionPool.sshClientFactory = config -> mock;
    return mock;
  }
}
