/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;

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
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
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

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

public class TextEditorActivity extends ThemedActivity
    implements TextWatcher, View.OnClickListener {

  public AppCompatEditText mainTextView;
  public AppCompatEditText searchEditText;
  private Typeface inputTypefaceDefault;
  private Typeface inputTypefaceMono;
  private androidx.appcompat.widget.Toolbar toolbar;
  ScrollView scrollView;
  private WebView markdownWebView;

  private SearchTextTask searchTextTask;
  private static final String KEY_MODIFIED_TEXT = "modified";
  private static final String KEY_INDEX = "index";
  private static final String KEY_ORIGINAL_TEXT = "original";
  private static final String KEY_MONOFONT = "monofont";
  private static final String KEY_MARKDOWN_PREVIEW = "markdown_preview";

  private ConstraintLayout searchViewLayout;
  public AppCompatImageButton upButton;
  public AppCompatImageButton downButton;

  private Snackbar loadingSnackbar;

  private TextEditorActivityViewModel viewModel;

  /** Scroll listener reference for windowed mode (so it can be removed if needed). */
  private ViewTreeObserver.OnScrollChangedListener windowedScrollListener;

  /** Pre-draw listener used to position a freshly loaded window before it is first drawn. */
  private ViewTreeObserver.OnPreDrawListener pendingWindowApplyPreDrawListener;

  /** True while replacing window content and restoring scroll programmatically. */
  private boolean isApplyingWindowContent;

  /** Suppress edge-triggered loads briefly after programmatic scroll changes. */
  private long suppressWindowLoadsUntilMs;

  /**
   * Duration (ms) to suppress edge-triggered window loads after a programmatic scroll change. Must
   * be long enough to cover the layout pass after setText() + the scroll restoration; 400ms is a
   * safe margin on most devices.
   */
  private static final long WINDOW_LOAD_SUPPRESSION_MS = 400L;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    viewModel = new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    searchViewLayout = findViewById(R.id.textEditorSearchBar);

    searchViewLayout.setBackgroundColor(getPrimary());

    searchEditText = searchViewLayout.findViewById(R.id.textEditorSearchBox);
    upButton = searchViewLayout.findViewById(R.id.textEditorSearchPrevButton);
    downButton = searchViewLayout.findViewById(R.id.textEditorSearchNextButton);

    searchEditText.addTextChangedListener(this);

    upButton.setOnClickListener(this);
    // upButton.setEnabled(false);
    downButton.setOnClickListener(this);
    // downButton.setEnabled(false);

    if (getSupportActionBar() != null) {
      boolean useNewStack = getBoolean(PREFERENCE_TEXTEDITOR_NEWSTACK);
      getSupportActionBar().setDisplayHomeAsUpEnabled(!useNewStack);
    }
    mainTextView = findViewById(R.id.textEditorMainEditText);
    scrollView = findViewById(R.id.textEditorScrollView);
    markdownWebView = findViewById(R.id.textEditorMarkdownWebView);
    markdownWebView.getSettings().setJavaScriptEnabled(false);

    final Uri uri = getIntent().getData();
    if (uri != null) {
      viewModel.setFile(new EditableFileAbstraction(this, uri));
    } else {
      Toast.makeText(this, R.string.no_file_error, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    ActionBar actionBar = getSupportActionBar();

    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(!getBoolean(PREFERENCE_TEXTEDITOR_NEWSTACK));
      actionBar.setTitle(viewModel.getFile().name);
    }

    mainTextView.addTextChangedListener(this);

    if (getAppTheme().equals(AppTheme.DARK)) {
      mainTextView.setBackgroundColor(Utils.getColor(this, R.color.holo_dark_action_mode));
      mainTextView.setTextColor(Utils.getColor(this, R.color.primary_white));
    } else if (getAppTheme().equals(AppTheme.BLACK)) {
      mainTextView.setBackgroundColor(Utils.getColor(this, android.R.color.black));
      mainTextView.setTextColor(Utils.getColor(this, R.color.primary_white));
    } else {
      mainTextView.setTextColor(Utils.getColor(this, R.color.primary_grey_900));
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
      // Restore markdown preview state
      if (savedInstanceState.getBoolean(KEY_MARKDOWN_PREVIEW, false)) {
        viewModel.setMarkdownPreviewEnabled(true);
        toggleMarkdownPreview(true);
      }
      // Restore windowed mode state after rotation
      if (viewModel.isWindowed()) {
        setReadOnly();
        initWindowedScrollListener();
      }
    } else {
      load(this);
    }
    initStatusBarResources(findViewById(R.id.textEditorRootView));

    // Observe windowed-mode LiveData for new window content
    observeWindowContent();
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    outState.putString(
        KEY_MODIFIED_TEXT, mainTextView.getText() != null ? mainTextView.getText().toString() : "");
    outState.putInt(KEY_INDEX, mainTextView.getScrollY());
    outState.putString(KEY_ORIGINAL_TEXT, viewModel.getOriginal());
    outState.putBoolean(KEY_MONOFONT, inputTypefaceMono.equals(mainTextView.getTypeface()));
    outState.putBoolean(KEY_MARKDOWN_PREVIEW, viewModel.getMarkdownPreviewEnabled());
  }

  private void checkUnsavedChanges() {
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    // In windowed mode, the file is read-only — no unsaved changes possible
    if (viewModel.isWindowed()) {
      finish();
      return;
    }

    if (viewModel.getOriginal() != null
        && mainTextView.isShown()
        && mainTextView.getText() != null
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

    boolean windowed = viewModel.isWindowed();

    // Hide save in windowed mode; otherwise show based on modification state
    menu.findItem(R.id.save).setVisible(!windowed && viewModel.getModified());

    // Hide search in windowed mode (search only works on in-memory text)
    menu.findItem(R.id.find).setVisible(!windowed);

    // Show markdown preview item only for .md/.markdown files
    MenuItem markdownItem = menu.findItem(R.id.markdown_preview);
    markdownItem.setVisible(isMarkdownFile());
    markdownItem.setChecked(viewModel.getMarkdownPreviewEnabled());

    menu.findItem(R.id.monofont).setChecked(inputTypefaceMono.equals(mainTextView.getTypeface()));
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);
    final EditableFileAbstraction editableFileAbstraction = viewModel.getFile();

    if (item.getItemId() == android.R.id.home) {
      checkUnsavedChanges();
    } else if (item.getItemId() == R.id.save) {
      // Make sure EditText is visible before saving!
      if (mainTextView.getText() != null) {
        saveFile(this, mainTextView.getText().toString());
      }
    } else if (item.getItemId() == R.id.details) {
      if (editableFileAbstraction.scheme.equals(FILE)
          && editableFileAbstraction.hybridFileParcelable.getFile() != null
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
    } else if (item.getItemId() == R.id.openwith) {
      if (editableFileAbstraction != null && editableFileAbstraction.scheme.equals(FILE)) {
        File currentFile = editableFileAbstraction.hybridFileParcelable.getFile();
        if (currentFile != null && currentFile.exists()) {
          boolean useNewStack = getBoolean(PREFERENCE_TEXTEDITOR_NEWSTACK);
          FileUtils.openWith(currentFile, this, useNewStack);
        } else {
          Toast.makeText(this, R.string.not_allowed, Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, R.string.reopen_from_source, Toast.LENGTH_SHORT).show();
      }
    } else if (item.getItemId() == R.id.find) {
      if (searchViewLayout.isShown()) hideSearchView();
      else revealSearchView();
    } else if (item.getItemId() == R.id.monofont) {
      item.setChecked(!item.isChecked());
      mainTextView.setTypeface(item.isChecked() ? inputTypefaceMono : inputTypefaceDefault);
    } else if (item.getItemId() == R.id.markdown_preview) {
      boolean newState = !item.isChecked();
      item.setChecked(newState);
      viewModel.setMarkdownPreviewEnabled(newState);
      toggleMarkdownPreview(newState);
    } else {
      return false;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onDestroy() {
    clearPendingWindowApplyPreDrawListener();
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
    if (searchEditText.getText() != null
        && charSequence.hashCode() == searchEditText.getText().hashCode()) {
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
    if (mainTextView.getText() != null
        && charSequence.hashCode() == mainTextView.getText().hashCode()) {
      final TextEditorActivityViewModel viewModel =
          new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

      // Skip modification tracking in windowed mode (text changes are window loads, not edits)
      if (viewModel.isWindowed()) return;

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
                  textEditorActivity.mainTextView.getText() != null
                      && !textEditorActivity
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
    if (searchEditText.getText() != null
        && editable.hashCode() == searchEditText.getText().hashCode()) {
      final WeakReference<TextEditorActivity> textEditorActivityWR = new WeakReference<>(this);

      final OnProgressUpdate<SearchResultIndex> onProgressUpdate =
          index -> {
            final TextEditorActivity textEditorActivity = textEditorActivityWR.get();
            if (textEditorActivity == null) {
              return;
            }
            textEditorActivity.colorSearchResult(index, getPrimary());
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
              textEditorActivity.colorSearchResult(searchResultIndex, getPrimary());
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

      if (mainTextView.getText() != null) {
        searchTextTask =
            new SearchTextTask(
                mainTextView.getText().toString(),
                editable.toString(),
                onProgressUpdate,
                onAsyncTaskFinished);
        searchTextTask.execute();
      }
    }
  }

  private void revealSearchView() {

    searchViewLayout.setVisibility(View.VISIBLE);

    Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_top);

    animation.setAnimationListener(
        new Animation.AnimationListener() {
          @Override
          public void onAnimationStart(Animation animation) {}

          @Override
          public void onAnimationEnd(Animation animation) {

            searchEditText.requestFocus();

            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
          }

          @Override
          public void onAnimationRepeat(Animation animation) {}
        });

    searchViewLayout.startAnimation(animation);
  }

  private void hideSearchView() {

    Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out_top);

    animation.setAnimationListener(
        new Animation.AnimationListener() {
          @Override
          public void onAnimationStart(Animation animation) {}

          @Override
          public void onAnimationEnd(Animation animation) {

            searchViewLayout.setVisibility(View.GONE);

            cleanSpans(viewModel);
            searchEditText.setText("");

            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(
                    searchEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
          }

          @Override
          public void onAnimationRepeat(Animation animation) {}
        });

    searchViewLayout.startAnimation(animation);
  }

  @Override
  public void onClick(View v) {
    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(this).get(TextEditorActivityViewModel.class);

    if (v.getId() == R.id.textEditorSearchPrevButton) {
      // upButton
      if (viewModel.getCurrent() > 0) {
        unhighlightCurrentSearchResult(viewModel);

        // highlighting previous element in list
        viewModel.setCurrent(viewModel.getCurrent() - 1);

        highlightCurrentSearchResult(viewModel);
      }
    } else if (v.getId() == R.id.textEditorSearchNextButton) {
      // downButton
      if (viewModel.getCurrent() < viewModel.getSearchResultIndices().size() - 1) {
        unhighlightCurrentSearchResult(viewModel);

        viewModel.setCurrent(viewModel.getCurrent() + 1);

        highlightCurrentSearchResult(viewModel);
      }
    } else {
      throw new IllegalStateException();
    }
  }

  private void unhighlightCurrentSearchResult(final TextEditorActivityViewModel viewModel) {
    if (viewModel.getCurrent() == -1) {
      return;
    }

    SearchResultIndex resultIndex = viewModel.getSearchResultIndices().get(viewModel.getCurrent());
    colorSearchResult(resultIndex, getPrimary());
  }

  private void highlightCurrentSearchResult(final TextEditorActivityViewModel viewModel) {
    SearchResultIndex keyValueNew = viewModel.getSearchResultIndices().get(viewModel.getCurrent());
    colorSearchResult(keyValueNew, getAccent());

    // scrolling to the highlighted element
    if (getSupportActionBar() != null) {
      scrollView.scrollTo(
          0,
          (Integer) keyValueNew.getLineNumber()
              + mainTextView.getLineHeight()
              + Math.round(mainTextView.getLineSpacingExtra())
              - getSupportActionBar().getHeight());
    }
  }

  private void colorSearchResult(SearchResultIndex resultIndex, @ColorInt int color) {
    if (mainTextView.getText() != null) {
      mainTextView
          .getText()
          .setSpan(
              new BackgroundColorSpan(color),
              (Integer) resultIndex.getStartCharNumber(),
              (Integer) resultIndex.getEndCharNumber(),
              Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }
  }

  private void cleanSpans(TextEditorActivityViewModel viewModel) {
    // resetting current highlight and line number
    viewModel.setSearchResultIndices(Collections.emptyList());
    viewModel.setCurrent(-1);
    viewModel.setLine(0);

    // clearing textView spans
    if (mainTextView.getText() != null) {
      BackgroundColorSpan[] colorSpans =
          mainTextView.getText().getSpans(0, mainTextView.length(), BackgroundColorSpan.class);
      for (BackgroundColorSpan colorSpan : colorSpans) {
        mainTextView.getText().removeSpan(colorSpan);
      }
    }
  }

  // ── Sliding Window Helpers ──────────────────────────────────────────

  /**
   * Observe the ViewModel's windowContent LiveData. When a new window is loaded, replace the
   * EditText content and adjust the scroll position for visual continuity.
   */
  private void observeWindowContent() {
    viewModel
        .getWindowContent()
        .observe(
            this,
            result -> {
              if (result == null) return;

              // Capture the currently visible text as an anchor before replacing content
              String anchorText = null;
              int oldScrollY = scrollView.getScrollY();
              int viewportHeight = scrollView.getHeight();

              Layout oldLayout = mainTextView.getLayout();
              if (oldLayout != null && mainTextView.getText() != null) {
                // Find the line at the middle of the viewport
                int anchorY = oldScrollY + viewportHeight / 2;
                int anchorLine = oldLayout.getLineForVertical(anchorY);

                if (anchorLine >= 0 && anchorLine < oldLayout.getLineCount()) {
                  int lineStart = oldLayout.getLineStart(anchorLine);
                  int lineEnd = oldLayout.getLineEnd(anchorLine);
                  if (lineStart < lineEnd && lineEnd <= mainTextView.getText().length()) {
                    // Get a distinctive snippet (up to 80 chars) from this line
                    int snippetEnd = Math.min(lineEnd, lineStart + 80);
                    anchorText =
                        mainTextView.getText().subSequence(lineStart, snippetEnd).toString();
                  }
                }
              }

              // Replace text (TextWatcher will fire but windowed-mode guard skips modification
              // tracking)
              final String savedAnchorText = anchorText;
              final TextEditorActivityViewModel.Direction lastDirection =
                  viewModel.getLastLoadDirection();
              isApplyingWindowContent = true;
              suppressWindowLoadsUntilMs = SystemClock.uptimeMillis() + WINDOW_LOAD_SUPPRESSION_MS;
              clearPendingWindowApplyPreDrawListener();
              mainTextView.setText(result.getText());

              pendingWindowApplyPreDrawListener =
                  () -> {
                    Layout newLayout = mainTextView.getLayout();
                    if (newLayout == null) {
                      return true; // layout not ready yet, let the draw pass proceed
                    }

                    int targetY =
                        resolveWindowedTargetScrollY(
                            newLayout, savedAnchorText, viewportHeight, lastDirection);

                    suppressWindowLoadsUntilMs =
                        SystemClock.uptimeMillis() + WINDOW_LOAD_SUPPRESSION_MS;
                    scrollView.scrollTo(0, targetY);
                    clearPendingWindowApplyPreDrawListener();
                    scrollView.post(() -> isApplyingWindowContent = false);
                    invalidateOptionsMenu();
                    return true; // proceed with this draw pass using the corrected scroll
                  };

              ViewTreeObserver observer = scrollView.getViewTreeObserver();
              if (observer.isAlive()) {
                observer.addOnPreDrawListener(pendingWindowApplyPreDrawListener);
              } else {
                isApplyingWindowContent = false;
              }
            });
  }

  private void clearPendingWindowApplyPreDrawListener() {
    if (pendingWindowApplyPreDrawListener == null) return;

    ViewTreeObserver observer = scrollView.getViewTreeObserver();
    if (observer.isAlive()) {
      observer.removeOnPreDrawListener(pendingWindowApplyPreDrawListener);
    }
    pendingWindowApplyPreDrawListener = null;
  }

  private int resolveWindowedTargetScrollY(
      Layout newLayout,
      String savedAnchorText,
      int viewportHeight,
      TextEditorActivityViewModel.Direction lastDirection) {
    if (savedAnchorText != null && mainTextView.getText() != null) {
      String newText = mainTextView.getText().toString();
      int anchorIndex = findAnchorNearExpectedPosition(newText, savedAnchorText, lastDirection);

      if (anchorIndex >= 0) {
        int anchorLine = newLayout.getLineForOffset(anchorIndex);
        int anchorLineTop = newLayout.getLineTop(anchorLine);
        return Math.max(0, anchorLineTop - viewportHeight / 2);
      }
    }

    return inferScrollPositionFallback(newLayout, viewportHeight, lastDirection);
  }

  /**
   * Find the anchor text in the new content, preferring the occurrence closest to where it is
   * expected given the load direction and 60% overlap.
   *
   * <p>For a FORWARD load the old viewport-center content should end up in roughly the first 30–40%
   * of the new window (because 60% overlaps). For a BACKWARD load it should be in the last 30–40%.
   * We estimate an expected char offset and pick the occurrence nearest to it.
   *
   * <p>This avoids the problem with naive {@code indexOf}/{@code lastIndexOf} picking a wrong
   * duplicate occurrence in files with many repeated lines (e.g. log files).
   */
  private static int findAnchorNearExpectedPosition(
      String newText, String anchor, TextEditorActivityViewModel.Direction direction) {
    if (newText.isEmpty() || anchor.isEmpty()) return -1;

    // Estimate where in the new text the anchor should be
    int expectedPos;
    if (direction == TextEditorActivityViewModel.Direction.FORWARD) {
      // After a 40% forward shift with 60% overlap, the old viewport center
      // (≈50% of old window) maps to ≈ (50%-40%) / (100%) ≈ first 10-30% of new window
      expectedPos = (int) (newText.length() * 0.20);
    } else {
      // After a 40% backward shift, the old viewport center maps to ≈ last 70-80%
      expectedPos = (int) (newText.length() * 0.80);
    }

    int bestIndex = -1;
    int bestDistance = Integer.MAX_VALUE;
    int searchFrom = 0;

    while (searchFrom <= newText.length() - anchor.length()) {
      int idx = newText.indexOf(anchor, searchFrom);
      if (idx < 0) break;

      int distance = Math.abs(idx - expectedPos);
      if (distance < bestDistance) {
        bestDistance = distance;
        bestIndex = idx;
      }
      searchFrom = idx + 1;
    }

    return bestIndex;
  }

  /**
   * Fallback method when anchor text is not found. Keep the viewport in the middle band so edge
   * thresholds do not immediately trigger another opposite-direction window load.
   */
  private int inferScrollPositionFallback(
      Layout newLayout, int viewportHeight, TextEditorActivityViewModel.Direction direction) {
    int targetLine = newLayout.getLineCount() / 2;
    if (direction == TextEditorActivityViewModel.Direction.BACKWARD) {
      targetLine = (newLayout.getLineCount() * 55) / 100;
    } else if (direction == TextEditorActivityViewModel.Direction.FORWARD) {
      targetLine = (newLayout.getLineCount() * 45) / 100;
    }
    return Math.max(0, newLayout.getLineTop(targetLine) - viewportHeight / 2);
  }

  /**
   * Called by ReadTextFileTask after initializing windowed mode. Sets up a scroll listener that
   * triggers window loads when the user scrolls near the top or bottom edge.
   */
  public void initWindowedScrollListener() {
    if (windowedScrollListener != null) return; // already initialized

    windowedScrollListener =
        () -> {
          if (!viewModel.isWindowed()) return;
          if (isApplyingWindowContent) return;
          if (SystemClock.uptimeMillis() < suppressWindowLoadsUntilMs) return;

          int scrollY = scrollView.getScrollY();
          int viewportHeight = scrollView.getHeight();
          int contentHeight = mainTextView.getHeight();

          if (contentHeight <= 0 || viewportHeight <= 0) return;

          // Threshold: 20% of viewport
          int threshold = viewportHeight / 5;

          int distanceFromBottom = contentHeight - scrollY - viewportHeight;
          int distanceFromTop = scrollY;

          if (distanceFromBottom < threshold) {
            viewModel.loadWindow(TextEditorActivityViewModel.Direction.FORWARD);
          } else if (distanceFromTop < threshold) {
            viewModel.loadWindow(TextEditorActivityViewModel.Direction.BACKWARD);
          }
        };

    scrollView.getViewTreeObserver().addOnScrollChangedListener(windowedScrollListener);
  }

  // ── Markdown Preview Helpers ────────────────────────────────────────

  /** Returns true if the currently opened file has a Markdown extension (.md or .markdown). */
  private boolean isMarkdownFile() {
    EditableFileAbstraction file = viewModel.getFile();
    if (file == null) return false;
    return MarkdownHtmlGenerator.isMarkdownFile(file.name);
  }

  /**
   * Toggle between Markdown preview (WebView) and the normal EditText editor.
   *
   * @param enabled true to show the WebView with rendered Markdown; false to show EditText
   */
  private void toggleMarkdownPreview(boolean enabled) {
    if (enabled) {
      renderMarkdownToWebView();
      scrollView.setVisibility(View.GONE);
      markdownWebView.setVisibility(View.VISIBLE);
    } else {
      markdownWebView.setVisibility(View.GONE);
      scrollView.setVisibility(View.VISIBLE);
    }
    invalidateOptionsMenu();
  }

  /**
   * Parse the current EditText content as Markdown using commonmark, render to HTML, and load it
   * into the WebView.
   */
  private void renderMarkdownToWebView() {
    String markdownSource = "";
    if (mainTextView.getText() != null) {
      markdownSource = mainTextView.getText().toString();
    }

    String bodyHtml = MarkdownHtmlGenerator.renderToHtml(markdownSource);
    boolean isDark = getAppTheme().equals(AppTheme.DARK) || getAppTheme().equals(AppTheme.BLACK);
    String fullHtml = MarkdownHtmlGenerator.wrapWithBaseHtml(bodyHtml, isDark);
    markdownWebView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
  }
}
