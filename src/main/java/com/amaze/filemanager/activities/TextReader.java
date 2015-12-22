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
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MapEntry;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

public class TextReader extends AppCompatActivity implements TextWatcher, View.OnClickListener {

    String path;
    Futils utils = new Futils();
    Context c = this;
    boolean rootMode;
    int theme, theme1;
    SharedPreferences Sp;

    private EditText mInput, searchEditText;
    private java.io.File mFile;
    private String mOriginal, skin;
    private Timer mTimer;
    private boolean mModified, isEditAllowed = true;
    private int skinStatusBar;
    private String fabSkin;
    private WebView webView;
    private android.support.v7.widget.Toolbar toolbar;

    // hashMap to store search text indexes
    //private LinkedHashMap<Integer, Integer> hashMap;
    private ArrayList<MapEntry>  nodes;
    private ListIterator it;

    private ImageButton upButton, downButton, closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        fabSkin =PreferenceUtils.getFabColor(Sp.getInt("fab_skin_color_position", 1));

        theme = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;

        nodes = new ArrayList<>();
        it = nodes.listIterator();

        // setting accent theme
        if (Build.VERSION.SDK_INT >= 21) {

            switch (fabSkin) {
                case "#F44336":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#e91e63":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9c27b0":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673ab7":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3f51b5":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8bc34a":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607d8b":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004d40":
                    if (theme1==0)
                        setTheme(R.style.pref_accent_light_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme1==1) {
                setTheme(R.style.appCompatDark);
            } else {
                setTheme(R.style.appCompatLight);
            }
        }

        if (theme1 == 1) {
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        setContentView(R.layout.search);
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        skin = PreferenceUtils.getSkinColor(Sp.getInt("skin_color_position", 4));
        if (Build.VERSION.SDK_INT>=21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze", ((BitmapDrawable)getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(), Color.parseColor(skin));
            ((Activity)this).setTaskDescription(taskDescription);
        }
        skinStatusBar = PreferenceUtils.getStatusColor(skin);
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
            window.setStatusBarColor((PreferenceUtils.getStatusColor(skin)));
            if(colourednavigation)
                window.setNavigationBarColor((PreferenceUtils.getStatusColor(skin)));

        }
        mInput = (EditText) findViewById(R.id.fname);
        webView = (WebView) findViewById(R.id.webView);

        try {
            if (getIntent().getData() != null)
                mFile = new File(getIntent().getData().getPath());
            else
                mFile = new File(getIntent().getStringExtra("path"));
        } catch (Exception e) {
            mFile = null;
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultTextEncodingName("utf-8");

        if (mFile!=null)
            webView.loadUrl("file://" + mFile.getAbsolutePath());
        else
            webView.loadData(getResources().getString(R.string.error), "text/html", null);

        mInput.addTextChangedListener(this);
        try {
            if (theme1 == 1) mInput.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {

        }

        setTitle(mFile.getName());
    }

    private void checkUnsavedChanges() {
        if (mOriginal != null && mInput.isShown() && !mOriginal.equals(mInput.getText().toString())) {
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
                            Toast.makeText(c, R.string.error, Toast.LENGTH_SHORT).show();
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
        mInput.setHint(R.string.loading);
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (mFile.canRead()) {
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
                    } else {
                        mOriginal = "";
                        ArrayList<String> arrayList = RootHelper
                                .runAndWait1("cat " + mFile.getPath(), true);
                        for (String x : arrayList) {
                            if (mOriginal.equals("")) mOriginal = x;
                            else mOriginal = mOriginal + "\n" + x;

                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mInput.setText(mOriginal);
                                if (mOriginal.isEmpty())
                                    mInput.setHint(R.string.file_empty);
                                else
                                    mInput.setHint(null);
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
        menu.findItem(R.id.edit).setVisible(isEditAllowed);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                checkUnsavedChanges();
                break;
            case R.id.save:
                // Make sure EditText is visible before saving!
                writeTextFile(mFile.getPath(), mInput.getText().toString());
                break;
            case R.id.details:
                utils.showProps(mFile, this, theme1);
                break;
            case R.id.openwith:
                utils.openunknown(mFile, c, false);
                break;
            case R.id.edit:
                webView.setVisibility(View.GONE);
                mInput.setVisibility(View.VISIBLE);
                isEditAllowed = false;
                load(mFile);
                invalidateOptionsMenu();
                break;
            case R.id.find:
                toolbar.startActionMode(mActionModeCallback);
                break;
            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        if (charSequence.hashCode()==mInput.getText().hashCode()) {
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
    }

    @Override
    public void afterTextChanged(Editable editable) {

        // searchBox callback block
        if (searchEditText!=null && editable.hashCode()==searchEditText.getText().hashCode()) {

            // clearing before adding new values
            while (it.hasNext()){
                it.next();
                it.remove();
                System.out.println("clearing");
            }

            for (int i = 0; i<(mOriginal.length()-editable.length()); i++) {
                if (searchEditText.length()==0)
                    break;

                if (mOriginal.substring(i, i+editable.length()).equalsIgnoreCase(editable.toString())) {

                    MapEntry mapEntry = new MapEntry(i, i+editable.length());
                    it.add(mapEntry);
                }

            }
            System.out.println(nodes.size());

            // ignore this code block for time being :/
            if (!it.hasNext()) {
                upButton.setEnabled(true);
                downButton.setEnabled(true);
            } else {
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            }
        }
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            View actionModeLayout = getLayoutInflater().inflate(R.layout.actionmode_textviewer, null);

            mode.setCustomView(actionModeLayout);
            menuInflater.inflate(R.menu.empty, menu);

            searchQuery(actionModeLayout);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };

    public boolean searchQuery(final View actionModeView) {

        searchEditText = (EditText) actionModeView.findViewById(R.id.search_box);

        upButton = (ImageButton) actionModeView.findViewById(R.id.prev);
        downButton = (ImageButton) actionModeView.findViewById(R.id.next);
        closeButton = (ImageButton) actionModeView.findViewById(R.id.close);

        searchEditText.addTextChangedListener(this);

        upButton.setOnClickListener(this);
        upButton.setEnabled(false);
        downButton.setOnClickListener(this);
        downButton.setEnabled(false);
        closeButton.setOnClickListener(this);

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.prev:
                // upButton
                Log.d(getClass().getName(), "previous button pressed");
                if(it.hasPrevious()) {
                    MapEntry keyValue = (MapEntry) it.previous();
                    Log.d(getClass().getName(), "equals after index " + keyValue.getKey()
                            + " to " + keyValue.getValue());
                }
                break;
            case R.id.next:
                // downButton
                Log.d(getClass().getName(), "next button pressed");
                if(it.hasNext()) {
                    MapEntry keyValue = (MapEntry) it.next();
                    Log.d(getClass().getName(), "equals after index " + keyValue.getKey()
                            + " to " + keyValue.getValue());
                }
                break;
            case R.id.close:
                // closeButton
                searchEditText.setText("");
                break;
            default:
                return;
        }
    }
}
