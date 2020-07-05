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

import org.junit.Ignore;
import org.junit.Test;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;

import android.os.Environment;

@Ignore("Test skipped due to problem at upstream library.")
public class SevenZipHelperTaskTest2 extends AbstractCompressedHelperTaskTest {

  @Test
  @Override
  public void testRoot() {
    CompressedHelperTask task = createTask("");
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertEquals(result.result.size(), 0);
  }

  @Test
  @Override
  @Ignore("Not testing this one")
  public void testSublevels() {}

  @Override
  protected CompressedHelperTask createTask(String relativePath) {
    return new SevenZipHelperTask(
        new File(Environment.getExternalStorageDirectory(), "compress.7z").getAbsolutePath(),
        relativePath,
        false,
        (data) -> {});
  }
}
