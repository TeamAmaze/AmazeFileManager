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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.Test;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.github.junrar.exception.UnsupportedRarV5Exception;

import android.os.Environment;

public class RarHelperTaskTest extends AbstractCompressedHelperTaskTest {

  @Test
  public void testMultiVolumeRar() {
    CompressedHelperTask task =
        new RarHelperTask(
            new File(
                    Environment.getExternalStorageDirectory(),
                    "test-multipart-archive-v4.part1.rar")
                .getAbsolutePath(),
            "",
            false,
            data -> {});
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertNotNull(result);
    assertNotNull(result.result);
    assertEquals(1, result.result.size());
    assertEquals("test.bin", result.result.get(0).name);
    assertEquals(1024 * 128, result.result.get(0).size);
  }

  @Test
  public void testMultiVolumeRarV5() {
    CompressedHelperTask task =
        new RarHelperTask(
            new File(
                    Environment.getExternalStorageDirectory(),
                    "test-multipart-archive-v5.part1.rar")
                .getAbsolutePath(),
            "",
            false,
            data -> {});
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
    assertNotNull(result);
    assertNull(result.result);
    assertNotNull(result.exception);
    assertEquals(ArchiveException.class, result.exception.getClass());
    assertEquals(UnsupportedRarV5Exception.class, result.exception.getCause().getClass());
  }

  @Override
  protected CompressedHelperTask createTask(String relativePath) {
    return new RarHelperTask(
        new File(Environment.getExternalStorageDirectory(), "test-archive.rar").getAbsolutePath(),
        relativePath,
        false,
        (data) -> {});
  }
}
