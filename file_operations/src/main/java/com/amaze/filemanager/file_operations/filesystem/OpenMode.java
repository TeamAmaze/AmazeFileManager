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

package com.amaze.filemanager.file_operations.filesystem;

/**
 * Created by vishal on 10/11/16.
 *
 * <p>Class denotes the type of file being handled
 */
public enum OpenMode {
  UNKNOWN,
  FILE,
  SMB,
  SFTP,

  /** Custom file types like apk/images/downloads (which don't have a defined path) */
  CUSTOM,

  ROOT,
  OTG,
  DOCUMENT_FILE,
  GDRIVE,
  DROPBOX,
  BOX,
  ONEDRIVE;

  /**
   * Get open mode based on the id assigned. Generally used to retrieve this type after config
   * change or to send enum as argument
   *
   * @param ordinal the position of enum starting from 0 for first element
   */
  public static OpenMode getOpenMode(int ordinal) {
    return OpenMode.values()[ordinal];
  }
}
