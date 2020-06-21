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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.session.Session;
import org.junit.Ignore;
import org.junit.Test;

import com.amaze.filemanager.filesystem.ssh.test.BlockFileCreationFileSystemProvider;

import android.os.Environment;

@Ignore("Skipped due to no solid test case given")
public class CreateFileOnSshdTest extends AbstractSftpServerTest {

  @Test
  public void testCreateFileNormal() throws Exception {
    tearDown();
    createSshServer(
        new VirtualFileSystemFactory(
            Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath())),
        serverPort);
  }

  @Test
  public void testCreateFilePermissionDenied() throws Exception {
    tearDown();
    createSshServer(
        new VirtualFileSystemFactory() {
          @Override
          public FileSystem createFileSystem(Session session) throws IOException {
            return new BlockFileCreationFileSystemProvider()
                .newFileSystem(
                    Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath()),
                    Collections.emptyMap());
          }
        },
        serverPort);
  }
}
