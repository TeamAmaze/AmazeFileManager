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

package com.amaze.filemanager.filesystem.compressed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.compress.ZipHelperTask;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.os.Environment;

@RunWith(RobolectricTestRunner.class)
@Config(
    shadows = {ShadowMultiDex.class},
    maxSdk = 27)
public class B0rkenZipTest {

  private File zipfile1 = new File(Environment.getExternalStorageDirectory(), "zip-slip.zip");
  private File zipfile2 = new File(Environment.getExternalStorageDirectory(), "zip-slip-win.zip");
  private File zipfile3 =
      new File(Environment.getExternalStorageDirectory(), "test-slashprefix.zip");

  private Extractor.OnUpdate emptyListener =
      new Extractor.OnUpdate() {

        @Override
        public void onStart(long totalBytes, String firstEntryName) {}

        @Override
        public void onUpdate(String entryPath) {}

        @Override
        public void onFinish() {}

        @Override
        public boolean isCancelled() {
          return false;
        }
      };

  @Before
  public void setUp() throws Exception {
    IOUtils.copy(
        getClass().getClassLoader().getResourceAsStream("zip-slip.zip"),
        new FileOutputStream(zipfile1));
    IOUtils.copy(
        getClass().getClassLoader().getResourceAsStream("zip-slip-win.zip"),
        new FileOutputStream(zipfile2));
    IOUtils.copy(
        getClass().getClassLoader().getResourceAsStream("test-slashprefix.zip"),
        new FileOutputStream(zipfile3));
  }

  @Test
  public void testExtractZipWithWrongPathUnix() throws Exception {
    Extractor extractor =
        new ZipExtractor(
            RuntimeEnvironment.application,
            zipfile1.getAbsolutePath(),
            Environment.getExternalStorageDirectory().getAbsolutePath(),
            emptyListener);
    extractor.extractEverything();
    assertEquals(1, extractor.getInvalidArchiveEntries().size());
    assertTrue(new File(Environment.getExternalStorageDirectory(), "good.txt").exists());
  }

  @Test
  public void testExtractZipWithWrongPathWindows() throws Exception {
    Extractor extractor =
        new ZipExtractor(
            RuntimeEnvironment.application,
            zipfile2.getAbsolutePath(),
            Environment.getExternalStorageDirectory().getAbsolutePath(),
            emptyListener);
    extractor.extractEverything();
    assertEquals(1, extractor.getInvalidArchiveEntries().size());
    assertTrue(new File(Environment.getExternalStorageDirectory(), "good.txt").exists());
  }

  @Test
  public void testExtractZipWithSlashPrefixEntry() throws Exception {
    Extractor extractor =
        new ZipExtractor(
            RuntimeEnvironment.application,
            zipfile3.getAbsolutePath(),
            Environment.getExternalStorageDirectory().getAbsolutePath(),
            emptyListener);
    extractor.extractFiles(new String[] {"/test.txt"});
    assertEquals(0, extractor.getInvalidArchiveEntries().size());
    assertTrue(new File(Environment.getExternalStorageDirectory(), "test.txt").exists());
  }

  @Test
  public void testZipHelperTaskShouldOmitInvalidEntries() throws Exception {
    ZipHelperTask task =
        new ZipHelperTask(
            RuntimeEnvironment.application, zipfile1.getAbsolutePath(), null, false, (data) -> {});
    List<CompressedObjectParcelable> result = task.execute().get().result;
    assertEquals(1, result.size());
    assertEquals("good.txt", result.get(0).path);
    assertEquals(
        RuntimeEnvironment.application.getString(R.string.multiple_invalid_archive_entries),
        ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testZipHelperTaskShouldOmitInvalidEntriesWithBackslash() throws Exception {
    ZipHelperTask task =
        new ZipHelperTask(
            RuntimeEnvironment.application, zipfile2.getAbsolutePath(), null, false, (data) -> {});
    List<CompressedObjectParcelable> result = task.execute().get().result;
    assertEquals(1, result.size());
    assertEquals("good.txt", result.get(0).path);
    assertEquals(
        RuntimeEnvironment.application.getString(R.string.multiple_invalid_archive_entries),
        ShadowToast.getTextOfLatestToast());
  }
}
