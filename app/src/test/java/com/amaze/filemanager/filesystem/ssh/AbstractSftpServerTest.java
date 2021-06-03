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

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public abstract class AbstractSftpServerTest {

  protected SshServer server;

  protected static TestKeyProvider hostKeyProvider;

  protected int serverPort;

  @BeforeClass
  public static void bootstrap() throws Exception {
    hostKeyProvider = new TestKeyProvider();
  }

  @Before
  public void setUp() throws IOException {
    serverPort =
        createSshServer(
            new VirtualFileSystemFactory(
                Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath())),
            64000);
    prepareSshConnection();
  }

  @After
  public void tearDown() throws IOException {
    SshConnectionPool.INSTANCE.shutdown();
    if (server != null && server.isOpen()) {
      server.stop(true);
    }
  }

  protected final void prepareSshConnection() {
    String hostFingerprint = KeyUtils.getFingerPrint(hostKeyProvider.getKeyPair().getPublic());
    SshConnectionPool.INSTANCE.getConnection(
        "127.0.0.1", serverPort, hostFingerprint, "testuser", "testpassword", null);
  }

  protected final int createSshServer(FileSystemFactory fileSystemFactory, int startPort)
      throws IOException {

    server = SshServer.setUpDefaultServer();

    server.setFileSystemFactory(fileSystemFactory);
    server.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
    server.setHost("127.0.0.1");
    server.setKeyPairProvider(hostKeyProvider);
    server.setCommandFactory(new ScpCommandFactory());
    server.setSubsystemFactories(Arrays.asList(new SftpSubsystemFactory()));
    server.setPasswordAuthenticator(
        ((username, password, session) ->
            username.equals("testuser") && password.equals("testpassword")));

    try {
      server.setPort(startPort);
      server.start();
      return startPort;
    } catch (BindException ifPortIsUnavailable) {
      return createSshServer(fileSystemFactory, startPort + 1);
    }
  }
}
