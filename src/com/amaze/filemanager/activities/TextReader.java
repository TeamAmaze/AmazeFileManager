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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TextReader extends Activity {
    EditText ma;
    String path;
    ProgressBar p;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        ma = (EditText) findViewById(R.id.fname);
        p = (ProgressBar) findViewById(R.id.pbar);
        ma.setVisibility(View.GONE);
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
        path = this.getIntent().getStringExtra("path");
        if (path != null) {
            Toast.makeText(this, "" + path, Toast.LENGTH_SHORT).show();
            new LoadText().execute(path);
        } else {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
            finish();
        }
        getActionBar().setTitle(new File(path).getName());
        getActionBar().setSubtitle(path);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text, menu);
        return true;
    }

    boolean save;

    @Override
    public boolean onPrepareOptionsMenu(Menu m) {

        return super.onPrepareOptionsMenu(m);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.save:
                writeTextFile(path, ma.getText().toString());
                return true;
        }
        return super.onOptionsItemSelected(menu);
    }

    public String readTextFile(String fileName) {
        String returnValue = "";
        FileReader file = null;
        String line = "";
        try {
            file = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(file);

            while ((line = reader.readLine()) != null) {
                returnValue += line + "\n";

            }
            reader.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
            finish();
        } catch (IOException e) {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
            finish();
        } finally {
            if (file != null) {
                try {
                    file.close();

                } catch (IOException e) {
                    Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show();
                    finish();
                    e.printStackTrace();
                }
            }
        }

        return returnValue;
    }

    public void writeTextFile(String fileName, String s) {
        File f = new File(fileName);

        FileWriter output = null;
        try {
            output = new FileWriter(fileName);
            BufferedWriter writer = new BufferedWriter(output);
            writer.write(s);
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
        }


    }

    class LoadText extends AsyncTask<String, String, String> {
        public void onpreExecute() {
            ma.setVisibility(View.GONE);
            p.setVisibility(View.VISIBLE);
        }

        boolean editable = true;

        public String doInBackground(String... p) {
            String returnValue = "";
            if (new File(p[0]).canWrite()) editable = true;
            else editable = false;
            FileReader file = null;
            String line = "";
            try {
                file = new FileReader(p[0]);
                BufferedReader reader = new BufferedReader(file);

                while ((line = reader.readLine()) != null) {
                    returnValue += line + "\n";
                }
                reader.close();
            } catch (FileNotFoundException e) {
                //	Toast.makeText(this,"Could not read file",Toast.LENGTH_LONG).show();finish();
            } catch (IOException e) {
                //	Toast.makeText(this,"Could not read file",Toast.LENGTH_LONG).show();finish();
            } finally {
                if (file != null) {
                    try {
                        file.close();

                    } catch (IOException e) {
                        //Toast.makeText(this,"Could not read file",Toast.LENGTH_LONG).show();finish();
                        e.printStackTrace();
                    }
                }
            }
            return returnValue;
        }

        @Override
        public void onPostExecute(String s) {
            p.setVisibility(View.GONE);
            ma.setFocusable(editable);
            save = editable;
            TextReader.this.invalidateOptionsMenu();
            ma.setVisibility(View.VISIBLE);
            ma.setText(s);
        }
    }

}
