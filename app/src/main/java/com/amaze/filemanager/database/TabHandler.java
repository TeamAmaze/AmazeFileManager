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

package com.amaze.filemanager.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amaze.filemanager.database.models.explorer.Tab;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Vishal on 9/17/2014.
 */
public class TabHandler {

    private final ExplorerDatabase database;

    public TabHandler() {
        database = ExplorerDatabase.getInstance();
    }

    public void addTab(@NonNull Tab tab) {
        database.tabDao().insertTab(tab);
    }

    public void clear() {
        database.tabDao().clear();
    }

    @Nullable
    public Tab findTab(int tabNo) {
        return database.tabDao().find(tabNo);
    }

    public List<Tab> getAllTabs() {
        return Arrays.asList(database.tabDao().list());
    }

    public void close() {
        database.close();
    }
}
