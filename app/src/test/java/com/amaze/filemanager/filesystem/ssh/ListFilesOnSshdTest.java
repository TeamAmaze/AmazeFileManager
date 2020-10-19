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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;

import android.os.Environment;

import androidx.test.core.app.ApplicationProvider;

public class ListFilesOnSshdTest extends AbstractSftpServerTest {

  @Test
  public void testNormalListDirs() {
    for (String s : new String[] {"sysroot", "srv", "var", "tmp", "bin", "lib", "usr"}) {
      new File(Environment.getExternalStorageDirectory(), s).mkdir();
    }
    assertTrue(performVerify());
  }

  @Test
  public void testListDirsAndSymlinks() throws Exception {
    createNecessaryDirsForSymlinkRelatedTests();
    assertTrue(performVerify());
  }

  private void createNecessaryDirsForSymlinkRelatedTests() throws IOException {
    File sysroot = new File(Environment.getExternalStorageDirectory(), "sysroot");
    sysroot.mkdir();
    for (String s : new String[] {"srv", "var", "tmp"}) {
      File subdir = new File(sysroot, s);
      subdir.mkdir();
      Files.createSymbolicLink(
          Paths.get(new File(Environment.getExternalStorageDirectory(), s).getAbsolutePath()),
          Paths.get(subdir.getAbsolutePath()));
    }
    for (String s : new String[] {"bin", "lib", "usr"}) {
      new File(Environment.getExternalStorageDirectory(), s).mkdir();
    }
  }

  private boolean performVerify() {
    List<String> result = new ArrayList<>();
    HybridFile file =
        new HybridFile(OpenMode.SFTP, "ssh://testuser:testpassword@127.0.0.1:" + serverPort);
    file.forEachChildrenFile(
        ApplicationProvider.getApplicationContext(),
        false,
        (fileFound) -> {
          assertTrue(fileFound.getPath() + " not seen as directory", fileFound.isDirectory());
          result.add(fileFound.getName());
        });
    await().until(() -> result.size() == 7);
    assertThat(result, hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr"));
    return true;
  }

  @Test
  public void testListDirsAndFilesAndSymlinks() throws Exception {
    createNecessaryDirsForSymlinkRelatedTests();
    for (int i = 1; i <= 4; i++) {
      File f = new File(Environment.getExternalStorageDirectory(), i + ".txt");
      FileOutputStream out = new FileOutputStream(f);
      out.write(i);
      out.close();
      Files.createSymbolicLink(
          Paths.get(
              new File(Environment.getExternalStorageDirectory(), "symlink" + i + ".txt")
                  .getAbsolutePath()),
          Paths.get(f.getAbsolutePath()));
    }
    List<String> dirs = new ArrayList<>(), files = new ArrayList<>();
    HybridFile file =
        new HybridFile(OpenMode.SFTP, "ssh://testuser:testpassword@127.0.0.1:" + serverPort);
    file.forEachChildrenFile(
        ApplicationProvider.getApplicationContext(),
        false,
        (fileFound) -> {
          if (!fileFound.getName().endsWith(".txt")) {
            assertTrue(fileFound.getPath() + " not seen as directory", fileFound.isDirectory());
            dirs.add(fileFound.getName());
          } else {
            assertFalse(fileFound.getPath() + " not seen as file", fileFound.isDirectory());
            files.add(fileFound.getName());
          }
        });
    await().until(() -> dirs.size() == 7);
    assertThat(dirs, hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr"));
    assertThat(
        files,
        hasItems(
            "1.txt",
            "2.txt",
            "3.txt",
            "4.txt",
            "symlink1.txt",
            "symlink2.txt",
            "symlink3.txt",
            "symlink4.txt"));
  }

  @Test
  public void testListDirsAndBrokenSymlinks() throws Exception {
    createNecessaryDirsForSymlinkRelatedTests();
    Files.createSymbolicLink(
        Paths.get(
            new File(Environment.getExternalStorageDirectory(), "b0rken.symlink")
                .getAbsolutePath()),
        Paths.get(new File("/tmp/notfound.file").getAbsolutePath()));
    assertTrue(performVerify());
  }

  @Test
  public void testListDirsWithDirectPathToDir() throws Exception {
    createNecessaryDirsForSymlinkRelatedTests();
    for (int i = 1; i <= 4; i++) {
      File f = new File(new File(Environment.getExternalStorageDirectory(), "tmp"), i + ".txt");
      FileOutputStream out = new FileOutputStream(f);
      out.write(i);
      out.close();
    }
    List<String> result = new ArrayList<>();
    HybridFile file =
        new HybridFile(
            OpenMode.SFTP, "ssh://testuser:testpassword@127.0.0.1:" + serverPort + "/tmp");
    file.forEachChildrenFile(
        ApplicationProvider.getApplicationContext(),
        false,
        (fileFound) -> {
          assertFalse(fileFound.getPath() + " not seen as file", fileFound.isDirectory());
          result.add(fileFound.getName());
        });
    await().until(() -> result.size() == 4);
    assertThat(result, hasItems("1.txt", "2.txt", "3.txt", "4.txt"));
    List<String> result2 = new ArrayList<>();
    file =
        new HybridFile(OpenMode.SFTP, file.getParent(ApplicationProvider.getApplicationContext()));
    file.forEachChildrenFile(
        ApplicationProvider.getApplicationContext(),
        false,
        (fileFound) -> {
          assertTrue(fileFound.getPath() + " not seen as directory", fileFound.isDirectory());
          result2.add(fileFound.getName());
        });
    await().until(() -> result2.size() == 7);
    assertThat(result2, hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr"));
  }
}
