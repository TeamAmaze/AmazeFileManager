/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.compressed.sevenz;

/// Map between folders, files and streams.
class StreamMap {
  /// The first Archive.packStream index of each folder.
  int[] folderFirstPackStreamIndex;
  /// Offset to beginning of this pack stream's data, relative to the beginning of the first pack
  // stream.
  long[] packStreamOffsets;
  /// Index of first file for each folder.
  int[] folderFirstFileIndex;
  /// Index of folder for each file.
  int[] fileFolderIndex;

  @Override
  public String toString() {
    return "StreamMap with indices of "
        + folderFirstPackStreamIndex.length
        + " folders, offsets of "
        + packStreamOffsets.length
        + " packed streams,"
        + " first files of "
        + folderFirstFileIndex.length
        + " folders and"
        + " folder indices for "
        + fileFolderIndex.length
        + " files";
  }
}
