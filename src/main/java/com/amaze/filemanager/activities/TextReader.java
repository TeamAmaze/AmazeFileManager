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


import android.animation.Animator;
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
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
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

public class TextReader extends BaseActivity
        implements TextWatcher, View.OnClickListener {

    String path;
    Futils utils = new Futils();
    Context c = this;
    public EditText mInput, searchEditText;
    private java.io.File mFile;
    private String mOriginal;
    private Timer mTimer;
    private boolean mModified, isEditAllowed = true;
    private android.support.v7.widget.Toolbar toolbar;
    //ArrayList<StringBuilder> texts;
    //static final int maxlength=200;
    //int index=0;
     ScrollView scrollView;

    /*
     * List maintaining the searched text's start/end index as key/value pair
     */
    public ArrayList<MapEntry> nodes = new ArrayList<>();

    /*
     * variable to maintain the position of index
     * while pressing next/previous button in the searchBox
     */
    private int mCurrent = -1;

    /*
     * variable to maintain line number of the searched phrase
     * further used to calculate the scroll position
     */
    public int mLine = 0;

    private SearchTextTask searchTextTask;
    private static final String KEY_MODIFIED_TEXT = "modified";
    private static final String KEY_INDEX = "index";
    private static final String KEY_ORIGINAL_TEXT = "original";

    private RelativeLayout searchViewLayout;

    Uri uri=null;
    public ImageButton upButton, downButton, closeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (theme1 == 1) {
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        setContentView(R.layout.search);
        searchViewLayout = (RelativeLayout) findViewById(R.id.searchview);
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //findViewById(R.id.lin).setBackgroundColor(Color.parseColor(skin));
        toolbar.setBackgroundColor(Color.parseColor(MainActivity.currentTab==1?skinTwo:skin));
        searchViewLayout.setBackgroundColor(Color.parseColor(MainActivity.currentTab==1?skinTwo:skin));
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze",
                    ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(),
                    Color.parseColor(MainActivity.currentTab==1?skinTwo:skin));
            ((Activity) this).setTaskDescription(taskDescription);
        }

        searchEditText = (EditText) searchViewLayout.findViewById(R.id.search_box);
        upButton = (ImageButton) searchViewLayout.findViewById(R.id.prev);
        downButton = (ImageButton) searchViewLayout.findViewById(R.id.next);
        closeButton = (ImageButton) searchViewLayout.findViewById(R.id.close);

        searchEditText.addTextChangedListener(this);

        upButton.setOnClickListener(this);
        //upButton.setEnabled(false);
        downButton.setOnClickListener(this);
        //downButton.setEnabled(false);
        closeButton.setOnClickListener(this);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color
                .parseColor(MainActivity.currentTab==1?skinTwo:skin)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int sdk = Build.VERSION.SDK_INT;

        if (sdk == 20 || sdk == 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(MainActivity.currentTab==1?skinTwo:skin));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.texteditor).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (Build.VERSION.SDK_INT >= 21) {
            boolean colourednavigation = Sp.getBoolean("colorednavigation", true);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor((PreferenceUtils.getStatusColor(MainActivity.currentTab==1?skinTwo:skin)));
            if (colourednavigation)
                window.setNavigationBarColor((PreferenceUtils
                        .getStatusColor(MainActivity.currentTab==1?skinTwo:skin)));

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

        if (savedInstanceState!=null) {

            mOriginal = savedInstanceState.getString(KEY_ORIGINAL_TEXT);
            int index = savedInstanceState.getInt(KEY_INDEX);
            mInput.setText(savedInstanceState.getString(KEY_MODIFIED_TEXT));
            mInput.setScrollY(index);
        } else {

            load(mFile);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_MODIFIED_TEXT, mInput.getText().toString());
        outState.putInt(KEY_INDEX, mInput.getScrollY());
        outState.putString(KEY_ORIGINAL_TEXT, mOriginal);
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
                    .positiveColor(Color.parseColor(accentSkin))
                    .negativeColor(Color.parseColor(accentSkin))
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
                if (searchViewLayout.isShown()) hideSearchView();
                else revealSearchView();
                break;
            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        // condition to check if callback is called in search editText
        if (searchEditText != null && charSequence.hashCode() == searchEditText.getText().hashCode()) {

            // clearing before adding new values
            if (searchTextTask!=null) searchTextTask.cancel(true);

            cleanSpans();
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

    /**
     * show search view with a circular reveal animation
     */
    void revealSearchView() {

        int startRadius = 4;
        int endRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // hardcoded and completely random
        int cx = metrics.widthPixels - 160;
        int cy = toolbar.getBottom();
        Animator animator = ViewAnimationUtils.createCircularReveal(searchViewLayout, cx, cy,
                startRadius, endRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(600);
        searchViewLayout.setVisibility(View.VISIBLE);
        searchEditText.setText("");
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

    }

    /**
     * hide search view with a circular reveal animation
     */
    void hideSearchView() {

        int endRadius = 4;
        int startRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // hardcoded and completely random
        int cx = metrics.widthPixels - 160;
        int cy = toolbar.getBottom();
        Animator animator = ViewAnimationUtils.createCircularReveal(searchViewLayout, cx, cy,
                startRadius, endRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(600);
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                searchViewLayout.setVisibility(View.GONE);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(searchEditText.getWindowToken(),
                        InputMethodManager.HIDE_IMPLICIT_ONLY);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
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
                    mInput.getText().setSpan(new BackgroundColorSpan(getResources()
                                    .getColor(R.color.search_text_highlight, getTheme())),
                            (Integer) keyValueNew.getKey(),
                            (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // scrolling to the highlighted element
                    scrollView.scrollTo(0, (Integer) keyValueNew.getValue()
                            + mInput.getLineHeight() + Math.round(mInput.getLineSpacingExtra())
                            - getSupportActionBar().getHeight());
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
                    mInput.getText().setSpan(new BackgroundColorSpan(getResources()
                                    .getColor(R.color.search_text_highlight, getTheme())),
                            (Integer) keyValueNew.getKey(),
                            (Integer) keyValueNew.getValue(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    // scrolling to the highlighted element
                    scrollView.scrollTo(0, (Integer) keyValueNew.getValue()
                            + mInput.getLineHeight() + Math.round(mInput.getLineSpacingExtra())
                            - getSupportActionBar().getHeight());
                }
                break;
            case R.id.close:
                // closeButton
                findViewById(R.id.searchview).setVisibility(View.GONE);
                cleanSpans();
                break;
            default:
                return;
        }
    }

    private void cleanSpans() {

        // resetting current highlight and line number
        nodes.clear();
        mCurrent = -1;
        mLine = 0;

        // clearing textView spans
        BackgroundColorSpan[] colorSpans = mInput.getText().getSpans(0,
                mInput.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan colorSpan : colorSpans) {
            mInput.getText().removeSpan(colorSpan);
        }
    }

    public int getLineNumber() {
        return this.mLine;
    }

    public void setLineNumber(int lineNumber) {
        this.mLine = lineNumber;
    }
}
