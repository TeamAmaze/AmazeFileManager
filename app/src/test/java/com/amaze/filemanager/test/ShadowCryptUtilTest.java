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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.utils.files.CryptUtil;

@RunWith(RobolectricTestRunner.class)
@Config(
    constants = BuildConfig.class,
    shadows = {ShadowMultiDex.class, ShadowCryptUtil.class},
    maxSdk = 27)
public class ShadowCryptUtilTest {

  @Test
  public void testEncryptDecrypt() throws GeneralSecurityException, IOException {
    String text = "test";
    String encrypted = CryptUtil.encryptPassword(RuntimeEnvironment.application, text);
    assertEquals(text, CryptUtil.decryptPassword(RuntimeEnvironment.application, encrypted));
  }

  @Test
  public void testWithUtilsHandler() throws GeneralSecurityException, IOException {
    UtilsHandler utilsHandler = new UtilsHandler(RuntimeEnvironment.application);
    utilsHandler.onCreate(utilsHandler.getWritableDatabase());

    String fingerprint = "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff";
    String url = "ssh://test:test@127.0.0.1:22";

    utilsHandler.addSsh(
        "Test", SshClientUtils.encryptSshPathAsNecessary(url), fingerprint, null, null);
    assertEquals(fingerprint, utilsHandler.getSshHostKey(url));

    utilsHandler.close();
  }
}
