/*
 * Copyright (C) 2018 Emmanuel Messulam <emmanuelbendavid@gmail.com>
 * Copyright (C) 2014 Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.database.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Ning on 5/28/2018.
 */
public class Sort {
    public static final int SORT_TYPE_NAME = 0;
    public static final int SORT_TYPE_LAST_MODIFIED = 1;
    public static final int SORT_TYPE_SIZE = 2;
    public static final int SORT_TYPE_TYPE = 3;
    public static final int SORT_TYPE_NAME_DESC = 4;
    public static final int SORT_TYPE_LAST_MODIFIED_DESC = 5;
    public static final int SORT_TYPE_SIZE_DESC = 6;
    public static final int SORT_TYPE_TYPE_DESC = 7;

    @IntDef({
            SORT_TYPE_NAME,
            SORT_TYPE_LAST_MODIFIED,
            SORT_TYPE_SIZE,
            SORT_TYPE_TYPE,
            SORT_TYPE_NAME_DESC,
            SORT_TYPE_LAST_MODIFIED_DESC,
            SORT_TYPE_SIZE_DESC,
            SORT_TYPE_TYPE_DESC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TYPE {
    }

    private int _id;
    public String path;
    @TYPE
    public int sortType;

    public Sort() {
    }

    public Sort(String path, @TYPE int sortType) {
        this.path = path;
        this.sortType = sortType;
    }

    public int getId() {
        return _id;
    }

    public void setId(int _id) {
        this._id = _id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getSortType() {
        return sortType;
    }

    public void setSortType(@TYPE int sortType) {
        this.sortType = sortType;
    }

}
