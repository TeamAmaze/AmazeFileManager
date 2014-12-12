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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.services.asynctasks.MoveFiles;
import com.amaze.filemanager.utils.Futils;
import com.readystatesoftware.systembartint.SystemBarTintManager;

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
import java.util.Timer;
import java.util.TimerTask;

public class TextReader extends Activity {
    EditText ma;
    String path;
    ProgressBar p;
    Futils utils=new Futils();
    Context c=this;
    File file;
    boolean rootMode;
    boolean mModified=false;
    String mOriginal="";
    private Timer mTimer;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        ma = (EditText) findViewById(R.id.fname);
        p = (ProgressBar) findViewById(R.id.pbar);
        ma.setVisibility(View.VISIBLE);
        String skin = PreferenceManager.getDefaultSharedPreferences(this).getString("skin_color", "#5677fc");
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        if(Build.VERSION.SDK_INT>=19){
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            FrameLayout a=(FrameLayout)ma.getParent();
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) a.getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0,config.getPixelInsetTop(true),0,0);
        }
        rootMode = PreferenceManager.getDefaultSharedPreferences(c)
        .getBoolean("rootmode", false);
        path = this.getIntent().getStringExtra("path");
        if (path != null) {
            file=new File(path);
            //Toast.makeText(this, "" + path, Toast.LENGTH_SHORT).show();
            new LoadText().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
        } else {
            Toast.makeText(this,utils.getString(this,R.string.cant_read_file) , Toast.LENGTH_LONG).show();
            finish();
        }
        ma.addTextChangedListener(t);
        getActionBar().setTitle(file.getName());
        getActionBar().setSubtitle(path);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        if(file!=null)if(!file.canWrite() && !rootMode){

            m.findItem(R.id.save).setVisible(false);
        }else if(mModified)
            m.findItem(R.id.save).setVisible(true);
        else m.findItem(R.id.save).setVisible(false);
        return super.onPrepareOptionsMenu(m);
    }
TextWatcher t=new TextWatcher() {
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
                mModified = !ma.getText().toString().equals(mOriginal);
                invalidateOptionsMenu();
            }
        }, 250);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
};   private void checkUnsavedChanges() {
        if (mOriginal != null && !mOriginal.equals(ma.getText().toString())) {
            new MaterialDialog.Builder(this)
                    .title(R.string.unsavedchanges)
                    .content(R.string.unsavedchangesdesc)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .callback(new MaterialDialog.Callback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            writeTextFile(file.getPath(),ma.getText().toString());
                            finish();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }
                    })
                    .build().show();
        }
        else { finish(); }

    }
    @Override
    public void onBackPressed(){   if(file!=null)if(file.canWrite() || rootMode){
        checkUnsavedChanges();}else finish();}
    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.save:
                writeTextFile(path, ma.getText().toString());
                return true;
        }
        return super.onOptionsItemSelected(menu);
    }
    File f;
    public void writeTextFile(String fileName, String s) {
       f = new File(fileName);
        mOriginal=s;
       final String s1=s;
        if(!file.canWrite()){f=new File(this.getFilesDir()+"/"+f.getName());}
        Toast.makeText(c,R.string.saving,Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileWriter output = null;
                try {
                    output = new FileWriter(f.getPath());
                    BufferedWriter writer = new BufferedWriter(output);
                    writer.write(s1);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

            }if(!file.canWrite())

            {
                ArrayList<File> a = new ArrayList<File>();
                a.add(f);
                new MoveFiles(a, null, c).execute(file.getParent());
            }}
            }).start();
    }

    class LoadText extends AsyncTask<String, String, String> {
        @Override
        public void onPreExecute() {
            ma.setVisibility(View.GONE);
            p.setVisibility(View.VISIBLE);
        }

        @Override
        public void onProgressUpdate(String... x){
Toast.makeText(c,R.string.cant_read_file,Toast.LENGTH_SHORT).show();    }
        public String doInBackground(String... p) {
            String returnValue = "";
            FileReader file = null;
            String line = "";
            try {
                file = new FileReader(p[0]);
                BufferedReader reader = new BufferedReader(file);

                while ((line = reader.readLine()) != null) {
                    returnValue += line+"\n" ;
              }
                reader.close();
            } catch (FileNotFoundException e) {
              publishProgress("");
            } catch (IOException e) {
              publishProgress("");
            } finally {
                if (file != null) {
                    try {
                        file.close();

                    } catch (IOException e) {

                      publishProgress("");
                        e.printStackTrace();
                    }
                }
            }
            return returnValue;
        }

        @Override
        public void onPostExecute(String s) {
            p.setVisibility(View.GONE);
            mOriginal=s;
            TextReader.this.invalidateOptionsMenu();
            ma.setVisibility(View.VISIBLE);
           ma.setText(s);
        }
    }}
