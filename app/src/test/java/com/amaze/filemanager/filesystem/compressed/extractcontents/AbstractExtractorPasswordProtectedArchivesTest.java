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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.amaze.filemanager.filesystem.compressed.ArchivePasswordCache;

import android.os.Environment;

public abstract class AbstractExtractorPasswordProtectedArchivesTest extends AbstractExtractorTest {

  @Test(expected = IOException.class)
  public void testExtractFilesWithoutPassword() throws Exception {
    ArchivePasswordCache.getInstance().clear();
    try {
      doTestExtractFiles();
    } catch (IOException e) {
      assertExceptionIsExpected(e);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void testExtractFilesWithWrongPassword() throws Exception {
    ArchivePasswordCache.getInstance().clear();
    ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "abcdef");
    try {
      doTestExtractFiles();
    } catch (IOException e) {
      assertExceptionIsExpected(e);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void testExtractFilesWithRepeatedWrongPassword() throws Exception {
    ArchivePasswordCache.getInstance().clear();
    ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "abcdef");
    try {
      doTestExtractFiles();
    } catch (IOException e) {
      assertExceptionIsExpected(e);
      throw e;
    }
    ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "pqrstuv");
    try {
      doTestExtractFiles();
    } catch (IOException e) {
      assertExceptionIsExpected(e);
      throw e;
    }
  }

  @Test
  @Override
  public void testExtractFiles() throws Exception {
    ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "123456");
    doTestExtractFiles();
  }

  @Override
  protected File getArchiveFile() {
    return new File(
        Environment.getExternalStorageDirectory(), "test-archive-encrypted." + getArchiveType());
  }

  protected abstract Class[] expectedRootExceptionClass();

  protected void assertExceptionIsExpected(IOException e) throws IOException {
    for (Class<? extends Throwable> c : expectedRootExceptionClass()) {
      if (e.getCause() != null
          ? (c.isAssignableFrom(e.getCause().getClass()))
          : c.isAssignableFrom(e.getClass())) return;
    }
    fail("Exception verification failed.");
    throw e;
  }
}
