package com.amaze.filemanager.fragments;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.DatabaseViewerActivity;
import com.amaze.filemanager.asynchronous.asynctasks.DbViewerTask;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by Vishal on 06-02-2015.
 */
public class DbViewerFragment extends Fragment {
    public DatabaseViewerActivity databaseViewerActivity;
    private String tableName;
    private View rootView;
    private Cursor schemaCursor, contentCursor;
    private RelativeLayout relativeLayout;
    public TextView loadingText;
    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        databaseViewerActivity = (DatabaseViewerActivity) getActivity();

        rootView = inflater.inflate(R.layout.fragment_db_viewer, null);
        webView = rootView.findViewById(R.id.webView1);
        loadingText = rootView.findViewById(R.id.loadingText);
        relativeLayout = rootView.findViewById(R.id.tableLayout);
        tableName = getArguments().getString("table");
        databaseViewerActivity.setTitle(tableName);

        schemaCursor = databaseViewerActivity.sqLiteDatabase.rawQuery("PRAGMA table_info(" + tableName + ");", null);
        contentCursor = databaseViewerActivity.sqLiteDatabase.rawQuery("SELECT * FROM " + tableName, null);

        new DbViewerTask(schemaCursor, contentCursor, webView, this).execute();

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (databaseViewerActivity.getAppTheme().equals(AppTheme.DARK)) {
            relativeLayout.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
            webView.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
        } else if (databaseViewerActivity.getAppTheme().equals(AppTheme.BLACK)) {
            relativeLayout.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
            webView.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
        } else {
            relativeLayout.setBackgroundColor(Color.parseColor("#ffffff"));
            webView.setBackgroundColor(Color.parseColor("#ffffff"));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        schemaCursor.close();
        contentCursor.close();
    }
}
