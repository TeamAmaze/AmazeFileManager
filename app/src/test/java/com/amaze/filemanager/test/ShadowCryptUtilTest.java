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

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.filesystem.files.CryptUtil.KEY_ALIAS_AMAZE;
import static com.amaze.filemanager.filesystem.files.CryptUtil.KEY_STORE_ANDROID;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSQLiteConnection;

import com.amaze.filemanager.database.UtilitiesDatabase;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.filesystem.files.AmazeSpecificEncryptDecrypt;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.file_operations.filesystem.encryption.EncryptDecrypt;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class, ShadowCryptUtil.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class ShadowCryptUtilTest {

  private static Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    RxJavaPlugins.reset();
    RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    RxAndroidPlugins.reset();
    RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
  }

  @After
  public void tearDown() {
    ShadowSQLiteConnection.reset();
  }

  @Test
  public void testEncryptDecrypt() throws GeneralSecurityException, IOException {
    String text = "test";
    String encrypted = AmazeSpecificEncryptDecrypt.encryptPassword(ApplicationProvider.getApplicationContext(), text);
    assertEquals(
        text, AmazeSpecificEncryptDecrypt.decryptPassword(ApplicationProvider.getApplicationContext(), encrypted));
  }

  @Test
  public void testWithUtilsHandler() {

    UtilitiesDatabase utilitiesDatabase =
        UtilitiesDatabase.initialize(ApplicationProvider.getApplicationContext());
    UtilsHandler utilsHandler =
        new UtilsHandler(ApplicationProvider.getApplicationContext(), utilitiesDatabase);

    String fingerprint = "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff";
    String url = "ssh://test:test@127.0.0.1:22";

    utilsHandler.saveToDatabase(
        new OperationData(
            UtilsHandler.Operation.SFTP,
            SshClientUtils.encryptSshPathAsNecessary(context, url),
            "Test",
            fingerprint,
            null,
            null));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .until(
            () -> {
              assertEquals(
                  fingerprint,
                  utilsHandler.getSshHostKey(SshClientUtils.encryptSshPathAsNecessary(context, url)));
              utilitiesDatabase.close();
              return true;
            });
  }
}
