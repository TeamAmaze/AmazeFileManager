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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.asynctasks.MoveFiles;
import com.amaze.filemanager.utils.Futils;

import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
public class TextReader extends ActionBarActivity implements TextWatcher {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        setContentView(R.layout.search);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        skin = Sp.getString("skin_color", "#03A9F4");
        String x = getStatusColor();
        skinStatusBar = Color.parseColor(x);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootMode = PreferenceManager.getDefaultSharedPreferences(c)
                .getBoolean("rootmode", false);

        // status bar
        if (Build.VERSION.SDK_INT >= 21) {

            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(skinStatusBar);
        }
        mInput = (EditText) findViewById(R.id.fname);
        mInput.addTextChangedListener(this);
        if (theme1 == 1) mInput.setBackgroundColor(Color.parseColor("#000000"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent().getData() != null) load(new File(getIntent().getData().getPath()));
        else load(new File(getIntent().getStringExtra("path")));
    }

    private void checkUnsavedChanges() {
        if (mOriginal != null && !mOriginal.equals(mInput.getText().toString())) {
            new MaterialDialog.Builder(this)
                    .title(R.string.unsavedchanges)
                    .content(R.string.unsavedchangesdesc)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .callback(new MaterialDialog.Callback() {
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
                    ArrayList<File> a = new ArrayList<File>();
                    a.add(f);
                    new MoveFiles(a, null, c).execute(mFile.getParent());
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
        Log.v("TextEditor", "Loading...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mFile.exists()) {
                    Log.v("TextEditor", "File doesn't exist...");
                    finish();
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(mFile.getName());
                    }
                });
                try {
                    Log.v("TextEditor", "Reading file...");
                    try {
                        mOriginal = FileUtils.fileRead(mFile);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mInput.setHint(R.string.error);
                            }
                        });
                    }
                    Log.v("TextEditor", "Setting contents to input area...");
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
                    Log.v("TextEditor", "Error: " + e.getMessage());
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
            utils.openunknown(mFile, c);
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
}