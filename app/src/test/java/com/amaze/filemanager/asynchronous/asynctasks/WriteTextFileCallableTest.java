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

package com.amaze.filemanager.asynchronous.asynctasks;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.ftpserver.util.IoUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import com.amaze.filemanager.asynchronous.asynctasks.texteditor.write.WriteTextFileCallable;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.root.ConcatenateFileCommand;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class WriteTextFileCallableTest {

  private static final String contents = "This is modified data";

  @Test
  public void testWriteContentUri()
      throws ShellNotRunningException, IOException, StreamNotFoundException {
    Uri uri = Uri.parse("content://com.amaze.filemanager.test/foobar.txt");
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    shadowOf(cr).registerOutputStream(uri, bout);

    WriteTextFileCallable task =
        new WriteTextFileCallable(
            ctx, cr, new EditableFileAbstraction(ctx, uri), contents, null, false);
    task.call();
    assertEquals(contents, new String(bout.toByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testWriteFileNonRoot()
      throws IOException, StreamNotFoundException, ShellNotRunningException {
    File file = new File(Environment.getExternalStorageDirectory(), "test.txt");
    Uri uri = Uri.fromFile(file);
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();
    WriteTextFileCallable task =
        new WriteTextFileCallable(
            ctx, cr, new EditableFileAbstraction(ctx, uri), contents, null, false);
    task.call();

    String verify = IoUtils.readFully(new FileInputStream(file));
    assertEquals(contents, verify);
  }

  @Test
  public void testWriteFileOverwriting()
      throws IOException, StreamNotFoundException, ShellNotRunningException {
    File file = new File(Environment.getExternalStorageDirectory(), "test.txt");
    IoUtils.copy(new StringReader("Dummy test content"), new FileWriter(file), 1024);
    Uri uri = Uri.fromFile(file);
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();
    WriteTextFileCallable task =
        new WriteTextFileCallable(
            ctx, cr, new EditableFileAbstraction(ctx, uri), contents, null, false);
    task.call();

    String verify = IoUtils.readFully(new FileInputStream(file));
    assertEquals(contents, verify);
  }

  @Test
  @Config(shadows = {BlockAllOutputStreamsFileUtil.class, BypassMountPartitionRootUtils.class})
  public void testWriteFileRoot()
      throws IOException, StreamNotFoundException, ShellNotRunningException {
    File file = new File(Environment.getExternalStorageDirectory(), "test.txt");
    File cacheFile = File.createTempFile("test.txt", "cache");
    cacheFile.deleteOnExit();
    Uri uri = Uri.fromFile(file);
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();
    WriteTextFileCallable task =
        new WriteTextFileCallable(
            ctx, cr, new EditableFileAbstraction(ctx, uri), contents, cacheFile, true);
    task.call();

    String verify = IoUtils.readFully(new FileInputStream(file));
    assertEquals(contents, verify);
  }

  @Test
  @Config(shadows = {BlockAllOutputStreamsFileUtil.class})
  public void testWriteFileRootNoCacheFile()
      throws ShellNotRunningException, IOException, StreamNotFoundException {
    File file = new File(Environment.getExternalStorageDirectory(), "test.txt");
    Uri uri = Uri.fromFile(file);
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();
    try {
      WriteTextFileCallable task =
          new WriteTextFileCallable(
              ctx, cr, new EditableFileAbstraction(ctx, uri), contents, null, true);
      task.call();
    } catch (StreamNotFoundException e) {
      return;
    }
    fail();
  }

  @Test
  @Config(shadows = {BlockAllOutputStreamsFileUtil.class})
  public void testWriteFileRootCacheFileNotFound()
      throws ShellNotRunningException, IOException, StreamNotFoundException {
    File file = new File(Environment.getExternalStorageDirectory(), "test.txt");
    Uri uri = Uri.fromFile(file);
    File cacheFile = new File(Environment.getExternalStorageDirectory(), "test.txt.cache");
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();

    try {
      WriteTextFileCallable task =
          new WriteTextFileCallable(
              ctx, cr, new EditableFileAbstraction(ctx, uri), contents, cacheFile, true);
      task.call();
    } catch (StreamNotFoundException e) {
      return;
    }
    fail();
  }

  @Test
  @Config(shadows = {ShellNotRunningRootUtils.class})
  public void testWriteFileRootShellNotRunning()
      throws IOException, StreamNotFoundException, ShellNotRunningException {
    File file = new File(Environment.getExternalStorageDirectory(), "test.txt");
    Uri uri = Uri.fromFile(file);
    File cacheFile = File.createTempFile("test.txt", "cache");
    cacheFile.deleteOnExit();
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();

    try {
      WriteTextFileCallable task =
          new WriteTextFileCallable(
              ctx, cr, new EditableFileAbstraction(ctx, uri), contents, cacheFile, true);
      task.call();
    } catch (ShellNotRunningException e) {
      return;
    }
    fail();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteBogeyUri()
      throws ShellNotRunningException, IOException, StreamNotFoundException {
    Uri uri = Uri.parse("ftp://bogey.ftp/test.txt");
    Context ctx = ApplicationProvider.getApplicationContext();
    ContentResolver cr = ctx.getContentResolver();
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    shadowOf(cr).registerOutputStream(uri, bout);

    WriteTextFileCallable task =
        new WriteTextFileCallable(
            ctx, cr, new EditableFileAbstraction(ctx, uri), contents, null, false);

    task.call();
  }

  @Implements(FileUtil.class)
  public static class BlockAllOutputStreamsFileUtil {

    @Implementation
    public static OutputStream getOutputStream(final File target, Context context)
        throws FileNotFoundException {
      return null;
    }
  }

  @Implements(ConcatenateFileCommand.class)
  public static class BypassMountPartitionRootUtils {

    @Implementation
    public static void concatenateFile(String sourcePath, String destinationPath)
        throws ShellNotRunningException {
      try {
        IoUtils.copy(new FileInputStream(sourcePath), new FileOutputStream(destinationPath), 512);
      } catch (IOException e) {
        throw new ShellNotRunningException();
      }
    }
  }

  @Implements(ConcatenateFileCommand.class)
  public static class ShellNotRunningRootUtils {
    @Implementation
    public static void concatenateFile(String sourcePath, String destinationPath)
        throws ShellNotRunningException {
      throw new ShellNotRunningException();
    }
  }
}
