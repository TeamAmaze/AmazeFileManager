/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.ui.activities;

import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.CONTENT;
import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.FILE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.asynctasks.ReadFileTask;
import com.amaze.filemanager.asynchronous.asynctasks.SearchTextTask;
import com.amaze.filemanager.asynchronous.asynctasks.WriteFileAbstraction;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.MapEntry;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.snackbar.Snackbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class TextEditorActivity extends ThemedActivity
    implements TextWatcher, View.OnClickListener {

  public EditText mInput, searchEditText;
  private EditableFileAbstraction mFile;
  private String mOriginal;
  private Timer mTimer;
  private boolean mModified;
  private Typeface mInputTypefaceDefault, mInputTypefaceMono;
  private androidx.appcompat.widget.Toolbar toolbar;
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
  private static final String KEY_MONOFONT = "monofont";

  private RelativeLayout searchViewLayout;
  public ImageButton upButton, downButton, closeButton;
  private File cacheFile; // represents a file saved in cache

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    searchViewLayout = findViewById(R.id.searchview);
    searchViewLayout.setBackgroundColor(getPrimary());

    searchEditText = searchViewLayout.findViewById(R.id.search_box);
    upButton = searchViewLayout.findViewById(R.id.prev);
    downButton = searchViewLayout.findViewById(R.id.next);
    closeButton = searchViewLayout.findViewById(R.id.close);

    searchEditText.addTextChangedListener(this);

    upButton.setOnClickListener(this);
    // upButton.setEnabled(false);
    downButton.setOnClickListener(this);
    // downButton.setEnabled(false);
    closeButton.setOnClickListener(this);

    boolean useNewStack = getBoolean(PREFERENCE_TEXTEDITOR_NEWSTACK);

    getSupportActionBar().setDisplayHomeAsUpEnabled(!useNewStack);

    mInput = findViewById(R.id.fname);
    scrollView = findViewById(R.id.editscroll);

    final Uri uri = getIntent().getData();
    if (uri != null) {
      mFile = new EditableFileAbstraction(this, uri);
    } else {
      Toast.makeText(this, R.string.no_file_error, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    getSupportActionBar().setTitle(mFile.name);

    mInput.addTextChangedListener(this);
    if (getAppTheme().equals(AppTheme.DARK))
      mInput.setBackgroundColor(Utils.getColor(this, R.color.holo_dark_background));
    else if (getAppTheme().equals(AppTheme.BLACK))
      mInput.setBackgroundColor(Utils.getColor(this, android.R.color.black));

    if (mInput.getTypeface() == null) mInput.setTypeface(Typeface.DEFAULT);

    mInputTypefaceDefault = mInput.getTypeface();
    mInputTypefaceMono = Typeface.MONOSPACE;

    if (savedInstanceState != null) {
      mOriginal = savedInstanceState.getString(KEY_ORIGINAL_TEXT);
      int index = savedInstanceState.getInt(KEY_INDEX);
      mInput.setText(savedInstanceState.getString(KEY_MODIFIED_TEXT));
      mInput.setScrollY(index);
      if (savedInstanceState.getBoolean(KEY_MONOFONT)) mInput.setTypeface(mInputTypefaceMono);
    } else {
      load();
    }
    initStatusBarResources(findViewById(R.id.texteditor));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_MODIFIED_TEXT, mInput.getText().toString());
    outState.putInt(KEY_INDEX, mInput.getScrollY());
    outState.putString(KEY_ORIGINAL_TEXT, mOriginal);
    outState.putBoolean(KEY_MONOFONT, mInputTypefaceMono.equals(mInput.getTypeface()));
  }

  private void checkUnsavedChanges() {
    if (mOriginal != null && mInput.isShown() && !mOriginal.equals(mInput.getText().toString())) {
      new MaterialDialog.Builder(this)
          .title(R.string.unsaved_changes)
          .content(R.string.unsaved_changes_description)
          .positiveText(R.string.yes)
          .negativeText(R.string.no)
          .positiveColor(getAccent())
          .negativeColor(getAccent())
          .onPositive(
              (dialog, which) -> {
                saveFile(mInput.getText().toString());
                finish();
              })
          .onNegative((dialog, which) -> finish())
          .build()
          .show();
    } else {
      finish();
    }
  }

  /**
   * Method initiates a worker thread which writes the {@link #mInput} bytes to the defined file/uri
   * 's output stream
   *
   * @param editTextString the edit text string
   */
  private void saveFile(final String editTextString) {
    Toast.makeText(this, R.string.saving, Toast.LENGTH_SHORT).show();

    new WriteFileAbstraction(
            this,
            getContentResolver(),
            mFile,
            editTextString,
            cacheFile,
            isRootExplorer(),
            (errorCode) -> {
              switch (errorCode) {
                case WriteFileAbstraction.NORMAL:
                  mOriginal = editTextString;
                  mModified = false;
                  invalidateOptionsMenu();
                  Toast.makeText(
                          getApplicationContext(), getString(R.string.done), Toast.LENGTH_SHORT)
                      .show();
                  break;
                case WriteFileAbstraction.EXCEPTION_STREAM_NOT_FOUND:
                  Toast.makeText(
                          getApplicationContext(),
                          R.string.error_file_not_found,
                          Toast.LENGTH_SHORT)
                      .show();
                  break;
                case WriteFileAbstraction.EXCEPTION_IO:
                  Toast.makeText(getApplicationContext(), R.string.error_io, Toast.LENGTH_SHORT)
                      .show();
                  break;
                case WriteFileAbstraction.EXCEPTION_SHELL_NOT_RUNNING:
                  Toast.makeText(getApplicationContext(), R.string.root_failure, Toast.LENGTH_SHORT)
                      .show();
                  break;
              }
            })
        .execute();
  }

  /**
   * Initiates loading of file/uri by getting an input stream associated with it on a worker thread
   */
  private void load() {
    Snackbar.make(scrollView, R.string.loading, Snackbar.LENGTH_SHORT).show();

    new ReadFileTask(
            getContentResolver(),
            mFile,
            getExternalCacheDir(),
            isRootExplorer(),
            (data) -> {
              switch (data.error) {
                case ReadFileTask.NORMAL:
                  cacheFile = data.cachedFile;
                  mOriginal = data.fileContents;

                  try {
                    mInput.setText(data.fileContents);

                    if (mFile.scheme.equals(FILE)
                        && getExternalCacheDir() != null
                        && mFile
                            .hybridFileParcelable
                            .getPath()
                            .contains(getExternalCacheDir().getPath())
                        && cacheFile == null) {
                      // file in cache, and not a root temporary file
                      mInput.setInputType(EditorInfo.TYPE_NULL);
                      mInput.setSingleLine(false);
                      mInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);

                      Snackbar snackbar =
                          Snackbar.make(
                              mInput,
                              getResources().getString(R.string.file_read_only),
                              Snackbar.LENGTH_INDEFINITE);
                      snackbar.setAction(
                          getResources().getString(R.string.got_it).toUpperCase(),
                          v -> snackbar.dismiss());
                      snackbar.show();
                    }

                    if (data.fileContents.isEmpty()) {
                      mInput.setHint(R.string.file_empty);
                    } else {
                      mInput.setHint(null);
                    }
                  } catch (OutOfMemoryError e) {
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_SHORT)
                        .show();
                    finish();
                  }
                  break;
                case ReadFileTask.EXCEPTION_STREAM_NOT_FOUND:
                  Toast.makeText(
                          getApplicationContext(),
                          R.string.error_file_not_found,
                          Toast.LENGTH_SHORT)
                      .show();
                  finish();
                  break;
                case ReadFileTask.EXCEPTION_IO:
                  Toast.makeText(getApplicationContext(), R.string.error_io, Toast.LENGTH_SHORT)
                      .show();
                  finish();
                  break;
                case ReadFileTask.EXCEPTION_OOM:
                  Toast.makeText(
                          getApplicationContext(),
                          R.string.error_file_too_large,
                          Toast.LENGTH_SHORT)
                      .show();
                  finish();
                  break;
              }
            })
        .execute();
  }

  @Override
  public void onBackPressed() {
    checkUnsavedChanges();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.text, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.save).setVisible(mModified);
    menu.findItem(R.id.monofont).setChecked(mInputTypefaceMono.equals(mInput.getTypeface()));
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        checkUnsavedChanges();
        break;
      case R.id.save:
        // Make sure EditText is visible before saving!
        saveFile(mInput.getText().toString());
        break;
      case R.id.details:
        if (mFile.scheme.equals(FILE) && mFile.hybridFileParcelable.getFile().exists()) {
          GeneralDialogCreation.showPropertiesDialogWithoutPermissions(
              mFile.hybridFileParcelable, this, getAppTheme());
        } else if (mFile.scheme.equals(CONTENT)) {
          if (getApplicationContext().getPackageName().equals(mFile.uri.getAuthority())) {
            File file = FileUtils.fromContentUri(mFile.uri);
            HybridFileParcelable p = new HybridFileParcelable(file.getAbsolutePath());
            if (isRootExplorer()) p.setMode(OpenMode.ROOT);
            GeneralDialogCreation.showPropertiesDialogWithoutPermissions(p, this, getAppTheme());
          }
        } else {
          Toast.makeText(this, R.string.no_obtainable_info, Toast.LENGTH_SHORT).show();
        }
        break;
      case R.id.openwith:
        if (mFile.scheme.equals(FILE)) {
          File currentFile = mFile.hybridFileParcelable.getFile();
          if (currentFile.exists()) {
            boolean useNewStack = getBoolean(PREFERENCE_TEXTEDITOR_NEWSTACK);
            FileUtils.openWith(currentFile, this, useNewStack);
          } else {
            Toast.makeText(this, R.string.not_allowed, Toast.LENGTH_SHORT).show();
          }
        } else {
          Toast.makeText(this, R.string.reopen_from_source, Toast.LENGTH_SHORT).show();
        }
        break;
      case R.id.find:
        if (searchViewLayout.isShown()) hideSearchView();
        else revealSearchView();
        break;
      case R.id.monofont:
        item.setChecked(!item.isChecked());
        mInput.setTypeface(item.isChecked() ? mInputTypefaceMono : mInputTypefaceDefault);
        break;
      default:
        return false;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (cacheFile != null && cacheFile.exists()) cacheFile.delete();
  }

  @Override
  public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    // condition to check if callback is called in search editText
    if (searchEditText != null && charSequence.hashCode() == searchEditText.getText().hashCode()) {

      // clearing before adding new values
      if (searchTextTask != null) searchTextTask.cancel(true);

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
      mTimer.schedule(
          new TimerTask() {
            boolean modified;

            @Override
            public void run() {
              modified = !mInput.getText().toString().equals(mOriginal);
              if (mModified != modified) {
                mModified = modified;
                invalidateOptionsMenu();
              }
            }
          },
          250);
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

  /** show search view with a circular reveal animation */
  void revealSearchView() {
    int startRadius = 4;
    int endRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    // hardcoded and completely random
    int cx = metrics.widthPixels - 160;
    int cy = toolbar.getBottom();
    Animator animator;

    // FIXME: 2016/11/18   ViewAnimationUtils Compatibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      animator =
          ViewAnimationUtils.createCircularReveal(searchViewLayout, cx, cy, startRadius, endRadius);
    else animator = ObjectAnimator.ofFloat(searchViewLayout, "alpha", 0f, 1f);

    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(600);
    searchViewLayout.setVisibility(View.VISIBLE);
    searchEditText.setText("");
    animator.start();
    animator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            searchEditText.requestFocus();
            InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
          }
        });
  }

  /** hide search view with a circular reveal animation */
  void hideSearchView() {
    int endRadius = 4;
    int startRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    // hardcoded and completely random
    int cx = metrics.widthPixels - 160;
    int cy = toolbar.getBottom();

    Animator animator;
    // FIXME: 2016/11/18   ViewAnimationUtils Compatibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      animator =
          ViewAnimationUtils.createCircularReveal(searchViewLayout, cx, cy, startRadius, endRadius);
    } else {
      animator = ObjectAnimator.ofFloat(searchViewLayout, "alpha", 0f, 1f);
    }

    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(600);
    animator.start();
    animator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            searchViewLayout.setVisibility(View.GONE);
            InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                searchEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
          }
        });
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.prev:
        // upButton
        if (mCurrent > 0) {

          // setting older span back before setting new one
          Map.Entry keyValueOld = nodes.get(mCurrent).getKey();
          if (getAppTheme().equals(AppTheme.LIGHT)) {
            mInput
                .getText()
                .setSpan(
                    new BackgroundColorSpan(Color.YELLOW),
                    (Integer) keyValueOld.getKey(),
                    (Integer) keyValueOld.getValue(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
          } else {
            mInput
                .getText()
                .setSpan(
                    new BackgroundColorSpan(Color.LTGRAY),
                    (Integer) keyValueOld.getKey(),
                    (Integer) keyValueOld.getValue(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
          }
          // highlighting previous element in list
          Map.Entry keyValueNew = nodes.get(--mCurrent).getKey();
          mInput
              .getText()
              .setSpan(
                  new BackgroundColorSpan(Utils.getColor(this, R.color.search_text_highlight)),
                  (Integer) keyValueNew.getKey(),
                  (Integer) keyValueNew.getValue(),
                  Spanned.SPAN_INCLUSIVE_INCLUSIVE);

          // scrolling to the highlighted element
          scrollView.scrollTo(
              0,
              (Integer) keyValueNew.getValue()
                  + mInput.getLineHeight()
                  + Math.round(mInput.getLineSpacingExtra())
                  - getSupportActionBar().getHeight());
        }
        break;
      case R.id.next:
        // downButton
        if (mCurrent < nodes.size() - 1) {

          // setting older span back before setting new one
          if (mCurrent != -1) {

            Map.Entry keyValueOld = nodes.get(mCurrent).getKey();
            if (getAppTheme().equals(AppTheme.LIGHT)) {
              mInput
                  .getText()
                  .setSpan(
                      new BackgroundColorSpan(Color.YELLOW),
                      (Integer) keyValueOld.getKey(),
                      (Integer) keyValueOld.getValue(),
                      Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            } else {
              mInput
                  .getText()
                  .setSpan(
                      new BackgroundColorSpan(Color.LTGRAY),
                      (Integer) keyValueOld.getKey(),
                      (Integer) keyValueOld.getValue(),
                      Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
          }

          Map.Entry keyValueNew = nodes.get(++mCurrent).getKey();
          mInput
              .getText()
              .setSpan(
                  new BackgroundColorSpan(Utils.getColor(this, R.color.search_text_highlight)),
                  (Integer) keyValueNew.getKey(),
                  (Integer) keyValueNew.getValue(),
                  Spanned.SPAN_INCLUSIVE_INCLUSIVE);

          // scrolling to the highlighted element
          scrollView.scrollTo(
              0,
              (Integer) keyValueNew.getValue()
                  + mInput.getLineHeight()
                  + Math.round(mInput.getLineSpacingExtra())
                  - getSupportActionBar().getHeight());
        }
        break;
      case R.id.close:
        // closeButton
        findViewById(R.id.searchview).setVisibility(View.GONE);
        cleanSpans();
        break;
    }
  }

  private void cleanSpans() {
    // resetting current highlight and line number
    nodes.clear();
    mCurrent = -1;
    mLine = 0;

    // clearing textView spans
    BackgroundColorSpan[] colorSpans =
        mInput.getText().getSpans(0, mInput.length(), BackgroundColorSpan.class);
    for (BackgroundColorSpan colorSpan : colorSpans) {
      mInput.getText().removeSpan(colorSpan);
    }
  }
}
