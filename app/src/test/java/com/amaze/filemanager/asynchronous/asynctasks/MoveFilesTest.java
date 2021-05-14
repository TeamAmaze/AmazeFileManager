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

package com.amaze.filemanager.asynchronous.asynctasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.ssh.AbstractSftpServerTest;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.os.Environment;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowMultiDex.class})
public class MoveFilesTest extends AbstractSftpServerTest {

  private File sshServerRoot;

  @Before
  @Override
  public void setUp() throws IOException {
    sshServerRoot = Environment.getExternalStoragePublicDirectory("sshserver-test");
    serverPort =
        createSshServer(
            new VirtualFileSystemFactory(Paths.get(sshServerRoot.getAbsolutePath())), 64000);
    prepareSshConnection();
  }

  @Test
  @Ignore("Not yet for CI testing until pass")
  public void testMoveFilesUsingSftp() throws Exception {
    File sourceFile = new File(sshServerRoot, "testfile.bin");
    SecureRandom rnd = new SecureRandom();
    byte[] randomBytes = new byte[32];
    rnd.nextBytes(randomBytes);
    IOUtils.copy(new ByteArrayInputStream(randomBytes), new FileOutputStream(sourceFile));

    ArrayList<HybridFileParcelable> filesToCopy = new ArrayList<>();
    HybridFileParcelable file =
        new HybridFileParcelable(
            "ssh://testuser:testpassword@127.0.0.1:" + serverPort + "/testfile.bin");
    file.generateMode(ApplicationProvider.getApplicationContext());
    filesToCopy.add(file);
    ArrayList<ArrayList<HybridFileParcelable>> filesToCopyPerFolder = new ArrayList<>();
    filesToCopyPerFolder.add(filesToCopy);
    MoveFiles task =
        new MoveFiles(
            filesToCopyPerFolder,
            false,
            null,
            ApplicationProvider.getApplicationContext(),
            OpenMode.FILE);

    ArrayList<String> paths = new ArrayList<>();
    paths.add(Environment.getExternalStorageDirectory().getAbsolutePath());
    task.doInBackground(paths);

    assertFalse(sourceFile.exists());
    assertTrue(new File(Environment.getExternalStorageDirectory(), "testfile.bin").exists());
  }
}
