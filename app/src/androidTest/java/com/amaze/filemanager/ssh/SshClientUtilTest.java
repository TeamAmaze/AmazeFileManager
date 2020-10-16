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

package com.amaze.filemanager.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class SshClientUtilTest {

  @Test
  public void testEncryptDecryptUriWithNoPassword() {
    String uri = "ssh://testuser@127.0.0.1:22";
    assertEquals(uri, SshClientUtils.encryptSshPathAsNecessary(uri));
    assertEquals(uri, SshClientUtils.decryptSshPathAsNecessary(uri));
  }

  @Test
  public void testEncryptDecryptPasswordWithMinusSign1() {
    String uri = "ssh://testuser:abcd-efgh@127.0.0.1:22";
    String result = SshClientUtils.encryptSshPathAsNecessary(uri);
    assertTrue(result.contains("ssh://testuser:"));
    assertTrue(result.contains("@127.0.0.1:22"));
    String verify = SshClientUtils.decryptSshPathAsNecessary(result);
    assertEquals(uri, verify);
  }

  @Test
  public void testEncryptDecryptPasswordWithMinusSign2() {
    String uri = "ssh://testuser:---------------@127.0.0.1:22";
    String result = SshClientUtils.encryptSshPathAsNecessary(uri);
    assertTrue(result.contains("ssh://testuser:"));
    assertTrue(result.contains("@127.0.0.1:22"));
    String verify = SshClientUtils.decryptSshPathAsNecessary(result);
    assertEquals(uri, verify);
  }

  @Test
  public void testEncryptDecryptPasswordWithMinusSign3() {
    String uri = "ssh://testuser:--agdiuhdpost15@127.0.0.1:22";
    String result = SshClientUtils.encryptSshPathAsNecessary(uri);
    assertTrue(result.contains("ssh://testuser:"));
    assertTrue(result.contains("@127.0.0.1:22"));
    String verify = SshClientUtils.decryptSshPathAsNecessary(result);
    assertEquals(uri, verify);
  }

  @Test
  public void testEncryptDecryptPasswordWithMinusSign4() {
    String uri = "ssh://testuser:t-h-i-s-i-s-p-a-s-s-w-o-r-d-@127.0.0.1:22";
    String result = SshClientUtils.encryptSshPathAsNecessary(uri);
    assertTrue(result.contains("ssh://testuser:"));
    assertTrue(result.contains("@127.0.0.1:22"));
    String verify = SshClientUtils.decryptSshPathAsNecessary(result);
    assertEquals(uri, verify);
  }
}
