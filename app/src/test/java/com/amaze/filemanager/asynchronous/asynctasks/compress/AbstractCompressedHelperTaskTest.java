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

package com.amaze.filemanager.asynchronous.asynctasks.compress;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.os.Environment;

@RunWith(RobolectricTestRunner.class)
@Config(
    shadows = {ShadowMultiDex.class},
    minSdk = 27,
    maxSdk = 27)
public abstract class AbstractCompressedHelperTaskTest {

  @Before
  public void setUp() throws IOException {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    copyArchivesToStorage();
  }

  @Test
  public void testRoot() {
    CompressedHelperTask task = createTask("");
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("test-archive", result.result.get(0).name);
  }

  @Test
  public void testSublevels() {
    CompressedHelperTask task = createTask("test-archive");
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertEquals(5, result.result.size());
    assertEquals("1", result.result.get(0).name);
    assertEquals("2", result.result.get(1).name);
    assertEquals("3", result.result.get(2).name);
    assertEquals("4", result.result.get(3).name);
    assertEquals("a", result.result.get(4).name);

    task = createTask("test-archive/1");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("8", result.result.get(0).name);

    task = createTask("test-archive/2");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("7", result.result.get(0).name);

    task = createTask("test-archive/3");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("6", result.result.get(0).name);

    task = createTask("test-archive/4");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("5", result.result.get(0).name);

    task = createTask("test-archive/a");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("b", result.result.get(0).name);

    task = createTask("test-archive/a/b");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("c", result.result.get(0).name);

    task = createTask("test-archive/a/b/c");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("d", result.result.get(0).name);

    task = createTask("test-archive/a/b/c/d");
    result = task.doInBackground();
    assertEquals(1, result.result.size());
    assertEquals("lipsum.bin", result.result.get(0).name);
    // assertEquals(512, result.get(0).size);
  }

  protected abstract CompressedHelperTask createTask(String relativePath);

  private void copyArchivesToStorage() throws IOException {
    for (File f : new File("src/test/resources").listFiles()) {
      IOUtils.copy(
          new FileInputStream(f),
          new FileOutputStream(new File(Environment.getExternalStorageDirectory(), f.getName())));
    }
  }
}
