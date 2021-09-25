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

package com.amaze.filemanager.utils;

import static com.amaze.filemanager.file_operations.filesystem.OpenMode.BOX;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.CUSTOM;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.DOCUMENT_FILE;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.DROPBOX;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.FILE;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.GDRIVE;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.ONEDRIVE;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.OTG;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.ROOT;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.SFTP;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.SMB;
import static com.amaze.filemanager.file_operations.filesystem.OpenMode.UNKNOWN;
import static java.lang.Integer.MAX_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;

public class OpenModeTest {

  @Test
  public void testGetOpenMode() {
    assertEquals(UNKNOWN, OpenMode.getOpenMode(0));
    assertEquals(FILE, OpenMode.getOpenMode(1));
    assertEquals(SMB, OpenMode.getOpenMode(2));
    assertEquals(SFTP, OpenMode.getOpenMode(3));
    assertEquals(CUSTOM, OpenMode.getOpenMode(4));
    assertEquals(ROOT, OpenMode.getOpenMode(5));
    assertEquals(OTG, OpenMode.getOpenMode(6));
    assertEquals(DOCUMENT_FILE, OpenMode.getOpenMode(7));
    assertEquals(GDRIVE, OpenMode.getOpenMode(8));
    assertEquals(DROPBOX, OpenMode.getOpenMode(9));
    assertEquals(BOX, OpenMode.getOpenMode(10));
    assertEquals(ONEDRIVE, OpenMode.getOpenMode(11));
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> OpenMode.getOpenMode(-1));
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> OpenMode.getOpenMode(MAX_VALUE));
  }
}
