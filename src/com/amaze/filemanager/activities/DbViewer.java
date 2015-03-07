package com.amaze.filemanager.activities;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.DbViewerFragment;
import com.amaze.filemanager.utils.RootHelper;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by Vishal on 02-02-2015.
 */
public class DbViewer extends ActionBarActivity {

    private SharedPreferences Sp;
    private String skin, path;
    private boolean rootMode;
    private ListView listView;
    private ArrayList<String> arrayList;
    private ArrayAdapter arrayAdapter;
    private Cursor c;
    private File pathFile;
    boolean delete=false;
    public Toolbar toolbar;
    public SQLiteDatabase sqLiteDatabase;
    public int theme, theme1, skinStatusBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        theme = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme;
        if (theme == 2) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour <= 6 || hour >= 18) {
                theme1 = 1;
            } else
                theme1 = 0;
        }
        if (theme1 == 1) {
            setTheme(R.style.appCompatDark);
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        }
        setContentView(R.layout.activity_db_viewer);
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        skin = Sp.getString("skin_color", "#03A9F4");
        String x = getStatusColor();
        skinStatusBar = Color.parseColor(x);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("rootmode", false);
        int sdk= Build.VERSION.SDK_INT;
        if(sdk==20 || sdk==19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.texteditor).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        }else if(Build.VERSION.SDK_INT>=21){
            boolean colourednavigation=Sp.getBoolean("colorednavigation",true);
            Window window =getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.parseColor(getStatusColor()));
            if(colourednavigation)
                window.setNavigationBarColor(Color.parseColor(getStatusColor()));

        }

        path = getIntent().getStringExtra("path");
        pathFile = new File(path);
        listView = (ListView) findViewById(R.id.listView);

        load(pathFile);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                DbViewerFragment fragment = new DbViewerFragment();
                Bundle bundle = new Bundle();
                bundle.putString("table", arrayList.get(position));
                fragment.setArguments(bundle);
                fragmentTransaction.add(R.id.content_frame, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

    }

    private ArrayList<String> getDbTableNames(Cursor c) {
        ArrayList<String> result = new ArrayList<String>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            for (int i = 0; i < c.getColumnCount(); i++) {
                result.add(c.getString(i));
            }
        }
        return result;
    }
    private void load(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!file.canRead() && rootMode) {
                    File file1=getExternalCacheDir();
                    if(file1!=null)file1=getCacheDir();
                    RootTools.remount(file.getParent(), "RW");
                    RootTools.copyFile(pathFile.getPath(),new File(file1.getPath(),file.getName()).getPath(), true,false);
                    pathFile=new File(file1.getPath(),file.getName());
                    RootHelper.runAndWait("chmod 777 "+pathFile.getPath(),true);
                    delete=true;
                }
                try {
                    sqLiteDatabase = SQLiteDatabase.openDatabase(pathFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);

                    c = sqLiteDatabase.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table'", null);
                    arrayList = getDbTableNames(c);
                    arrayAdapter = new ArrayAdapter(DbViewer.this, android.R.layout.simple_list_item_1, arrayList);
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        listView.setAdapter(arrayAdapter);
                    }
                });
            }
        }).start();
    }

    private String getStatusColor() {


        String[] colors = new String[]{
                "#F44336","#D32F2F",
                "#e91e63","#C2185B",
                "#9c27b0","#7B1FA2",
                "#673ab7","#512DA8",
                "#3f51b5","#303F9F",
                "#2196F3","#1976D2",
                "#03A9F4","#0288D1",
                "#00BCD4","#0097A7",
                "#009688","#00796B",
                "#4CAF50","#388E3C",
                "#8bc34a","#689F38",
                "#FFC107","#FFA000",
                "#FF9800","#F57C00",
                "#FF5722","#E64A19",
                "#795548","#5D4037",
                "#212121","#000000",
                "#607d8b","#455A64",
                "#004d40","#002620"
        };
        return colors[ Arrays.asList(colors).indexOf(skin)+1];
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       if(sqLiteDatabase!=null) sqLiteDatabase.close();
        if(c!=null) c.close();
        if(true)pathFile.delete();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        toolbar.setTitle(pathFile.getName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            toolbar.setTitle(pathFile.getName());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
