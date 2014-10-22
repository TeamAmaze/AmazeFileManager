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

package com.amaze.filemanager.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Comparator;

public class AppsSorter implements Comparator<ApplicationInfo> {
    PackageManager p;

    public AppsSorter(PackageManager p) {
        this.p = p;
    }

    public int compare(ApplicationInfo p1, ApplicationInfo p2) {
        return 1 * p1.loadLabel(p).toString().compareToIgnoreCase(p2.loadLabel(p).toString());
        // TODO: Implement this method

    }

}
