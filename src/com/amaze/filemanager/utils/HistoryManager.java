package com.amaze.filemanager.utils;


import android.content.Context;
import android.database.Cursor;
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
        try {
            db.execSQL("DROP TABLE " + x);
        } catch (Exception e) {
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS " + x + " (PATH VARCHAR)");
    }

    public ArrayList<String> readTable() {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String> paths = new ArrayList<String>();
        do {
            paths.add(c.getString(c.getColumnIndex("PATH")));
        } while (c.moveToPrevious());
        return paths;
    }

    public void addPath(String path) {
        try {
            db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
        } catch (Exception e) {
        }
        db.execSQL("INSERT INTO " + table + " VALUES" + "('" + path + "');");
    }

    public void end() {
        db.close();
    }

    public void open() {
        db = c.openOrCreateDatabase(table, c.MODE_PRIVATE, null);
    }
}
