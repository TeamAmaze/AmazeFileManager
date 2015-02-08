package com.amaze.filemanager.fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.DbViewer;

import java.util.ArrayList;

/**
 * Created by Vishal on 06-02-2015.
 */
public class DbViewerFragment extends Fragment {
    private DbViewer dbViewer;
    private String tableName;
    private View rootView;
    private Cursor schemaCursor, contentCursor;
    private ArrayList<String> schemaList;
    private ArrayList<String[]> contentList;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_db_viewer, null);

        dbViewer = (DbViewer) getActivity();
        tableName = getArguments().getString("table");
        Toast.makeText(dbViewer, tableName, Toast.LENGTH_LONG).show();
        dbViewer.setTitle(tableName);

        schemaCursor = dbViewer.sqLiteDatabase.rawQuery("PRAGMA table_info(" + tableName + ");", null);
        schemaList = getDbTableSchema(schemaCursor);
        for (String s : schemaList) {
            Log.d("table schema", s);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public ArrayList<String[]> getDbTableDetails(Cursor c) {
        ArrayList<String[]> result = new ArrayList<String[]>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String[] temp = new String[c.getCount()];
            for (int i = 0; i < temp.length; i++) {
                //Log.d("table content extra", c.getString(i));
                temp[i] = c.getString(i);
            }
            result.add(temp);
        }
        return result;
    }
    private ArrayList<String> getDbTableSchema(Cursor c) {
        ArrayList<String> result = new ArrayList<String>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

            for (int i = 0; i<c.getCount(); i++) {
                result.add(c.getString(i));
            }
        }
        return result;
    }
}
