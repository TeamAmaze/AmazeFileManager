/*
 * SortHandler.java
 *
 * Copyright (C) 2018-2020 ning <ning.xyw@gmail.com>, Emmanuel Messulam<emmanuelbendavid@gmail.com>,
 * Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

import com.amaze.filemanager.database.models.explorer.Sort;

import java.util.HashSet;
import java.util.Set;

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS;

/**
 * Created by Ning on 5/28/2018.
 */

public class SortHandler {

    private final ExplorerDatabase database;

    public static int getSortType(Context context, String path) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> onlyThisFloders = sharedPref.getStringSet(PREFERENCE_SORTBY_ONLY_THIS, new HashSet<>());
        final boolean onlyThis = onlyThisFloders.contains(path);
        final int globalSortby = Integer.parseInt(sharedPref.getString("sortby", "0"));
        if (!onlyThis) {
            return globalSortby;
        }
        SortHandler sortHandler = new SortHandler();
        Sort sort = sortHandler.findEntry(path);
        if (sort == null) {
            return globalSortby;
        }
        return sort.type;
    }

    public SortHandler() {
        database = ExplorerDatabase.getInstance();
    }

    public void addEntry(Sort sort) {
        database.sortDao().insert(sort);
    }

    public void clear(String path) {
        database.sortDao().clear(database.sortDao().find(path));
    }

    public void updateEntry(Sort oldSort, Sort newSort) {
        database.sortDao().update(newSort);
    }

    @Nullable
    public Sort findEntry(String path) {
        return database.sortDao().find(path);
    }
}
