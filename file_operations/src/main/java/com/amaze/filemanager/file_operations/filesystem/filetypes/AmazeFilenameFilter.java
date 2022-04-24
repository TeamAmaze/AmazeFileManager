/*
 * Copyright (C) 2014-2013 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.file_operations.filesystem.filetypes;

// Android-changed: Removed @see tag (target does not exist on Android):
// @see     java.awt.FileDialog#setFilenameFilter(java.io.FilenameFilter)

/**
 * Instances of classes that implement this interface are used to filter filenames. These instances
 * are used to filter directory listings in the <code>list</code> method of class <code>File</code>,
 * and by the Abstract Window Toolkit's file dialog component.
 *
 * @author Arthur van Hoff
 * @author Jonathan Payne
 * @see java.io.File
 * @see java.io.File#list(java.io.FilenameFilter)
 * @since JDK1.0
 */
@FunctionalInterface
public interface AmazeFilenameFilter {
  /**
   * Tests if a specified file should be included in a file list.
   *
   * @param dir the directory in which the file was found.
   * @param name the name of the file.
   * @return <code>true</code> if and only if the name should be included in the file list; <code>
   *     false</code> otherwise.
   */
  boolean accept(AmazeFile dir, String name);
}
