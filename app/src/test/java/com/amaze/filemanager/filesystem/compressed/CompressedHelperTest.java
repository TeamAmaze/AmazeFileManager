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

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.file_operations.utils.UpdatePosition;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.Bzip2Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.GzipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.LzmaExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.SevenZipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.XzExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.Bzip2Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.GzipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.LzmaDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.SevenZipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.XzDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.ZipDecompressor;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class CompressedHelperTest {

  private Context context;
  private Extractor.OnUpdate emptyUpdateListener;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    emptyUpdateListener =
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
  }

  /**
   * Extractor check This program use 6 extension and 4 extractor. Check if each extensions matched
   * correct extractor
   */
  @Test
  public void getExtractorInstance() {
    UpdatePosition updatePosition = ServiceWatcherUtil.UPDATE_POSITION;

    File file = new File("/test/test.zip"); // .zip used by ZipExtractor
    Extractor result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), ZipExtractor.class);
    file = new File("/test/test.jar"); // .jar used by ZipExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), ZipExtractor.class);
    file = new File("/test/test.apk"); // .apk used by ZipExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), ZipExtractor.class);
    file = new File("/test/test.tar"); // .tar used by TarExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), TarExtractor.class);
    file = new File("/test/test.tar.gz"); // .tar.gz used by GzipExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), GzipExtractor.class);
    file = new File("/test/test.tgz"); // .tgz used by GzipExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), GzipExtractor.class);
    file = new File("/test/test.rar"); // .rar used by RarExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), RarExtractor.class);
    file = new File("/test/test.tar.bz2"); // .rar used by RarExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), Bzip2Extractor.class);
    file = new File("/test/test.tbz"); // .rar used by RarExtractor
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), Bzip2Extractor.class);
    file = new File("/test/test.7z");
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), SevenZipExtractor.class);
    file = new File("/test/test.tar.xz");
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), XzExtractor.class);
    file = new File("/test/test.tar.lzma");
    result =
        CompressedHelper.getExtractorInstance(
            context, file, "/test2", emptyUpdateListener, updatePosition);
    assertEquals(result.getClass(), LzmaExtractor.class);
  }

  /**
   * Decompressor check This program use 6 extension and 4 decompressor. Check if each extensions
   * matched correct decompressor
   */
  @Test
  public void getCompressorInstance() {
    File file = new File("/test/test.zip"); // .zip used by ZipDecompressor
    Decompressor result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), ZipDecompressor.class);
    file = new File("/test/test.jar"); // .jar used by ZipDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), ZipDecompressor.class);
    file = new File("/test/test.apk"); // .apk used by ZipDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), ZipDecompressor.class);
    file = new File("/test/test.tar"); // .tar used by TarDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), TarDecompressor.class);
    file = new File("/test/test.tar.gz"); // .tar.gz used by GzipDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), GzipDecompressor.class);
    file = new File("/test/test.tgz"); // .tar.gz used by GzipDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), GzipDecompressor.class);
    file = new File("/test/test.rar"); // .rar used by RarDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), RarDecompressor.class);
    file = new File("/test/test.tar.bz2"); // .rar used by RarDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), Bzip2Decompressor.class);
    file = new File("/test/test.tbz"); // .rar used by RarDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), Bzip2Decompressor.class);
    file = new File("/test/test.7z"); // Can't use 7zip
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), SevenZipDecompressor.class);
    file = new File("/test/test.tar.xz"); // .rar used by RarDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), XzDecompressor.class);
    file = new File("/test/test.tar.lzma"); // .rar used by RarDecompressor
    result = CompressedHelper.getCompressorInstance(context, file);
    assertEquals(result.getClass(), LzmaDecompressor.class);
  }

  /** isFileExtractable() fuction test extension check */
  @Test
  public void isFileExtractableTest() throws Exception {
    // extension in code. So, it return true
    assertTrue(CompressedHelper.isFileExtractable("/test/test.zip"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.rar"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.tar"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.tar.gz"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.tgz"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.tar.bz2"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.tbz"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.tar.lzma"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.jar"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.apk"));
    assertTrue(CompressedHelper.isFileExtractable("/test/test.7z"));

    // extension not in code. So, it return false
    assertFalse(CompressedHelper.isFileExtractable("/test/test.z"));
  }

  /**
   * getFileName() function test it return file name. But, if it is invalid compressed file, return
   * file name with extension
   */
  @Test
  public void getFileNameTest() throws Exception {
    assertEquals("test", CompressedHelper.getFileName("test.zip"));
    assertEquals("test", CompressedHelper.getFileName("test.rar"));
    assertEquals("test", CompressedHelper.getFileName("test.tar"));
    assertEquals("test", CompressedHelper.getFileName("test.tar.gz"));
    assertEquals("test", CompressedHelper.getFileName("test.tgz"));
    assertEquals("test", CompressedHelper.getFileName("test.tar.bz2"));
    assertEquals("test", CompressedHelper.getFileName("test.tbz"));
    assertEquals("test", CompressedHelper.getFileName("test.tar.lzma"));
    assertEquals("test", CompressedHelper.getFileName("test.jar"));
    assertEquals("test", CompressedHelper.getFileName("test.apk"));
    assertEquals("test", CompressedHelper.getFileName("test.7z"));

    // no extension(directory)
    assertEquals("test", CompressedHelper.getFileName("test"));

    // invalid extension
    assertEquals("test.z", CompressedHelper.getFileName("test.z"));

    // no path
    assertEquals("", CompressedHelper.getFileName(""));
  }
}
