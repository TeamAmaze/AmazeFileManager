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

package com.amaze.filemanager.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;

import android.os.Parcel;

/** Created by Rustam Khadipash on 29/3/2018. */
public class HybridFileParcelableTest {
  private HybridFileParcelable filePath;
  private HybridFileParcelable directory;
  private HybridFileParcelable file;

  @Before
  public void setUp() {
    filePath = new HybridFileParcelable("/storage/sdcard0/Test1/Test1.txt");

    directory = new HybridFileParcelable("/storage/sdcard0/Test2", "rw", 123456, 654321, true);

    file =
        new HybridFileParcelable("/storage/sdcard0/Test3/Test3.txt", "rw", 123456, 654321, false);
  }

  /** Purpose: Get name of a file / directory Input: no Expected: return file / directory's name */
  @Test
  public void getName() {
    assertEquals("Test1.txt", filePath.getName());
    assertEquals("Test2", directory.getName());
    assertEquals("Test3.txt", file.getName());
  }

  /**
   * Purpose: Change name of a file / directory Input: setName newName Expected: File / directory's
   * name is changed
   */
  @Test
  public void setName() {
    filePath.setName("Tset1.txt");
    assertEquals("Tset1.txt", filePath.getName());
    directory.setName("Tset2");
    assertEquals("Tset2", directory.getName());
    file.setName("Tset3.txt");
    assertEquals("Tset3.txt", file.getName());
  }

  /**
   * Purpose: Get open mode of a file / directory Input: no Expected: return OpenMode.FILE
   *
   * <p>All files and directories in this class have open mode set as OpenMode.FILE
   */
  @Test
  public void getMode() {
    assertEquals(OpenMode.FILE, filePath.getMode());
    assertEquals(OpenMode.FILE, directory.getMode());
    assertEquals(OpenMode.FILE, file.getMode());
  }

  /** Purpose: Get link to a file / directory Input: no Expected: return empty string */
  @Test
  public void getLink() {
    assertEquals("", filePath.getLink());
    assertEquals("", directory.getLink());
    assertEquals("", file.getLink());
  }

  /**
   * Purpose: Set link to a file / directory Input: setLink newLink Expected: File / directory's
   * link is changed
   */
  @Test
  public void setLink() {
    filePath.setLink("abc");
    assertEquals("abc", filePath.getLink());
    directory.setLink("def");
    assertEquals("def", directory.getLink());
    file.setLink("ghi");
    assertEquals("ghi", file.getLink());
  }

  /** Purpose: Get creation date of a file / directory Input: no Expected: return date */
  @Test
  public void getDate() {
    assertEquals(0, filePath.getDate());
    assertEquals(123456, directory.getDate());
    assertEquals(123456, file.getDate());
  }

  /**
   * Purpose: Change creation date of a file / directory Input: setDate newDate Expected: File /
   * directory's creation date is changed
   */
  @Test
  public void setDate() {
    filePath.setDate(746352);
    assertEquals(746352, filePath.getDate());
    directory.setDate(474587);
    assertEquals(474587, directory.getDate());
    file.setDate(3573335);
    assertEquals(3573335, file.getDate());
  }

  /** Purpose: Get size of a file / directory Input: no Expected: return size */
  @Test
  public void getSize() {
    assertEquals(0, filePath.getSize());
    assertEquals(654321, directory.getSize());
    assertEquals(654321, file.getSize());
  }

  /**
   * Purpose: Change size of a file / directory Input: setSize newSize Expected: File / directory's
   * size is changed
   */
  @Test
  public void setSize() {
    filePath.setSize(3534546);
    assertEquals(3534546, filePath.getSize());
    directory.setSize(1745534);
    assertEquals(1745534, directory.getSize());
    file.setSize(7546543);
    assertEquals(7546543, file.getSize());
  }

  /** Purpose: Check whether it is a file or directory Input: no Expected: return directory */
  @Test
  public void isDirectory() {
    assertEquals(false, filePath.isDirectory());
    assertEquals(true, directory.isDirectory());
    assertEquals(false, file.isDirectory());
  }

  /**
   * Purpose: Change type to file / directory Input: setDirectory isDirectory Expected: File /
   * directory's type is changed
   */
  @Test
  public void setDirectory() {
    filePath.setDirectory(true);
    assertEquals(true, filePath.isDirectory());
    directory.setDirectory(false);
    assertEquals(false, directory.isDirectory());
    file.setDirectory(true);
    assertEquals(true, file.isDirectory());
  }

  /** Purpose: Get path to a file / directory Input: no Expected: return path */
  @Test
  public void getPath() {
    assertEquals("/storage/sdcard0/Test1/Test1.txt", filePath.getPath());
    assertEquals("/storage/sdcard0/Test2", directory.getPath());
    assertEquals("/storage/sdcard0/Test3/Test3.txt", file.getPath());
  }

  /** Purpose: Get file / directory's permissions Input: no Expected: return permissions */
  @Test
  public void getPermission() {
    assertEquals(null, filePath.getPermission());
    assertEquals("rw", directory.getPermission());
    assertEquals("rw", file.getPermission());
  }

  /**
   * Purpose: Change permissions of a file / directory Input: setPermission newPermission Expected:
   * File / directory's permissions are changed
   */
  @Test
  public void setPermission() {
    filePath.setPermission("rwx");
    assertEquals("rwx", filePath.getPermission());
    directory.setPermission("rwx");
    assertEquals("rwx", directory.getPermission());
    file.setPermission("rwx");
    assertEquals("rwx", file.getPermission());
  }

  /**
   * Purpose: Write an object into a parcel, send it through an intent and read the object from the
   * parcel Input: writeToParcel parcel object Expected: The parcel can be sent and the object can
   * be extracted from the parcel
   */
  @Test
  public void writeToParcel() {
    Parcel parcel = Parcel.obtain();
    file.writeToParcel(parcel, file.describeContents());
    parcel.setDataPosition(0);

    HybridFileParcelable createdFromParcel = HybridFileParcelable.CREATOR.createFromParcel(parcel);
    assertEquals(file.getDate(), createdFromParcel.getDate());
    assertEquals(file.getLink(), createdFromParcel.getLink());
    assertEquals(file.getMode(), createdFromParcel.getMode());
    assertEquals(file.getName(), createdFromParcel.getName());
    assertEquals(file.getPath(), createdFromParcel.getPath());
    assertEquals(file.getPermission(), createdFromParcel.getPermission());
    assertEquals(file.getSize(), createdFromParcel.getSize());
    assertEquals(file.isDirectory(), createdFromParcel.isDirectory());
  }
}
