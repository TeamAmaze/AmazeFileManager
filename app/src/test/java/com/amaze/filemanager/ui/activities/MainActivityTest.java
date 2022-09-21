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

package com.amaze.filemanager.ui.activities;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;
import static androidx.test.core.app.ActivityScenario.launch;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowSQLiteConnection;
import org.robolectric.shadows.ShadowStorageManager;
import org.robolectric.util.ReflectionHelpers;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile;
import com.amaze.filemanager.test.ShadowPasswordUtil;
import com.amaze.filemanager.test.TestUtils;
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog;
import com.amaze.filemanager.utils.PasswordUtil;
import com.amaze.filemanager.utils.SmbUtil;

import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.util.Base64;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

@RunWith(AndroidJUnit4.class)
@Config(
    sdk = {JELLY_BEAN, KITKAT, P},
    shadows = {
      ShadowMultiDex.class,
      ShadowStorageManager.class,
      ShadowPasswordUtil.class,
      ShadowSmbFile.class
    })
/*
 * Need to make LooperMode PAUSED and flush the main looper before activity can show up.
 * @see {@link LooperMode.Mode.PAUSED}
 * @see {@link <a href="https://stackoverflow.com/questions/55679636/robolectric-throws-fragmentmanager-is-already-executing-transactions">StackOverflow discussion</a>}
 */
@LooperMode(LooperMode.Mode.PAUSED)
public class MainActivityTest {

  private static final String[] BUNDLE_KEYS = {
    "address", "port", "keypairName", "name", "username", "password", "edit"
  };

  private MockedConstruction<SftpConnectDialog> mc;

  @Before
  public void setUp() {
    if (Build.VERSION.SDK_INT >= N) TestUtils.initializeInternalStorage();
    RxJavaPlugins.reset();
    RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    RxAndroidPlugins.reset();
    RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
    ShadowSQLiteConnection.reset();

    mc =
        mockConstruction(
            SftpConnectDialog.class,
            (mock, context) -> {
              doCallRealMethod().when(mock).setArguments(any());
              when(mock.getArguments()).thenCallRealMethod();
            });
  }

  @After
  public void tearDown() {
    if (Build.VERSION.SDK_INT >= N)
      shadowOf(ApplicationProvider.getApplicationContext().getSystemService(StorageManager.class))
          .resetStorageVolumeList();

    mc.close();
  }

  @Test
  public void testInvokeSftpConnectionDialog() throws GeneralSecurityException, IOException {

    Bundle verify = new Bundle();
    verify.putString("address", "127.0.0.1");
    verify.putInt("port", 22);
    verify.putString("name", "SCP/SFTP Connection");
    verify.putString("username", "root");
    verify.putBoolean("hasPassword", false);
    verify.putBoolean("edit", true);
    verify.putString("keypairName", "abcdefgh");

    testOpenSftpConnectDialog("ssh://root@127.0.0.1:22", verify);
  }

  @Test
  public void testInvokeSftpConnectionDialogWithPassword()
      throws GeneralSecurityException, IOException {
    String uri =
        NetCopyClientUtils.INSTANCE.encryptFtpPathAsNecessary("ssh://root:12345678@127.0.0.1:22");

    Bundle verify = new Bundle();
    verify.putString("address", "127.0.0.1");
    verify.putInt("port", 22);
    verify.putString("name", "SCP/SFTP Connection");
    verify.putString("username", "root");
    verify.putBoolean("hasPassword", true);
    verify.putBoolean("edit", true);
    verify.putString("password", "12345678");

    testOpenSftpConnectDialog(uri, verify);
  }

  private void testOpenSftpConnectDialog(String uri, Bundle verify)
      throws GeneralSecurityException, IOException {
    MainActivity activity = mock(MainActivity.class);
    UtilsHandler utilsHandler = mock(UtilsHandler.class);
    when(utilsHandler.getSshAuthPrivateKeyName("ssh://root@127.0.0.1:22")).thenReturn("abcdefgh");
    ReflectionHelpers.setField(activity, "utilsHandler", utilsHandler);
    doCallRealMethod().when(activity).showSftpDialog(any(), any(), anyBoolean());

    activity.showSftpDialog(
        "SCP/SFTP Connection", NetCopyClientUtils.INSTANCE.encryptFtpPathAsNecessary(uri), true);
    assertEquals(1, mc.constructed().size());
    SftpConnectDialog mocked = mc.constructed().get(0);
    await().atMost(5, TimeUnit.SECONDS).until(() -> mocked.getArguments() != null);
    for (String key : BUNDLE_KEYS) {
      if (mocked.getArguments().get(key) != null) {
        if (!key.equals("password")) {
          assertEquals(verify.get(key), mocked.getArguments().get(key));
        } else {
          assertEquals(
              verify.get(key),
              PasswordUtil.INSTANCE.decryptPassword(
                  ApplicationProvider.getApplicationContext(),
                  (String) mocked.getArguments().get(key),
                  Base64.URL_SAFE));
        }
      }
    }
  }

  @Test
  public void testUpdateSmbExceptionShouldNotThrowNPE() {
    ActivityScenario<MainActivity> scenario = launch(MainActivity.class);

    ShadowLooper.idleMainLooper();

    scenario.moveToState(Lifecycle.State.STARTED);

    scenario.onActivity(
        activity -> {
          String path = "smb://root:toor@192.168.1.1";
          String oldName = "SMB connection";
          String newName = "root@192.168.1.1";
          try {

            activity.addConnection(
                false,
                oldName,
                path,
                SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path),
                null,
                null);
            activity.addConnection(
                true,
                newName,
                path,
                SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path),
                oldName,
                path);

            ShadowLooper.idleMainLooper();

            await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> AppConfig.getInstance().getUtilsHandler().getSmbList().size() > 0);
            await()
                .atMost(5, TimeUnit.SECONDS)
                .until(
                    () ->
                        AppConfig.getInstance()
                            .getUtilsHandler()
                            .getSmbList()
                            .get(0)[0]
                            .equals(newName));
            List<String[]> verify = AppConfig.getInstance().getUtilsHandler().getSmbList();
            assertEquals(1, verify.size());
            String[] entry = verify.get(0);
            assertEquals(path, entry[1]);

          } finally {
            scenario.moveToState(Lifecycle.State.DESTROYED);
            scenario.close();
          }
        });
  }
}
