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

import java.util.Comparator;

import com.amaze.filemanager.adapters.data.LayoutElementParcelable;

public class FileListSorter implements Comparator<LayoutElementParcelable> {

  private int dirsOnTop = 0;
  private int asc = 1;
  private int sort = 0;

  public FileListSorter(int dir, int sort, int asc) {
    this.dirsOnTop = dir;
    this.asc = asc;
    this.sort = sort;
  }

  private boolean isDirectory(LayoutElementParcelable path) {
    return path.isDirectory;
  }

  /**
   * Compares two elements and return negative, zero and positive integer if first argument is less
   * than, equal to or greater than second
   */
  @Override
  public int compare(LayoutElementParcelable file1, LayoutElementParcelable file2) {

    /*File f1;

    if(!file1.hasSymlink()) {

        f1=new File(file1.getDesc());
    } else {
        f1=new File(file1.getSymlink());
    }

    File f2;

    if(!file2.hasSymlink()) {

        f2=new File(file2.getDesc());
    } else {
        f2=new File(file1.getSymlink());
    }*/

    if (dirsOnTop == 0) {
      if (isDirectory(file1) && !isDirectory(file2)) {
        return -1;

      } else if (isDirectory(file2) && !isDirectory(file1)) {
        return 1;
      }
    } else if (dirsOnTop == 1) {
      if (isDirectory(file1) && !isDirectory(file2)) {

        return 1;
      } else if (isDirectory(file2) && !isDirectory(file1)) {
        return -1;
      }
    }

    if (sort == 0) {

      // sort by name
      return asc * file1.title.compareToIgnoreCase(file2.title);
    } else if (sort == 1) {

      // sort by last modified
      return asc * Long.valueOf(file1.date).compareTo(file2.date);
    } else if (sort == 2) {

      // sort by size
      if (!file1.isDirectory && !file2.isDirectory) {

        return asc * Long.valueOf(file1.longSize).compareTo(file2.longSize);
      } else {

        return file1.title.compareToIgnoreCase(file2.title);
      }

    } else if (sort == 3) {

      // sort by type
      if (!file1.isDirectory && !file2.isDirectory) {

        final String ext_a = getExtension(file1.title);
        final String ext_b = getExtension(file2.title);

        final int res = asc * ext_a.compareTo(ext_b);
        if (res == 0) {
          return asc * file1.title.compareToIgnoreCase(file2.title);
        }
        return res;
      } else {
        return file1.title.compareToIgnoreCase(file2.title);
      }
    }
    return 0;
  }

  private static String getExtension(String a) {
    return a.substring(a.lastIndexOf(".") + 1).toLowerCase();
  }
}
