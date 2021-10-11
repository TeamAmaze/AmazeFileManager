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

import java.io.File;

/**
 * A filter for abstract pathnames.
 *
 * <p>Instances of this interface may be passed to the <code>{@link
 * File#listFiles(java.io.FileFilter) listFiles(FileFilter)}</code> method of the <code>
 * {@link java.io.File}</code> class.
 *
 * @since 1.2
 */
@FunctionalInterface
public interface AmazeFileFilter {

  /**
   * Tests whether or not the specified abstract pathname should be included in a pathname list.
   *
   * @param pathname The abstract pathname to be tested
   * @return <code>true</code> if and only if <code>pathname</code> should be included
   */
  boolean accept(AmazeFile pathname);
}
