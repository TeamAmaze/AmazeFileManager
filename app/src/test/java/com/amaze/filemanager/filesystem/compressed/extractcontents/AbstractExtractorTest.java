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

package com.amaze.filemanager.filesystem.compressed.extractcontents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;

import com.amaze.filemanager.filesystem.compressed.ArchivePasswordCache;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.content.Context;
import android.os.Environment;

@RunWith(RobolectricTestRunner.class)
@Config(
    shadows = {ShadowMultiDex.class},
    minSdk = 14,
    maxSdk = 27)
public abstract class AbstractExtractorTest {

  protected abstract Class<? extends Extractor> extractorClass();

  protected abstract String getArchiveType();

  @Before
  public void setUp() throws Exception {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    copyArchivesToStorage();
  }

  @After
  public void tearDown() throws Exception {
    ArchivePasswordCache.getInstance().clear();
    File extractedArchiveRoot = new File(Environment.getExternalStorageDirectory(), "test-archive");
    if (extractedArchiveRoot.exists()) {
      Files.walk(Paths.get(extractedArchiveRoot.getAbsolutePath()))
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @Test
  public void testFixEntryName() throws Exception {
    Extractor extractor =
        extractorClass()
            .getConstructor(Context.class, String.class, String.class, Extractor.OnUpdate.class)
            .newInstance(
                RuntimeEnvironment.application,
                getArchiveFile().getAbsolutePath(),
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                null);

    assertEquals("test.txt", extractor.fixEntryName("test.txt"));
    assertEquals("test.txt", extractor.fixEntryName("/test.txt"));
    assertEquals("test.txt", extractor.fixEntryName("/////////test.txt"));
    assertEquals("test/", extractor.fixEntryName("/test/"));
    assertEquals("test/a/b/c/d/e/", extractor.fixEntryName("/test/a/b/c/d/e/"));
    assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("a/b/c/d/e/test.txt"));
    assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("/a/b/c/d/e/test.txt"));
    assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("///////a/b/c/d/e/test.txt"));

    // It is known redundant slashes inside path components are NOT tampered.
    assertEquals("a/b/c//d//e//test.txt", extractor.fixEntryName("a/b/c//d//e//test.txt"));
    assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("a/b/c/d/e/test.txt"));
    assertEquals("test.txt", extractor.fixEntryName("\\test.txt"));
    assertEquals("test.txt", extractor.fixEntryName("\\\\\\\\\\\\\\\\\\\\test.txt"));
    assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("\\a\\b\\c\\d\\e\\test.txt"));
    assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("\\a\\b/c\\d\\e/test.txt"));
  }

  @Test
  public void testExtractFiles() throws Exception {
    doTestExtractFiles();
  }

  protected void doTestExtractFiles() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Extractor extractor =
        extractorClass()
            .getConstructor(Context.class, String.class, String.class, Extractor.OnUpdate.class)
            .newInstance(
                RuntimeEnvironment.application,
                getArchiveFile().getAbsolutePath(),
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                new Extractor.OnUpdate() {

                  @Override
                  public void onStart(long totalBytes, String firstEntryName) {}

                  @Override
                  public void onUpdate(String entryPath) {}

                  @Override
                  public void onFinish() {
                    latch.countDown();
                    try {
                      verifyExtractedArchiveContents();
                    } catch (IOException e) {
                      e.printStackTrace();
                      fail("Error verifying extracted archive contents");
                    }
                  }

                  @Override
                  public boolean isCancelled() {
                    return false;
                  }
                });
    extractor.extractEverything();
    latch.await();
  }

  private void verifyExtractedArchiveContents() throws IOException {
    File extractedArchiveRoot = new File(Environment.getExternalStorageDirectory(), "test-archive");
    assertTrue(extractedArchiveRoot.exists());
    assertTrue(new File(extractedArchiveRoot, "1").exists());
    assertTrue(new File(extractedArchiveRoot, "2").exists());
    assertTrue(new File(extractedArchiveRoot, "3").exists());
    assertTrue(new File(extractedArchiveRoot, "4").exists());
    assertTrue(new File(extractedArchiveRoot, "a").exists());

    assertTrue(new File(new File(extractedArchiveRoot, "1"), "8").exists());
    assertTrue(new File(new File(extractedArchiveRoot, "2"), "7").exists());
    assertTrue(new File(new File(extractedArchiveRoot, "3"), "6").exists());
    assertTrue(new File(new File(extractedArchiveRoot, "4"), "5").exists());
    assertTrue(new File(new File(extractedArchiveRoot, "a/b/c/d"), "lipsum.bin").exists());

    assertTrue(
        IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "1"), "8")))
                .length
            == 2);
    assertTrue(
        IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "2"), "7")))
                .length
            == 3);
    assertTrue(
        IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "3"), "6")))
                .length
            == 4);
    assertTrue(
        IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "4"), "5")))
                .length
            == 5);
    assertTrue(
        IOUtils.toByteArray(
                    new FileInputStream(
                        new File(new File(extractedArchiveRoot, "a/b/c/d"), "lipsum.bin")))
                .length
            == 512);
  }

  private void copyArchivesToStorage() throws IOException {
    for (File f : new File("src/test/resources").listFiles()) {
      IOUtils.copy(
          new FileInputStream(f),
          new FileOutputStream(new File(Environment.getExternalStorageDirectory(), f.getName())));
    }
  }

  protected File getArchiveFile() {
    return new File(Environment.getExternalStorageDirectory(), "test-archive." + getArchiveType());
  }
}
