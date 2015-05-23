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

package com.amaze.filemanager.activities;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.RootHelper;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.stericson.RootTools.RootTools;

import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TextReader extends AppCompatActivity implements TextWatcher {

    String path;
    Futils utils = new Futils();
    Context c = this;
    boolean rootMode;
    int theme, theme1;
    SharedPreferences Sp;

    private EditText mInput;
    private java.io.File mFile;
    private String mOriginal, skin;
    private Timer mTimer;
    private boolean mModified;
    private int skinStatusBar;
    private String fabSkin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        fabSkin = Sp.getString("fab_skin_color", "#e91e63");

        theme = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;
        if (theme1 == 1) {
            setTheme(R.style.appCompatDark);
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        setContentView(R.layout.search);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        skin = Sp.getString("skin_color", "#3f51b5");
        if (Build.VERSION.SDK_INT>=21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze", ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_launcher)).getBitmap(), Color.parseColor(skin));
            ((Activity)this).setTaskDescription(taskDescription);
        }
        String x = PreferenceUtils.getStatusColor(skin);
        skinStatusBar = Color.parseColor(x);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootMode = PreferenceManager.getDefaultSharedPreferences(c)
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
            window.setStatusBarColor(Color.parseColor(PreferenceUtils.getStatusColor(skin)));
            if(colourednavigation)
                window.setNavigationBarColor(Color.parseColor(PreferenceUtils.getStatusColor(skin)));

        }
        mInput = (EditText) findViewById(R.id.fname);
        mInput.addTextChangedListener(this);
        try {
            if (theme1 == 1) mInput.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {

        }

        try {
            if (getIntent().getData() != null) load(new File(getIntent().getData().getPath()));
            else load(new File(getIntent().getStringExtra("path")));
        } catch (Exception e) {

        }
    }

    private void checkUnsavedChanges() {
        if (mOriginal != null && !mOriginal.equals(mInput.getText().toString())) {
            new MaterialDialog.Builder(this)
                    .title(R.string.unsavedchanges)
                    .content(R.string.unsavedchangesdesc)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .positiveColor(Color.parseColor(fabSkin))
                    .negativeColor(Color.parseColor(fabSkin))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            writeTextFile(mFile.getPath(), mInput.getText().toString());
                            finish();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }
                    })
                    .build().show();
        } else {
            finish();
        }
    }


    File f;

    public void writeTextFile(String fileName, String s) {
        f = new File(fileName);
        mOriginal = s;
        final String s1 = s;
        if (!mFile.canWrite()) {
            f = new File(this.getFilesDir() + "/" + f.getName());
        }
        Toast.makeText(c, R.string.saving, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileWriter output = null;
                try {
                    output = new FileWriter(f.getPath());
                    BufferedWriter writer = new BufferedWriter(output);
                    writer.write(s1);
                    writer.close();
                    output.close();
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(c, "error", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();


                }
                if (!mFile.canWrite())

                {
                    RootTools.remount(mFile.getParent(),"rw");
                    RootHelper.runAndWait("cat "+f.getPath()+" > "+mFile.getPath(),true);
                    f.delete();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(c, "Done", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }


    private void setProgress(boolean show) {
        //mInput.setVisibility(show ? View.GONE : View.VISIBLE);
        //   findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void load(final File mFile) {
        setProgress(true);
        this.mFile = mFile;
        mInput.setHint("Loading...");
        new Thread(new Runnable() {
            @Override
            public void run() {


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(mFile.getName());
                    }
                });
                try {
                    if(mFile.canRead())
                    {try {
                        mOriginal = FileUtils.fileRead(mFile);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mInput.setHint(R.string.error);
                            }
                        });
                    }}else{
                        mOriginal="";
                    ArrayList<String> arrayList=    RootHelper.runAndWait1("cat "+mFile.getPath(),true);
                        for(String x:arrayList){
                            if(mOriginal.equals(""))mOriginal=x;
                            else mOriginal=mOriginal+"\n"+x;

                        }


                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mInput.setText(mOriginal);
                            } catch (OutOfMemoryError e) {
                                mInput.setHint(R.string.error);
                            }
                            setProgress(false);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mInput.setHint(R.string.error);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        checkUnsavedChanges();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text, menu);
        menu.findItem(R.id.save).setVisible(mModified);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            checkUnsavedChanges();
            return true;
        } else if (item.getItemId() == R.id.save) {
            writeTextFile(mFile.getPath(), mInput.getText().toString());
            return true;
        } else if (item.getItemId() == R.id.details) {
            utils.showProps(mFile, c, theme1);
            return true;
        } else if (item.getItemId() == R.id.openwith) {
            utils.openunknown(mFile, c, false);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mModified = !mInput.getText().toString().equals(mOriginal);
                invalidateOptionsMenu();
            }
        }, 250);
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }
}
