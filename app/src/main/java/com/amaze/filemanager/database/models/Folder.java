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

/**
 * Created by llrraa on 10/28/2018.
 */
public class Folder {
    public final String path;
    public final long size;
    public final long time;


    public Folder(String path, long size, long time) {
        this.path = path;
        this.size = size;
        this.time = time;
    }

}
