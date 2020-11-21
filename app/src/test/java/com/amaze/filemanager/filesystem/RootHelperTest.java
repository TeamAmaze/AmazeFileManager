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

package com.amaze.filemanager.filesystem;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.test.ShadowShellInteractive;
import com.amaze.filemanager.ui.activities.MainActivity;

import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import eu.chainfire.libsuperuser.Shell;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class, ShadowShellInteractive.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class RootHelperTest {

  private static final File sysroot =
      new File(Environment.getExternalStorageDirectory(), "sysroot");

  private static final List<String> expected =
      Arrays.asList(
          "srv",
          "var",
          "tmp",
          "bin",
          "lib",
          "usr",
          "1.txt",
          "2.txt",
          "3.txt",
          "4.txt",
          "symlink1.txt",
          "symlink2.txt",
          "symlink3.txt",
          "symlink4.txt");

  @Before
  public void setUp() throws IOException {
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
  }

  @Test
  @Ignore
  public void testNonRoot() throws InterruptedException {
    runVerify(false);
  }

  @Test
  @Ignore
  public void testRoot() throws InterruptedException, SecurityException, IllegalArgumentException {
    MainActivity.shellInteractive = new Shell.Builder().setShell("/bin/false").open();
    runVerify(true);
  }

  private void runVerify(boolean root) throws InterruptedException {
    List<String> result = new ArrayList<>();
    CountDownLatch waiter = new CountDownLatch(expected.size());
    ListFilesCommand.INSTANCE.listFiles(
        Environment.getExternalStorageDirectory().getAbsolutePath(),
        root,
        true,
        mode -> null,
        file -> {
          if (result.contains(file.getName())) fail(file.getName() + " already listed");
          result.add(file.getName());
          waiter.countDown();
          return null;
        });
    waiter.await();
  }
}
