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
import java.util.ArrayList;

import org.junit.Test;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;

import android.os.Environment;

public class XzHelperTaskTest2 extends AbstractCompressedHelperTaskTest {

  @Test
  @Override
  public void testRoot() {
    CompressedHelperTask task = createTask("");
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertEquals(result.result.size(), 1);
    assertEquals("compress", result.result.get(0).name);
  }

  @Test
  @Override
  public void testSublevels() {
    CompressedHelperTask task = createTask("compress");
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertEquals(result.result.size(), 3);
    assertEquals("a", result.result.get(0).name);
    assertEquals("bç", result.result.get(1).name);
    assertEquals("r.txt", result.result.get(2).name);
    assertEquals(4, result.result.get(2).size);

    task = createTask("compress/a");
    result = task.doInBackground();
    assertEquals(result.result.size(), 0);

    task = createTask("compress/bç");
    result = task.doInBackground();
    assertEquals(result.result.size(), 1);
    assertEquals("t.txt", result.result.get(0).name);
    assertEquals(6, result.result.get(0).size);
  }

  @Override
  protected CompressedHelperTask createTask(String relativePath) {
    return new XzHelperTask(
        new File(Environment.getExternalStorageDirectory(), "compress.tar.xz").getAbsolutePath(),
        relativePath,
        false,
        (data) -> {});
  }
}
