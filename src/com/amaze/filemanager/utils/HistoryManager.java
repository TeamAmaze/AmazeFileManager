package com.amaze.filemanager.utils;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

import java.util.ArrayList;

public class HistoryManager {
    SQLiteDatabase db;
    String table;
    public HistoryManager(Context c,String x){

        db = c.openOrCreateDatabase(x, c.MODE_PRIVATE, null);
        table=x;
        db.execSQL("DROP TABLE "+x);
          db.execSQL("CREATE TABLE IF NOT EXISTS "+x+" (PATH VARCHAR)");
    }

    public ArrayList<String> readTable() {
      Cursor c= db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String> paths=new ArrayList<String>();
        do {
            paths.add(c.getString(c.getColumnIndex("PATH")));
        }while (c.moveToPrevious());
        return  paths;
    }
    public void addPath(String path){
        db.execSQL("INSERT INTO "+table+" VALUES"+"('"+path+"');");
}}
