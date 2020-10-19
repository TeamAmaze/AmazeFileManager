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

package com.amaze.filemanager.filesystem.files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.test.DummyFileGenerator;
import com.amaze.filemanager.utils.ProgressHandler;

@RunWith(Theories.class)
public class GenericCopyUtilTest {

  private GenericCopyUtil copyUtil;

  private File file1, file2;

  public static final @DataPoints int fileSizes[] = {512, 187139366};

  @Before
  public void setUp() throws IOException {
    copyUtil = new GenericCopyUtil(RuntimeEnvironment.application, new ProgressHandler());
    file1 = File.createTempFile("test", "bin");
    file2 = File.createTempFile("test", "bin");
    file1.deleteOnExit();
    file2.deleteOnExit();
  }

  @Theory // doCopy(ReadableByteChannel in, WritableByteChannel out)
  public void testCopyFile1(int size) throws IOException, NoSuchAlgorithmException {
    byte[] checksum = DummyFileGenerator.createFile(file1, size);
    copyUtil.doCopy(
        new FileInputStream(file1).getChannel(),
        Channels.newChannel(new FileOutputStream(file2)),
        ServiceWatcherUtil.UPDATE_POSITION);
    assertEquals(file1.length(), file2.length());
    assertSha1Equals(checksum, file2);
  }

  @Theory // copy(FileChannel in, FileChannel out)
  public void testCopyFile2(int size) throws IOException, NoSuchAlgorithmException {
    byte[] checksum = DummyFileGenerator.createFile(file1, size);
    copyUtil.copyFile(
        new FileInputStream(file1).getChannel(),
        new FileOutputStream(file2).getChannel(),
        ServiceWatcherUtil.UPDATE_POSITION);
    assertEquals(file1.length(), file2.length());
    assertSha1Equals(checksum, file2);
  }

  @Theory // copy(BufferedInputStream in, BufferedOutputStream out)
  public void testCopyFile3(int size) throws IOException, NoSuchAlgorithmException {
    byte[] checksum = DummyFileGenerator.createFile(file1, size);
    copyUtil.copyFile(
        new BufferedInputStream(new FileInputStream(file1)),
        new BufferedOutputStream(new FileOutputStream(file2)),
        ServiceWatcherUtil.UPDATE_POSITION);
    assertEquals(file1.length(), file2.length());
    assertSha1Equals(checksum, file2);
  }

  private void assertSha1Equals(byte[] expected, File file)
      throws NoSuchAlgorithmException, IOException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    DigestInputStream in = new DigestInputStream(new FileInputStream(file), md);
    byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    while (in.read(buffer) > -1) {}
    in.close();
    assertArrayEquals(expected, md.digest());
  }
}
