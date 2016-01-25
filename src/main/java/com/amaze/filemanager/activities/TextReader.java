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
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.services.asynctasks.SearchTextTask;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MapEntry;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TextReader extends AppCompatActivity
        implements TextWatcher, View.OnClickListener, Runnable {

    String path;
    Futils utils = new Futils();
    Context c = this;
    boolean rootMode;
    public int theme, theme1;
    SharedPreferences Sp;
    public EditText mInput, searchEditText;
    private java.io.File mFile;
    private String mOriginal, skin;
    private Timer mTimer;
    private boolean mModified, isEditAllowed = true;
    private int skinStatusBar;
    private String fabSkin;
    private android.support.v7.widget.Toolbar toolbar;
    //ArrayList<StringBuilder> texts;
    //static final int maxlength=200;
    //int index=0;
     ScrollView scrollView;

    /*
    List maintaining the searched text's start/end index as key/value pair
     */
    public ArrayList<MapEntry> nodes;

    /*
    variable to maintain the position of index
    while pressing next/previous button in the searchBox
     */
    private int mCurrent = -1;

    /*
    variable to maintain line number of the searched phrase
    further used to calculate the scroll position
     */
    public int mLine = 0;

    private SearchTextTask searchTextTask;

    Uri uri=null;
    public ImageButton upButton, downButton, closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        fabSkin = PreferenceUtils.getAccentString(Sp);

        theme = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme == 2 ? PreferenceUtils.hourOfDay() : theme;

        nodes = new ArrayList<>();

        // setting accent theme
        if (Build.VERSION.SDK_INT >= 21) {

            switch (fabSkin) {
                case "#F44336":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#e91e63":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9c27b0":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673ab7":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3f51b5":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8bc34a":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607d8b":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004d40":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme1 == 1) {
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
        skin = PreferenceUtils.getPrimaryColorString(Sp);
        findViewById(R.id.lin).setBackgroundColor(Color.parseColor(skin));
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze", ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(), Color.parseColor(skin));
            ((Activity) this).setTaskDescription(taskDescription);
        }
        skinStatusBar = PreferenceUtils.getStatusColor(skin);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootMode = PreferenceManager.getDefaultSharedPreferences(c)
                .getBoolean("rootmode", false);
        int sdk = Build.VERSION.SDK_INT;

        if (sdk == 20 || sdk == 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.texteditor).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (Build.VERSION.SDK_INT >= 21) {
            boolean colourednavigation = Sp.getBoolean("colorednavigation", true);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor((PreferenceUtils.getStatusColor(skin)));
            if (colourednavigation)
                window.setNavigationBarColor((PreferenceUtils.getStatusColor(skin)));

        }
        mInput = (EditText) findViewById(R.id.fname);
        scrollView=(ScrollView)findViewById(R.id.editscroll);

        try {
            if (getIntent().getData() != null){
                uri=getIntent().getData();

                mFile = new File(getIntent().getData().getPath());
            }
            else
                mFile = new File(getIntent().getStringExtra("path"));
        } catch (Exception e) {
            mFile = null;
        }
        String fileName=null;
        try {
            if (uri.getScheme().equals("file")) {
                fileName = uri.getLastPathSegment();
            } else {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, new String[]{
                            MediaStore.Images.ImageColumns.DISPLAY_NAME
                    }, null, null, null);

                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
                    }
                } finally {

                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(fileName==null || fileName.trim().length()==0)fileName=f.getName();
        getSupportActionBar().setTitle(fileName);
        mInput.addTextChangedListener(this);
        try {
            if (theme1 == 1)
                mInput.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {

        }
        load(mFile);
    }

    @Override
    public void run() {
        BackgroundColorSpan[] colorSpans = mInput.getText().getSpans(0,
                mInput.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan colorSpan : colorSpans) {
            mInput.getText().removeSpan(colorSpan);
        }
    }


    public void onDestroyActionMode() {

        // clearing all the spans
        Thread clearSpans = new Thread(this);
        clearSpans.run();
    }

    class a extends ScrollView {
    public a(Context context) {
        super(context);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

    }
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
                    RootTools.remount(mFile.getParent(), "rw");
                    RootHelper.runAndWait("cat " + f.getPath() + " > " + mFile.getPath(), true);
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
                    InputStream inputStream=getInputStream(uri,path);
                    if (inputStream!=null) {
                        String str=null;
                        //if(texts==null)texts=new ArrayList<>();
                        StringBuilder stringBuilder=new StringBuilder();
                        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
                        if(bufferedReader!=null){
                         //   int i=0,k=0;
                       //     StringBuilder stringBuilder1=new StringBuilder("");
                            while ((str=bufferedReader.readLine())!=null){
                                stringBuilder.append(str+"\n");
                         /*       if(k<maxlength){
                                    stringBuilder1.append(str+"\n");
                                    k++;
                                }else {
                                    texts.add(i,stringBuilder1);
                                    i++;
                                    stringBuilder1=new StringBuilder("");
                                    stringBuilder1.append(str+"\n");
                                    k=1;
                                }
                        */    }
                          //  texts.add(i,stringBuilder1);
                        }
                        mOriginal=stringBuilder.toString();
                     inputStream.close();
                    } else {
                        mOriginal = "";
                        StringBuilder stringBuilder=new StringBuilder();
                        ArrayList<String> arrayList = RootHelper
                                .runAndWait1("cat " + mFile.getPath(), true);
                      //  int i=0,k=0;
                        //StringBuilder stringBuilder1=new StringBuilder("");
                        for (String str:arrayList){
                            stringBuilder.append(str+"\n");
                        /*    if(k<maxlength){
                                stringBuilder1.append(str+"\n");
                                k++;
                            }else {
                                texts.add(i,stringBuilder1);
                                i++;
                                stringBuilder1=new StringBuilder("");
                                stringBuilder1.append(str+"\n");
                                k=1;
                            }
                        */}
                       // texts.add(i,stringBuilder1);
                        mOriginal=stringBuilder.toString();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mInput.setText(mOriginal);
                                if (mOriginal.isEmpty()) {

                                    mInput.setHint(R.string.file_empty);
                                } else
                                    mInput.setHint(null);
                            } catch (OutOfMemoryError e) {
                                mInput.setHint(R.string.error);
                            }
                            setProgress(false);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
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
        menu.findItem(R.id.find).setVisible(true);
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
                if(mFile.canRead()){
                    HFile hFile=new HFile(HFile.LOCAL_MODE,mFile.getPath());
                    hFile.generateMode(this);
                    utils.showProps(hFile, this, theme1);
                }else Toast.makeText(this,R.string.not_allowed,Toast.LENGTH_SHORT).show();
                break;
            case R.id.openwith:
                if(mFile.canRead()){
                    utils.openunknown(mFile, c, false);
                }else Toast.makeText(this,R.string.not_allowed,Toast.LENGTH_SHORT).show();
                break;
            case R.id.find:
                searchQueryInit(findViewById(R.id.searchview));
                break;
            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        // clearing before adding new values
        if (searchEditText != null && charSequence.hashCode() == searchEditText.getText().hashCode()) {

            if (searchTextTask!=null)
                searchTextTask.cancel(true);

            nodes.clear();
            mCurrent = -1;
            mLine = 0;

            Thread clearSpans = new Thread(this);
            clearSpans.run();
        }
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (charSequence.hashCode() == mInput.getText().hashCode()) {
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
        if (searchEditText != null && editable.hashCode() == searchEditText.getText().hashCode()) {

            searchTextTask = new SearchTextTask(this);
            searchTextTask.execute(editable);

        }
    }

    InputStream getInputStream(Uri uri,String path){
        InputStream stream=null;
           try {
               stream=getContentResolver().openInputStream(uri);
           } catch (FileNotFoundException e) {
               stream=null;
           }
        if(stream==null)
            if(new File(path).canRead()){
            try {
                stream=new FileInputStream(path);
            } catch (FileNotFoundException e) {
                stream=null;
            }
        }

        return stream;
    }
    public boolean searchQueryInit(final View actionModeView) {
        actionModeView.setVisibility(View.VISIBLE);
        searchEditText = (EditText) actionModeView.findViewById(R.id.search_box);
        searchEditText.setText("");
        upButton = (ImageButton) actionModeView.findViewById(R.id.prev);
        downButton = (ImageButton) actionModeView.findViewById(R.id.next);
        closeButton = (ImageButton) actionModeView.findViewById(R.id.close);

        searchEditText.addTextChangedListener(this);
        searchEditText.requestFocus();

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
                if (mCurrent>0) {

                    // setting older span back before setting new one
                    Map.Entry keyValueOld = (Map.Entry) nodes.get(mCurrent).getKey();
                    mInput.getText().setSpan(theme1 == 0 ? new BackgroundColorSpan(Color.YELLOW) :
                                    new BackgroundColorSpan(Color.LTGRAY),
                            (Integer) keyValueOld.getKey(),
                            (Integer) keyValueOld.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // highlighting previous element in list
                    Map.Entry keyValueNew = (Map.Entry) nodes.get(--mCurrent).getKey();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mInput.getText().setSpan(new BackgroundColorSpan(getResources()
                                        .getColor(R.color.search_text_highlight, getTheme())),
                                (Integer) keyValueNew.getKey(),
                                (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    } else {
                        mInput.getText().setSpan(new BackgroundColorSpan(getResources()
                                        .getColor(R.color.search_text_highlight)),
                                (Integer) keyValueNew.getKey(),
                                (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }
                }
                break;
            case R.id.next:
                // downButton
                if (mCurrent<nodes.size()-1) {

                    // setting older span back before setting new one
                    if (mCurrent!=-1) {

                        Map.Entry keyValueOld = (Map.Entry) nodes.get(mCurrent).getKey();
                        mInput.getText().setSpan(theme1 == 0 ? new BackgroundColorSpan(Color.YELLOW) :
                                        new BackgroundColorSpan(Color.LTGRAY),
                                (Integer) keyValueOld.getKey(),
                                (Integer) keyValueOld.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    Map.Entry keyValueNew = (Map.Entry) nodes.get(++mCurrent).getKey();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mInput.getText().setSpan(new BackgroundColorSpan(getResources()
                                        .getColor(R.color.search_text_highlight, getTheme())),
                                (Integer) keyValueNew.getKey(),
                                (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    } else {
                        mInput.getText().setSpan(new BackgroundColorSpan(getResources()
                                        .getColor(R.color.search_text_highlight)),
                                (Integer) keyValueNew.getKey(),
                                (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    // scrolling to the highlighted element
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    scrollView.scrollTo(0, (Integer) keyValueNew.getValue()
                            + mInput.getLineHeight() - displayMetrics.heightPixels/2);
                }
                break;
            case R.id.close:
                onDestroyActionMode();
                // closeButton
                findViewById(R.id.searchview).setVisibility(View.GONE);
                break;
            default:
                return;
        }
    }

    public int getLineNumber() {
        return this.mLine;
    }

    public void setLineNumber(int lineNumber) {
        this.mLine = lineNumber;
    }
}
