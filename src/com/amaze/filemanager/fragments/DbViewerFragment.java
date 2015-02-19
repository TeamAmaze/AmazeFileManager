package com.amaze.filemanager.fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.DbViewer;

import java.lang.reflect.Array;
import java.sql.Blob;
import java.util.ArrayList;

/**
 * Created by Vishal on 06-02-2015.
 */
public class DbViewerFragment extends Fragment {
    private DbViewer dbViewer;
    private String tableName;
    private View rootView;
    private Cursor schemaCursor, contentCursor;
    private ArrayList<String[]> contentList;
    private TableLayout tableLayout;
    private TableRow tableRow, tableRow1;
    private TextView textView;
    private ArrayList<String> schemaList;
    private RelativeLayout.LayoutParams matchParent;
    private RelativeLayout relativeLayout;
    private String blobString;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbViewer = (DbViewer) getActivity();

        rootView = inflater.inflate(R.layout.fragment_db_viewer, null);
        tableLayout = (TableLayout) rootView.findViewById(R.id.table);
        relativeLayout = (RelativeLayout) rootView.findViewById(R.id.tableLayout);
        tableName = getArguments().getString("table");
        dbViewer.setTitle(tableName);
        matchParent = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        schemaCursor = dbViewer.sqLiteDatabase.rawQuery("PRAGMA table_info(" + tableName + ");", null);
        contentCursor = dbViewer.sqLiteDatabase.rawQuery("SELECT * FROM " + tableName, null);
        schemaList = getDbTableSchema(schemaCursor);
        contentList = getDbTableDetails(contentCursor);

        tableRow = new TableRow(getActivity());
        tableRow.setLayoutParams(matchParent);
        tableRow.setBackgroundColor(Color.parseColor("#989898"));
        for (String s : schemaList) {
            Log.d("table schema is - ", s);
            textView = new TextView(getActivity());
            textView.setText(s);
            tableRow.addView(textView);
        }
        tableLayout.addView(tableRow, matchParent);

        int j = 0;
        for (String[] strings : contentList) {
          //  Log.d("column number", "  " + j++);
            tableRow1 = new TableRow(getActivity());
            tableRow1.setLayoutParams(matchParent);
            for (int i=0; i<strings.length; i++) {
                //Log.d("column data", strings[i]);
                textView = new TextView(getActivity());
                textView.setText(strings[i]);

                tableRow1.addView(textView);
            }
            tableLayout.addView(tableRow1, matchParent);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (dbViewer.theme1 == 1)
            relativeLayout.setBackgroundColor(Color.parseColor("#000000"));
        else
            relativeLayout.setBackgroundColor(Color.parseColor("#ffffff"));

    }

    @Override
    public void onDetach() {
        super.onDetach();
        schemaCursor.close();
        contentCursor.close();
    }

    private ArrayList<String[]> getDbTableDetails(Cursor c) {
        ArrayList<String[]> result = new ArrayList<String[]>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String[] temp = new String[c.getColumnCount()];
            for (int i = 0; i < temp.length; i++) {
                int dataType = c.getType(i);
                switch (dataType) {
                    case 0:
                        // #FIELD_TYPE_NULL
                        temp[i] = null;
                        break;
                    case 1:
                        // #FIELD_TYPE_INTEGER
                        temp[i] = String.valueOf(c.getInt(i));
                        break;
                    case 2:
                        // #FIELD_TYPE_FLOAT
                        temp[i] = String.valueOf(c.getFloat(i));
                        break;
                    case 3:
                        // #FIELD_TYPE_STRING
                        temp[i] = c.getString(i);
                        break;
                    case 4:
                        // #FIELD_TYPE_BLOB
                        /*byte[] blob = c.getBlob(i);
                        blobString = new String(blob);*/
                        temp[i] = "(BLOB)";
                        break;
                }
            }
            result.add(temp);
        }
        return result;
    }
    private ArrayList<String> getDbTableSchema(Cursor c) {
        ArrayList<String> result = new ArrayList<String>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            result.add(c.getString(1));
        }
        return result;
    }
}
