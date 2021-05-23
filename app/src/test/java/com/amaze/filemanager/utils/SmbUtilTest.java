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

package com.amaze.filemanager.utils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.DOESNT_EXIST;
import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.WRITABLE_ON_REMOTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.shadows.ShadowSmbUtil;
import com.amaze.filemanager.test.ShadowCryptUtil;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import jcifs.smb.NtlmPasswordAuthenticator;

@RunWith(AndroidJUnit4.class)
@Config(
    sdk = {JELLY_BEAN, KITKAT, P},
    shadows = {ShadowCryptUtil.class, ShadowSmbUtil.class})
public class SmbUtilTest {

  @Test
  public void testEncryptDecrypt() throws GeneralSecurityException, IOException {
    String path = "smb://root:toor@127.0.0.1";
    String encrypted =
        SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path);
    assertNotEquals(path, encrypted);
    assertTrue(encrypted.startsWith("smb://root:"));
    assertTrue(encrypted.endsWith("@127.0.0.1"));
    String decrypted =
        SmbUtil.getSmbDecryptedPath(ApplicationProvider.getApplicationContext(), encrypted);
    assertEquals(path, decrypted);
  }

  @Test
  public void testEncryptWithoutCredentials() throws GeneralSecurityException, IOException {
    String path = "smb://127.0.0.1";
    assertEquals(
        path, SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path));
  }

  @Test
  @Ignore("Good idea to fix me")
  public void testEncryptWithoutPassword() throws GeneralSecurityException, IOException {
    String path = "smb://toor@127.0.0.1";
    assertEquals(
        path, SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path));
  }

  @Test
  public void testCheckFolder() {
    assertEquals(
        DOESNT_EXIST, SmbUtil.checkFolder("smb://user:password@5.6.7.8/newfolder/DummyFolder"));
    assertEquals(
        DOESNT_EXIST, SmbUtil.checkFolder("smb://user:password@5.6.7.8/newfolder/resume.doc"));
    assertEquals(
        WRITABLE_ON_REMOTE, SmbUtil.checkFolder("smb://user:password@5.6.7.8/newfolder/Documents"));
    assertEquals(
        DOESNT_EXIST, SmbUtil.checkFolder("smb://user:password@5.6.7.8/newfolder/wirebroken.log"));
    assertEquals(
        DOESNT_EXIST, SmbUtil.checkFolder("smb://user:password@5.6.7.8/newfolder/failcheck"));
  }

  public void testCreateNtlmPasswordAuthenticator() {
    NtlmPasswordAuthenticator auth = SmbUtil.createFrom(null);
    assertEquals("", auth.getUserDomain());
    assertEquals("", auth.getUsername());
    assertEquals("", auth.getPassword());

    auth = SmbUtil.createFrom("");
    assertEquals("", auth.getUserDomain());
    assertEquals("", auth.getUsername());
    assertEquals("", auth.getPassword());

    auth = SmbUtil.createFrom("username:password");
    assertEquals("", auth.getUserDomain());
    assertEquals("username", auth.getUsername());
    assertEquals("password", auth.getPassword());

    auth = SmbUtil.createFrom("WORKGROUP;username:password");
    assertEquals("WORKGROUP", auth.getUserDomain());
    assertEquals("username", auth.getUsername());
    assertEquals("password", auth.getPassword());

    auth = SmbUtil.createFrom("WORKGROUP;username");
    assertEquals("WORKGROUP", auth.getUserDomain());
    assertEquals("username", auth.getUsername());
    assertEquals("", auth.getPassword());

    // #2313 major symptom
    auth = SmbUtil.createFrom("username:pass%w0rd");
    assertEquals("", auth.getUserDomain());
    assertEquals("username", auth.getUsername());
    assertEquals("pass%w0rd", auth.getPassword());

    // Shall not happen - we should rarely have % in username/workgroup names, but anyway.
    auth = SmbUtil.createFrom("WORKGROUP;user%1");
    assertEquals("WORKGROUP", auth.getUserDomain());
    assertEquals("user%1", auth.getUsername());
    assertEquals("", auth.getPassword());

    auth = SmbUtil.createFrom("WORKGROUP%2;user%1:pass%word");
    assertEquals("WORKGROUP%2", auth.getUserDomain());
    assertEquals("user%1", auth.getUsername());
    assertEquals("pass%word", auth.getPassword());
  }
}
