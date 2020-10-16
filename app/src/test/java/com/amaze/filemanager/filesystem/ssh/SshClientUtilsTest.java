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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;

import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

public class SshClientUtilsTest {

  @Test
  public void testDeriveSftpPathFrom() {
    assertEquals(
        "ssh://root:toor@127.0.0.1:22/",
        SshClientUtils.deriveSftpPathFrom("127.0.0.1", 22, null, "root", "toor", null));
    assertEquals(
        "ssh://root:toor@127.0.0.1:22",
        SshClientUtils.deriveSftpPathFrom("127.0.0.1", 22, "", "root", "toor", null));
  }

  @Test
  public void testIsDirectoryNormal() throws IOException {
    RemoteResourceInfo mock = mock(RemoteResourceInfo.class);
    when(mock.isDirectory()).thenReturn(true);
    FileAttributes mockAttributes =
        new FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build();
    when(mock.getAttributes()).thenReturn(mockAttributes);
    SFTPClient mockClient = mock(SFTPClient.class);
    assertTrue(SshClientUtils.isDirectory(mockClient, mock));
  }

  @Test
  public void testIsDirectoryWithFile() throws IOException {
    RemoteResourceInfo mock = mock(RemoteResourceInfo.class);
    when(mock.isDirectory()).thenReturn(false);
    FileAttributes mockAttributes =
        new FileAttributes.Builder().withType(FileMode.Type.REGULAR).build();
    when(mock.getAttributes()).thenReturn(mockAttributes);
    SFTPClient mockClient = mock(SFTPClient.class);
    assertFalse(SshClientUtils.isDirectory(mockClient, mock));
  }

  @Test
  public void testIsDirectorySymlinkNormal() throws IOException {
    RemoteResourceInfo mock = mock(RemoteResourceInfo.class);
    when(mock.getPath()).thenReturn("/sysroot/etc");
    when(mock.isDirectory()).thenReturn(true);
    FileAttributes mockAttributes =
        new FileAttributes.Builder().withType(FileMode.Type.SYMLINK).build();
    when(mock.getAttributes()).thenReturn(mockAttributes);

    SFTPClient mockClient = mock(SFTPClient.class);
    mockAttributes = new FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build();
    when(mockClient.stat("/sysroot/etc")).thenReturn(mockAttributes);

    assertTrue(SshClientUtils.isDirectory(mockClient, mock));
  }

  @Test
  public void testIsDirectorySymlinkBrokenDirectory() throws IOException {
    RemoteResourceInfo mock = mock(RemoteResourceInfo.class);
    when(mock.getPath()).thenReturn("/sysroot/etc");
    when(mock.isDirectory()).thenReturn(true);
    FileAttributes mockAttributes =
        new FileAttributes.Builder().withType(FileMode.Type.SYMLINK).build();
    when(mock.getAttributes()).thenReturn(mockAttributes);

    SFTPClient mockClient = mock(SFTPClient.class);
    mockAttributes = new FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build();
    when(mockClient.stat("/sysroot/etc")).thenThrow(new IOException());

    assertThrows(IOException.class, () -> SshClientUtils.isDirectory(mockClient, mock));
  }

  @Test
  public void testIsDirectorySymlinkBrokenFile() throws IOException {
    RemoteResourceInfo mock = mock(RemoteResourceInfo.class);
    when(mock.getPath()).thenReturn("/sysroot/etc");
    when(mock.isDirectory()).thenReturn(false);
    FileAttributes mockAttributes =
        new FileAttributes.Builder().withType(FileMode.Type.SYMLINK).build();
    when(mock.getAttributes()).thenReturn(mockAttributes);

    SFTPClient mockClient = mock(SFTPClient.class);
    mockAttributes = new FileAttributes.Builder().withType(FileMode.Type.DIRECTORY).build();
    when(mockClient.stat("/sysroot/etc")).thenThrow(new IOException());

    assertThrows(IOException.class, () -> SshClientUtils.isDirectory(mockClient, mock));
  }
}
