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

package com.amaze.filemanager.file_operations.filesystem.smbstreamer;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import com.amaze.filemanager.file_operations.filesystem.smbstreamer.StreamSource;
import com.amaze.filemanager.file_operations.shadows.ShadowMultiDex;
import com.amaze.filemanager.file_operations.shadows.jcifs.smb.ShadowSmbFile;

import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import jcifs.smb.SmbFile;

/** Created by Rustam Khadipash on 30/3/2018. */
@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class, ShadowSmbFile.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class StreamSourceTest {
  private SmbFile file;
  private StreamSource ss;
  private byte[] text;

  @Before
  public void setUp() throws IOException {
    StringBuilder textInFile = new StringBuilder();
    for (int i = 0; i < 20; i++) textInFile.append("a");

    text = textInFile.toString().getBytes();
    file = createFile();
    ss = new StreamSource(file, file.length());
  }

  @After
  public void tearDown() {
    if (ss != null) ss.close();
  }

  private SmbFile createFile() throws IOException {
    File testFile = new File(Environment.getExternalStorageDirectory(), "Test.txt");
    testFile.createNewFile();

    OutputStream is = new FileOutputStream(testFile);
    is.write(text);
    is.flush();
    is.close();

    SmbFile file = new SmbFile("smb://127.0.0.1/Test.txt");
    ShadowSmbFile shadowSmbFile = Shadow.extract(file);
    shadowSmbFile.setFile(testFile);

    return file;
  }

  /*
   From now on ssEmpty will not be used since StreamSource()
   constructor does not initialize any internal variables
  */

  /** Purpose: Open an existing file Input: no Expected: cs.read() = 1 buff[0] = text[0] */
  @Test
  public void openExisting() throws IOException {
    ss.open();
    byte[] buff = new byte[1];

    assertEquals(buff.length, ss.read(buff));
    assertEquals(text[0], buff[0]);
  }

  /**
   * Purpose: Read content of length equal to the buffer size from the file Input: read(buffer)
   * Expected: buffer = text n = len(buffer)
   */
  @Test
  public void read() throws IOException {
    ss.open();
    byte[] buff = new byte[10];
    int n = ss.read(buff);
    byte[] temp = Arrays.copyOfRange(text, 0, buff.length);

    assertArrayEquals(temp, buff);
    assertEquals(buff.length, n);
  }

  /**
   * Purpose: Read content from the file with the buffer size bigger than length of the text in the
   * file Input: read(buffer) Expected: buffer = text n = len
   */
  @Test
  public void readExceed() throws IOException {
    ss.open();
    byte[] buff = new byte[100];
    int n = ss.read(buff);
    // erase dummy values in the end of buffer
    byte[] buffer = Arrays.copyOfRange(buff, 0, n);

    assertArrayEquals(text, buffer);
    assertEquals(text.length, n);
  }

  /**
   * Purpose: Throw an exception when reading happen on a closed file Input: read(buffer) Expected:
   * IOException is thrown
   */
  @Test(expected = IOException.class)
  public void readClosedException() throws IOException {
    ss.open();
    ss.close();
    byte[] buff = new byte[text.length];
    int n = ss.read(buff);
  }

  /**
   * Purpose: Read content in certain positions of the buffer from the file Input: read(buffer,
   * startPosition, endPosition) Expected: buffer = text n = endPosition
   */
  @Test
  public void readStartEnd() throws IOException {
    ss.open();
    byte[] buff = new byte[100];
    int start = 5;
    int end = 10;

    int n = ss.read(buff, start, end);
    byte[] file = Arrays.copyOfRange(text, 0, end - start);
    byte[] buffer = Arrays.copyOfRange(buff, start, end);

    assertArrayEquals(file, buffer);
    assertEquals(end, n);
  }

  /**
   * Purpose: Throw an exception when start and/or end positions for writing in the buffer exceed
   * size of the buffer Input: read(buffer, startPosition, endPosition) Expected:
   * IndexOutOfBoundsException is thrown
   */
  @Test(expected = IndexOutOfBoundsException.class)
  public void readStartEndExceedException() throws IOException {
    ss.open();

    byte[] buff = new byte[100];
    int start = 95;
    int end = 110;

    ss.read(buff, start, end);
  }

  /**
   * Purpose: Throw an exception when reading happen on a closed file Input: read(buffer,
   * startPosition, endPosition) Expected: IOException is thrown
   */
  @Test(expected = IOException.class)
  public void readStartEndClosedException() throws IOException {
    ss.open();
    ss.close();
    byte[] buff = new byte[100];
    int start = 5;
    int end = 10;

    int n = ss.read(buff, start, end);
  }

  /**
   * Purpose: Read content of the file from a certain position Input: moveTo(readPosition),
   * read(buff) Expected: buff = text[readPosition] n = buff.length
   */
  @Test
  public void moveTo() throws IOException {
    int readPosition = text.length - 10;
    byte[] buff = new byte[1];

    ss.moveTo(readPosition);
    ss.open();

    int n = ss.read(buff);
    assertEquals(text[readPosition], buff[0]);
    assertEquals(buff.length, n);
  }

  /**
   * Purpose: Throw an exception when a reading position in the file is incorrect Input:
   * moveTo(wrongPosition) Expected: IllegalArgumentException is thrown
   */
  @Test(expected = IllegalArgumentException.class)
  public void moveToException() throws IllegalArgumentException, IOException {
    ss.open();
    ss.moveTo(-1);
  }

  /**
   * Purpose: Close file after successful reading Input: no Expected: Stream is closed and reading
   * from the file is unavailable
   */
  @Test
  public void close() throws IOException {
    ss.open();
    ss.close();

    int n = -1;
    try {
      byte[] buff = new byte[1];
      n = ss.read(buff);
    } catch (IOException ignored) {
    }

    assertEquals(-1, n);
  }

  /** Purpose: Get MIME type Input: no Expected: return "txt" */
  @Test
  public void getMimeType() {
    assertEquals("txt", ss.getMimeType());
  }

  /** Purpose: Get length of the text from a file Input: no Expected: return len */
  @Test
  public void length() {
    assertEquals(text.length, ss.length());
  }

  /** Purpose: Get name of a file Input: no Expected: return "Test.txt" */
  @Test
  public void getName() {
    assertEquals(file.getName(), ss.getName());
  }

  /**
   * Purpose: Get available to read remain amount of text from a file Input: no Expected: return
   * amount
   */
  @Test
  public void available() throws IOException {
    int amount = 12;
    ss.moveTo(text.length - amount);
    assertEquals(amount, ss.availableExact());
  }

  /** Purpose: Move reading position to the beginning of a file Input: no Expected: return len */
  @Test
  public void reset() throws IOException {
    ss.moveTo(10);
    assertEquals(text.length - 10, ss.availableExact());
    ss.reset();
    assertEquals(text.length, ss.availableExact());
  }

  /** Purpose: Get a file object Input: no Expected: return SmbFile */
  @Test
  public void getFile() {
    assertEquals(file, ss.getFile());
  }
}
