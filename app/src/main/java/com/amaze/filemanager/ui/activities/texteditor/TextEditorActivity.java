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

package com.amaze.filemanager.ui.activities.texteditor;

import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.CONTENT;
import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.FILE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.asynctasks.SearchTextTask;
import com.amaze.filemanager.asynchronous.asynctasks.TaskKt;
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.ReadTextFileTask;
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.write.WriteTextFileTask;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.OnProgressUpdate;
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

import androidx.annotation.ColorInt;
import androidx.lifecycle.ViewModelProvider;

public class TextEditorActivity extends ThemedActivity
    implements TextWatcher, View.OnClickListener {

  public EditText mainTextView;
  public EditText searchEditText;
  private Typeface inputTypefaceDefault;
  private Typeface inputTypefaceMono;
  private androidx.appcompat.widget.Toolbar toolbar;
  ScrollView scrollView;

  private SearchTextTask searchTextTask;
  private static final String KEY_MODIFIED_TEXT = "modified";
  private static final String KEY_INDEX = "index";
  private static final String KEY_ORIGINAL_TEXT = "original";
  private static final String KEY_MONOFONT = "monofont";

  private RelativeLayout searchViewLayout;
  public ImageButton upButton;
  public ImageButton downButton;
  public ImageButton closeButton;

  private Snackbar loadingSnackbar;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

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

    mainTextView = findViewById(R.id.fname);
    scrollView = findViewById(R.id.editscroll);

    final Uri uri = getIntent().getData();
    if (uri != null) {
      viewModel.setFile(new EditableFileAbstraction(this, uri));
    } else {
      Toast.makeText(this, R.string.no_file_error, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    getSupportActionBar().setTitle(viewModel.getFile().name);

    mainTextView.addTextChangedListener(this);

    if (getAppTheme().equals(AppTheme.DARK)) {
      mainTextView.setBackgroundColor(Utils.getColor(this, R.color.holo_dark_background));
    } else if (getAppTheme().equals(AppTheme.BLACK)) {
      mainTextView.setBackgroundColor(Utils.getColor(this, android.R.color.black));
    }

    if (mainTextView.getTypeface() == null) {
      mainTextView.setTypeface(Typeface.DEFAULT);
    }

    inputTypefaceDefault = mainTextView.getTypeface();
    inputTypefaceMono = Typeface.MONOSPACE;

    if (savedInstanceState != null) {
      viewModel.setOriginal(savedInstanceState.getString(KEY_ORIGINAL_TEXT));
      int index = savedInstanceState.getInt(KEY_INDEX);
      mainTextView.setText(savedInstanceState.getString(KEY_MODIFIED_TEXT));
      mainTextView.setScrollY(index);
      if (savedInstanceState.getBoolean(KEY_MONOFONT)) {
        mainTextView.setTypeface(inputTypefaceMono);
      }
    } else {
      load(this);
    }
    initStatusBarResources(findViewById(R.id.texteditor));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    outState.putString(KEY_MODIFIED_TEXT, mainTextView.getText().toString());
    outState.putInt(KEY_INDEX, mainTextView.getScrollY());
    outState.putString(KEY_ORIGINAL_TEXT, viewModel.getOriginal());
    outState.putBoolean(KEY_MONOFONT, inputTypefaceMono.equals(mainTextView.getTypeface()));
  }

  private void checkUnsavedChanges() {
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    if (viewModel.getOriginal() != null
        && mainTextView.isShown()
        && !viewModel.getOriginal().equals(mainTextView.getText().toString())) {
      new MaterialDialog.Builder(this)
          .title(R.string.unsaved_changes)
          .content(R.string.unsaved_changes_description)
          .positiveText(R.string.yes)
          .negativeText(R.string.no)
          .positiveColor(getAccent())
          .negativeColor(getAccent())
          .onPositive(
              (dialog, which) -> {
                saveFile(this, mainTextView.getText().toString());
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
   * Method initiates a worker thread which writes the {@link #mainTextView} bytes to the defined
   * file/uri 's output stream
   *
   * @param activity a reference to the current activity
   * @param editTextString the edit text string
   */
  private static void saveFile(final TextEditorActivity activity, final String editTextString) {
    final WeakReference<TextEditorActivity> textEditorActivityWR = new WeakReference<>(activity);
    final WeakReference<Context> appContextWR =
        new WeakReference<>(activity.getApplicationContext());

    TaskKt.fromTask(
        new WriteTextFileTask(activity, editTextString, textEditorActivityWR, appContextWR));
  }

  /**
   * Initiates loading of file/uri by getting an input stream associated with it on a worker thread
   */
  private static void load(final TextEditorActivity activity) {
    activity.dismissLoadingSnackbar();

    activity.loadingSnackbar =
        Snackbar.make(activity.scrollView, R.string.loading, Snackbar.LENGTH_SHORT);
    activity.loadingSnackbar.show();

    final WeakReference<TextEditorActivity> textEditorActivityWR = new WeakReference<>(activity);
    final WeakReference<Context> appContextWR =
        new WeakReference<>(activity.getApplicationContext());

    TaskKt.fromTask(new ReadTextFileTask(activity, textEditorActivityWR, appContextWR));
  }

  public void setReadOnly() {
    mainTextView.setInputType(EditorInfo.TYPE_NULL);
    mainTextView.setSingleLine(false);
    mainTextView.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
  }

  public void dismissLoadingSnackbar() {
    if (loadingSnackbar != null) {
      loadingSnackbar.dismiss();
      loadingSnackbar = null;
    }
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
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    menu.findItem(R.id.save).setVisible(viewModel.getModified());
    menu.findItem(R.id.monofont).setChecked(inputTypefaceMono.equals(mainTextView.getTypeface()));
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);
    final EditableFileAbstraction editableFileAbstraction = viewModel.getFile();

    switch (item.getItemId()) {
      case android.R.id.home:
        checkUnsavedChanges();
        break;
      case R.id.save:
        // Make sure EditText is visible before saving!
        saveFile(this, mainTextView.getText().toString());
        break;
      case R.id.details:
        if (editableFileAbstraction.scheme.equals(FILE)
            && editableFileAbstraction.hybridFileParcelable.getFile().exists()) {
          GeneralDialogCreation.showPropertiesDialogWithoutPermissions(
              editableFileAbstraction.hybridFileParcelable, this, getAppTheme());
        } else if (editableFileAbstraction.scheme.equals(CONTENT)) {
          if (getApplicationContext()
              .getPackageName()
              .equals(editableFileAbstraction.uri.getAuthority())) {
            File file = FileUtils.fromContentUri(editableFileAbstraction.uri);
            HybridFileParcelable p = new HybridFileParcelable(file.getAbsolutePath());
            if (isRootExplorer()) p.setMode(OpenMode.ROOT);
            GeneralDialogCreation.showPropertiesDialogWithoutPermissions(p, this, getAppTheme());
          }
        } else {
          Toast.makeText(this, R.string.no_obtainable_info, Toast.LENGTH_SHORT).show();
        }
        break;
      case R.id.openwith:
        if (editableFileAbstraction.scheme.equals(FILE)) {
          File currentFile = editableFileAbstraction.hybridFileParcelable.getFile();
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
        mainTextView.setTypeface(item.isChecked() ? inputTypefaceMono : inputTypefaceDefault);
        break;
      default:
        return false;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);
    final File cacheFile = viewModel.getCacheFile();

    if (cacheFile != null && cacheFile.exists()) {
      cacheFile.delete();
    }
  }

  @Override
  public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    // condition to check if callback is called in search editText
    if (searchEditText != null && charSequence.hashCode() == searchEditText.getText().hashCode()) {
      final TextEditorActivityViewModel viewModel =
          new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

      // clearing before adding new values
      if (searchTextTask != null) {
        searchTextTask.cancel(true);
        searchTextTask = null; // dereference the task for GC
      }

      cleanSpans(viewModel);
    }
  }

  @Override
  public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    if (charSequence.hashCode() == mainTextView.getText().hashCode()) {
      final TextEditorActivityViewModel viewModel =
          new ViewModelProvider(this).get(TextEditorActivityViewModel.class);
      final Timer oldTimer = viewModel.getTimer();
      viewModel.setTimer(null);

      if (oldTimer != null) {
        oldTimer.cancel();
        oldTimer.purge();
      }

      final WeakReference<TextEditorActivity> textEditorActivityWR = new WeakReference<>(this);

      Timer newTimer = new Timer();
      newTimer.schedule(
          new TimerTask() {
            boolean modified;

            @Override
            public void run() {
              final TextEditorActivity textEditorActivity = textEditorActivityWR.get();
              if (textEditorActivity == null) {
                return;
              }

              final TextEditorActivityViewModel viewModel =
                  new ViewModelProvider(textEditorActivity).get(TextEditorActivityViewModel.class);

              modified =
                  !textEditorActivity
                      .mainTextView
                      .getText()
                      .toString()
                      .equals(viewModel.getOriginal());
              if (viewModel.getModified() != modified) {
                viewModel.setModified(modified);
                invalidateOptionsMenu();
              }
            }
          },
          250);

      viewModel.setTimer(newTimer);
    }
  }

  @Override
  public void afterTextChanged(Editable editable) {
    // searchBox callback block
    if (searchEditText != null && editable.hashCode() == searchEditText.getText().hashCode()) {
      final WeakReference<TextEditorActivity> textEditorActivityWR = new WeakReference<>(this);

      final OnProgressUpdate<SearchResultIndex> onProgressUpdate =
          index -> {
            final TextEditorActivity textEditorActivity = textEditorActivityWR.get();
            if (textEditorActivity == null) {
              return;
            }
            textEditorActivity.unhighlightSearchResult(index);
          };

      final OnAsyncTaskFinished<List<SearchResultIndex>> onAsyncTaskFinished =
          data -> {
            final TextEditorActivity textEditorActivity = textEditorActivityWR.get();

            if (textEditorActivity == null) {
              return;
            }

            final TextEditorActivityViewModel viewModel =
                new ViewModelProvider(textEditorActivity).get(TextEditorActivityViewModel.class);
            viewModel.setSearchResultIndices(data);

            for (SearchResultIndex searchResultIndex : data) {
              textEditorActivity.unhighlightSearchResult(searchResultIndex);
            }

            if (data.size() != 0) {
              textEditorActivity.upButton.setEnabled(true);
              textEditorActivity.downButton.setEnabled(true);

              // downButton
              textEditorActivity.onClick(textEditorActivity.downButton);
            } else {
              textEditorActivity.upButton.setEnabled(false);
              textEditorActivity.downButton.setEnabled(false);
            }
          };

      searchTextTask =
          new SearchTextTask(
              mainTextView.getText().toString(),
              editable.toString(),
              onProgressUpdate,
              onAsyncTaskFinished);
      searchTextTask.execute();
    }
  }

  /** show search view with a circular reveal animation */
  private void revealSearchView() {
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
  private void hideSearchView() {
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
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    switch (v.getId()) {
      case R.id.prev:
        // upButton
        if (viewModel.getCurrent() > 0) {
          unhighlightCurrentSearchResult(viewModel);

          // highlighting previous element in list
          viewModel.setCurrent(viewModel.getCurrent() - 1);

          highlightCurrentSearchResult(viewModel);
        }
        break;
      case R.id.next:
        // downButton
        if (viewModel.getCurrent() < viewModel.getSearchResultIndices().size() - 1) {
          unhighlightCurrentSearchResult(viewModel);

          viewModel.setCurrent(viewModel.getCurrent() + 1);

          highlightCurrentSearchResult(viewModel);
        }
        break;
      case R.id.close:
        // closeButton
        findViewById(R.id.searchview).setVisibility(View.GONE);
        cleanSpans(viewModel);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  private void unhighlightCurrentSearchResult(final TextEditorActivityViewModel viewModel) {
    if (viewModel.getCurrent() == -1) {
      return;
    }

    SearchResultIndex resultIndex = viewModel.getSearchResultIndices().get(viewModel.getCurrent());
    unhighlightSearchResult(resultIndex);
  }

  private void highlightCurrentSearchResult(final TextEditorActivityViewModel viewModel) {
    SearchResultIndex keyValueNew = viewModel.getSearchResultIndices().get(viewModel.getCurrent());
    colorSearchResult(keyValueNew, Utils.getColor(this, R.color.search_text_highlight));

    // scrolling to the highlighted element
    scrollView.scrollTo(
        0,
        (Integer) keyValueNew.getLineNumber()
            + mainTextView.getLineHeight()
            + Math.round(mainTextView.getLineSpacingExtra())
            - getSupportActionBar().getHeight());
  }

  private void unhighlightSearchResult(SearchResultIndex resultIndex) {
    @ColorInt int color;
    if (getAppTheme().equals(AppTheme.LIGHT)) {
      color = Color.YELLOW;
    } else {
      color = Color.LTGRAY;
    }

    colorSearchResult(resultIndex, color);
  }

  private void colorSearchResult(SearchResultIndex resultIndex, @ColorInt int color) {
    mainTextView
        .getText()
        .setSpan(
            new BackgroundColorSpan(color),
            (Integer) resultIndex.getStartCharNumber(),
            (Integer) resultIndex.getEndCharNumber(),
            Spanned.SPAN_INCLUSIVE_INCLUSIVE);
  }

  private void cleanSpans(TextEditorActivityViewModel viewModel) {
    // resetting current highlight and line number
    viewModel.setSearchResultIndices(Collections.emptyList());
    viewModel.setCurrent(-1);
    viewModel.setLine(0);

    // clearing textView spans
    BackgroundColorSpan[] colorSpans =
        mainTextView.getText().getSpans(0, mainTextView.length(), BackgroundColorSpan.class);
    for (BackgroundColorSpan colorSpan : colorSpans) {
      mainTextView.getText().removeSpan(colorSpan);
    }
  }
}
