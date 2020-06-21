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

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OpenMode;

import android.os.Environment;

public class ListFilesOnSshdTest extends AbstractSftpServerTest {

  @Test
  public void testNormalListDirs() throws InterruptedException {
    for (String s : new String[] {"sysroot", "srv", "var", "tmp", "bin", "lib", "usr"}) {
      new File(Environment.getExternalStorageDirectory(), s).mkdir();
    }
    performVerify();
  }

  @Test
  public void testListDirsAndSymlinks() throws Exception {
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
    performVerify();
  }

  private void performVerify() throws InterruptedException {
    List<String> result = new ArrayList<>();
    HybridFile file =
        new HybridFile(OpenMode.SFTP, "ssh://testuser:testpassword@127.0.0.1:" + serverPort);
    CountDownLatch waiter = new CountDownLatch(7);
    file.forEachChildrenFile(
        RuntimeEnvironment.application,
        false,
        (fileFound) -> {
          assertTrue(fileFound.getPath() + " not seen as directory", fileFound.isDirectory());
          result.add(fileFound.getName());
          waiter.countDown();
        });
    waiter.await();
    assertEquals(7, result.size());
    assertThat(result, hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr"));
  }

  @Test
  public void testListDirsAndFilesAndSymlinks() throws Exception {
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
    CountDownLatch waiter = new CountDownLatch(15);
    file.forEachChildrenFile(
        RuntimeEnvironment.application,
        false,
        (fileFound) -> {
          if (!fileFound.getName().endsWith(".txt")) {
            assertTrue(fileFound.getPath() + " not seen as directory", fileFound.isDirectory());
            dirs.add(fileFound.getName());
          } else {
            assertFalse(fileFound.getPath() + " not seen as file", fileFound.isDirectory());
            files.add(fileFound.getName());
          }
          waiter.countDown();
        });
    waiter.await();
    assertEquals(7, dirs.size());
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
}
