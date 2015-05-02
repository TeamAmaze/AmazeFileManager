/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>
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


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class HistoryManager {
    SQLiteDatabase db;
    String table;
    Context c;

    public HistoryManager(Context c, String x) {
        this.c = c;
        table = x;
        open();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + x + " (PATH VARCHAR)");
    }

    public ArrayList<String> readTable() {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String> paths = new ArrayList<String>();
        do {
            try {
                paths.add(c.getString(c.getColumnIndex("PATH")));
            } catch (Exception e) {
     //           e.printStackTrace();
            }
        } while (c.moveToPrevious());
        return paths;
    }

    public void addPath(String path) {
        try {
            try {
                db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
            } catch (Exception e) {
            }
            db.execSQL("INSERT INTO " + table + " VALUES" + "('" + path + "');");
        } catch (Exception e) {
            open();
            try {
                db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
                db.execSQL("INSERT INTO " + table + " VALUES" + "('" + path + "');");
            } catch (Exception f) {
            }e.printStackTrace();
        }
    }
    public void removePath(String path){
        try {
            db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
        } catch (Exception e) {
        }
    }
    public void end() {
        db.close();
    }

    public void open() {
        db = c.openOrCreateDatabase(table, c.MODE_PRIVATE, null);
    }
}
