/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.utils.files;

import com.amaze.filemanager.adapters.data.LayoutElementParcelable;

import java.util.Comparator;

public class FileListSorter implements Comparator<LayoutElementParcelable> {

    private int directoryOnTop = 0;
    private int ascending = 1;
    private int sort = 0;

    public FileListSorter(int dir, int sort, int ascending) {
        this.directoryOnTop = dir;
        this.ascending = ascending;
        this.sort = sort;
    }

    private boolean isDirectory(LayoutElementParcelable path) {
        return path.isDirectory;
    }

    /* line arrangement - compare method : delete comments
    The comment in the method was deleted because it was the code commenting part that was before.
     */

    /* extract method - equals method : change condition statement
    I extracted the function for each conditional statement. (Improved understandability)
     */

    /**
     * Compares two elements and return negative, zero and positive integer if first argument is
     * less than, equal to or greater than second
     * @param file1
     * @param file2
     * @return
     */
    @Override
    public int compare(LayoutElementParcelable file1, LayoutElementParcelable file2) {

        if (directoryLocationIsBottom()) {
            if (onlyFirstFileIsDirectory(file1, file2)) {
                return -1;
            } else if (onlySecondFileIsDirectory(file1, file2)) {
                return 1;
            }
        } else if (directoryLocationIsTop()) {
            if (onlyFirstFileIsDirectory(file1, file2)) {
                return 1;
            } else if (onlySecondFileIsDirectory(file1, file2)) {
                return -1;
            }
        }

        if(sortNumberInSortRange()) {
            return eachSortNumberReturnValue(file1, file2);
        }

        return 0;

    }

    private boolean directoryLocationIsBottom() {
        return directoryOnTop == 0;
    }

    private boolean directoryLocationIsTop() {
        return directoryOnTop == 1;
    }
    private boolean onlyFirstFileIsDirectory(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        return isDirectory(file1) && !isDirectory(file2);
    }

    private boolean onlySecondFileIsDirectory(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        return isDirectory(file2) && !isDirectory(file1);
    }

    private boolean twoFilesAreNotDirectory(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        return !isDirectory(file1) && !isDirectory(file2);
    }

    private boolean isSortByFileName() {
        return sort == 0;
    }

    private int SortByFileName(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        return ascending * compareFileTitle(file1, file2);
    }

    private int compareFileTitle(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        return file1.title.compareToIgnoreCase(file2.title);
    }

    private boolean isSortByFileLastModifiedDate() {
        return sort == 1;
    }

    private int SortByFileLastModifiedDate(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        return ascending * Long.valueOf(file1.date).compareTo(file2.date);
    }

    private boolean isSortByFileSize() {
        return sort == 2;
    }

    private int SortByFileSize(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        if (twoFilesAreNotDirectory(file1, file2)) {
            return ascending * Long.valueOf(file1.longSize).compareTo(file2.longSize);
        } else {
            return compareFileTitle(file1, file2);
        }
    }

    private boolean isSortByFileExtensionName() {
        return sort == 3;
    }

    private int resIsZeroOrNotEachReturn(final int res, LayoutElementParcelable file1, LayoutElementParcelable file2) {
        if (res == 0) {
            return ascending * compareFileTitle(file1, file2);
        }
        return res;
    }
    private int SortByFileExtensionName(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        if(twoFilesAreNotDirectory(file1, file2)) {

            final String ext_file1 = getExtension(file1.title);
            final String ext_file2 = getExtension(file2.title);

            final int res = ascending*ext_file1.compareTo(ext_file2);

            return resIsZeroOrNotEachReturn(res, file1, file2);
        } else {
            return compareFileTitle(file1, file2);
        }
    }
    private boolean sortNumberInSortRange() {
        return isSortByFileName() || isSortByFileLastModifiedDate() || isSortByFileSize() || isSortByFileExtensionName();
    }

    private int eachSortNumberReturnValue(LayoutElementParcelable file1, LayoutElementParcelable file2) {
        int sortResultNumber = 0;

        if (isSortByFileName()) {
            sortResultNumber = SortByFileName(file1, file2);
        } else if (isSortByFileLastModifiedDate()) {
            sortResultNumber = SortByFileLastModifiedDate(file1, file2);
        } else if (isSortByFileSize()) {
            sortResultNumber = SortByFileSize(file1, file2);
        } else if(isSortByFileExtensionName()) {
            sortResultNumber = SortByFileExtensionName(file1, file2);
        }

        return sortResultNumber;
    }

    private static String getExtension(String a) {
        return a.substring(a.lastIndexOf(".") + 1).toLowerCase();
    }

}
