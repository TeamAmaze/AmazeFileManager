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

package com.amaze.filemanager.ui.views.appbar;

import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CHANGEPATHS;

import java.util.ArrayList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.fragments.CompressedExplorerFragment;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.TabFragment;
import com.amaze.filemanager.utils.BottomBarButtonPath;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.Utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

/**
 * layout_appbar.xml contains the layout for AppBar and BottomBar
 *
 * <p>BottomBar, it lays under the toolbar, used to show data of what is being displayed in the
 * MainFragment, for example directory, folder and file amounts, etc.
 *
 * @author Emmanuel on 2/8/2017, at 23:31.
 */
public class BottomBar implements View.OnTouchListener {
  private static final int PATH_ANIM_START_DELAY = 0;
  private static final int PATH_ANIM_END_DELAY = 0;

  private final MainActivity mainActivity;
  private final AppBar appbar;
  private String newPath;

  private final FrameLayout frame;
  private final LinearLayout pathLayout, buttons;
  private final HorizontalScrollView scroll, pathScroll;
  private final AppCompatTextView pathText, fullPathText, fullPathAnim;

  private final LinearLayout.LayoutParams buttonParams;
  private final AppCompatImageButton buttonRoot, buttonStorage;
  private final ArrayList<AppCompatImageView> arrowButtons = new ArrayList<>();
  private int lastUsedArrowButton = 0;
  private final ArrayList<AppCompatButton> folderButtons = new ArrayList<>();
  private int lastUsedFolderButton = 0;
  private final Drawable arrow;

  private final CountDownTimer timer;
  private final GestureDetector gestureDetector;

  public BottomBar(AppBar appbar, MainActivity mainActivity) {
    this.mainActivity = mainActivity;
    this.appbar = appbar;

    frame = mainActivity.findViewById(R.id.buttonbarframe);

    scroll = mainActivity.findViewById(R.id.scroll);
    buttons = mainActivity.findViewById(R.id.buttons);

    pathLayout = mainActivity.findViewById(R.id.pathbar);
    pathScroll = mainActivity.findViewById(R.id.scroll1);
    fullPathText = mainActivity.findViewById(R.id.fullpath);
    fullPathAnim = mainActivity.findViewById(R.id.fullpath_anim);

    pathText = mainActivity.findViewById(R.id.pathname);

    scroll.setSmoothScrollingEnabled(true);
    pathScroll.setSmoothScrollingEnabled(true);

    pathScroll.setOnKeyListener(
        (v, keyCode, event) -> {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
              this.mainActivity.findViewById(R.id.content_frame).requestFocus();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
              this.mainActivity.getDrawer().getDonateImageView().requestFocus();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
              this.mainActivity.onBackPressed();
            } else {
              return false;
            }
          }
          return true;
        });

    buttonParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    buttonParams.gravity = Gravity.CENTER_VERTICAL;

    buttonRoot = new AppCompatImageButton(mainActivity);
    buttonRoot.setBackgroundColor(Color.TRANSPARENT);
    buttonRoot.setLayoutParams(buttonParams);

    buttonStorage = new AppCompatImageButton(mainActivity);
    buttonStorage.setImageDrawable(getDrawable(R.drawable.ic_sd_storage_white_24dp));
    buttonStorage.setBackgroundColor(Color.TRANSPARENT);
    buttonStorage.setLayoutParams(buttonParams);

    arrow = getDrawable(R.drawable.ic_keyboard_arrow_right_white_24dp);

    timer =
        new CountDownTimer(5000, 1000) {
          @Override
          public void onTick(long l) {}

          @Override
          public void onFinish() {
            FileUtils.crossfadeInverse(buttons, pathLayout);
          }
        };

    gestureDetector =
        new GestureDetector(
            mainActivity.getApplicationContext(),
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(@NonNull MotionEvent e) {
                return true;
              }

              @Override
              public boolean onSingleTapConfirmed(MotionEvent e) {
                Fragment fragmentAtFrame = BottomBar.this.mainActivity.getFragmentAtFrame();
                if (fragmentAtFrame instanceof TabFragment) {
                  final MainFragment mainFragment =
                      BottomBar.this.mainActivity.getCurrentMainFragment();

                  if (mainFragment != null
                      && mainFragment.getMainFragmentViewModel() != null
                      && OpenMode.CUSTOM != mainFragment.getMainFragmentViewModel().getOpenMode()) {
                    FileUtils.crossfade(buttons, pathLayout);
                    timer.cancel();
                    timer.start();
                    showButtons(mainFragment);
                  }
                } else if (fragmentAtFrame instanceof CompressedExplorerFragment) {
                  FileUtils.crossfade(buttons, pathLayout);
                  timer.cancel();
                  timer.start();
                  showButtons((BottomBarButtonPath) fragmentAtFrame);
                }
                return false;
              }

              @Override
              public void onLongPress(@NonNull MotionEvent e) {
                final MainFragment mainFragment =
                    BottomBar.this.mainActivity.getCurrentMainFragment();
                if (BottomBar.this.mainActivity.getBoolean(PREFERENCE_CHANGEPATHS)
                    && ((mainFragment != null
                            && mainFragment.getMainFragmentViewModel() != null
                            && !mainFragment.getMainFragmentViewModel().getResults())
                        || buttons.getVisibility() == View.VISIBLE)) {
                  GeneralDialogCreation.showChangePathsDialog(
                      BottomBar.this.mainActivity, BottomBar.this.mainActivity.getPrefs());
                }
              }
            });
  }

  @SuppressLint("ClickableViewAccessibility")
  public void setClickListener() {
    // TODO: 15/8/2017 this is a horrible hack, if you see this, correct it
    frame.setOnTouchListener(this);
    scroll.setOnTouchListener(this);
    buttons.setOnTouchListener(this);
    pathLayout.setOnTouchListener(this);
    pathScroll.setOnTouchListener(this);
    fullPathText.setOnTouchListener(this);
    pathText.setOnTouchListener(this);
    scroll.setOnTouchListener(this);
    pathScroll.setOnTouchListener(this);
  }

  @SuppressLint("ClickableViewAccessibility")
  public void resetClickListener() {
    frame.setOnTouchListener(null);
  }

  public void setPathText(String text) {
    pathText.setText(text);
  }

  public void setFullPathText(String text) {
    fullPathText.setText(text);
  }

  public String getFullPathText() {
    return fullPathText.getText().toString();
  }

  public boolean areButtonsShowing() {
    return buttons.getVisibility() == View.VISIBLE;
  }

  public void showButtons(final BottomBarButtonPath buttonPathInterface) {
    final String path = buttonPathInterface.getPath();

    if (buttons.getVisibility() != View.VISIBLE || path == null) return;

    lastUsedArrowButton = 0;
    lastUsedFolderButton = 0;
    buttons.removeAllViews();
    buttons.setMinimumHeight(pathLayout.getHeight());

    buttonRoot.setImageDrawable(getDrawable(buttonPathInterface.getRootDrawable()));

    String[] names = FileUtils.getFolderNamesInPath(path);
    final String[] paths = FileUtils.getPathsInPath(path);
    View view = new View(mainActivity);
    LinearLayout.LayoutParams params1 =
        new LinearLayout.LayoutParams(
            appbar.getToolbar().getContentInsetLeft(), LinearLayout.LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params1);
    buttons.addView(view);

    for (int i = 0; i < names.length; i++) {
      final int k = i;
      if (i == 0) {
        buttonRoot.setOnClickListener(
            p1 -> {
              if (paths.length != 0) {
                buttonPathInterface.changePath(paths[k]);
                timer.cancel();
                timer.start();
              }
            });
        buttons.addView(buttonRoot);
      } else if (FileUtils.isStorage(paths[i])) {
        buttonStorage.setOnClickListener(
            p1 -> {
              buttonPathInterface.changePath(paths[k]);
              timer.cancel();
              timer.start();
            });
        buttons.addView(buttonStorage);
      } else {
        AppCompatButton button = createFolderButton(names[i]);
        button.setOnClickListener(
            p1 -> {
              buttonPathInterface.changePath(paths[k]);
              timer.cancel();
              timer.start();
            });
        buttons.addView(button);
      }

      if (names.length - i != 1) {
        buttons.addView(createArrow());
      }
    }

    scroll.post(
        () -> {
          sendScroll(scroll);
          sendScroll(pathScroll);
        });

    if (buttons.getVisibility() == View.VISIBLE) {
      timer.cancel();
      timer.start();
    }
  }

  private AppCompatImageView createArrow() {
    AppCompatImageView buttonArrow;

    if (lastUsedArrowButton >= arrowButtons.size()) {
      buttonArrow = new AppCompatImageView(mainActivity);
      buttonArrow.setImageDrawable(arrow);
      buttonArrow.setLayoutParams(buttonParams);
      arrowButtons.add(buttonArrow);
    } else {
      buttonArrow = arrowButtons.get(lastUsedArrowButton);
    }

    lastUsedArrowButton++;

    return buttonArrow;
  }

  private AppCompatButton createFolderButton(String text) {
    AppCompatButton button;

    if (lastUsedFolderButton >= folderButtons.size()) {
      button = new AppCompatButton(mainActivity);
      button.setTextColor(Utils.getColor(mainActivity, android.R.color.white));
      button.setTextSize(13);
      button.setLayoutParams(buttonParams);
      button.setBackgroundResource(0);
      folderButtons.add(button);
    } else {
      button = folderButtons.get(lastUsedFolderButton);
    }

    button.setText(text);

    lastUsedFolderButton++;

    return button;
  }

  public void setBackgroundColor(@ColorInt int color) {
    frame.setBackgroundColor(color);
  }

  public void setVisibility(int visibility) {
    frame.setVisibility(visibility);
  }

  public void updatePath(
      @NonNull final String news,
      boolean results,
      String query,
      OpenMode openmode,
      int folderCount,
      int fileCount,
      BottomBarButtonPath buttonPathInterface) {

    if (news.length() == 0) return;

    MainActivityHelper mainActivityHelper = mainActivity.mainActivityHelper;

    switch (openmode) {
      case SFTP:
      case SMB:
      case FTP:
        newPath = HybridFile.parseAndFormatUriForDisplay(news);
        break;
      case OTG:
        newPath = mainActivityHelper.parseOTGPath(news);
        break;
      case CUSTOM:
        newPath = mainActivityHelper.getIntegralNames(news);
        break;
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
        newPath = mainActivityHelper.parseCloudPath(openmode, news);
        break;
      default:
        newPath = news;
    }

    if (!results) {
      pathText.setText(mainActivity.getString(R.string.folderfilecount, folderCount, fileCount));
    } else {
      fullPathText.setText(mainActivity.getString(R.string.search_results, query));
      pathText.setText("");
      return;
    }

    final String oldPath = fullPathText.getText().toString();

    if (oldPath.equals(newPath)) return;

    if (!areButtonsShowing()) {
      final Animation slideIn = AnimationUtils.loadAnimation(mainActivity, R.anim.slide_in);
      Animation slideOut = AnimationUtils.loadAnimation(mainActivity, R.anim.slide_out);

      if (newPath.length() > oldPath.length()
          && newPath.contains(oldPath)
          && oldPath.length() != 0) {
        // navigate forward
        fullPathAnim.setAnimation(slideIn);
        fullPathAnim
            .animate()
            .setListener(
                new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    new Handler()
                        .postDelayed(
                            () -> {
                              fullPathAnim.setVisibility(View.GONE);
                              fullPathText.setText(newPath);
                            },
                            PATH_ANIM_END_DELAY);
                  }

                  @Override
                  public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathAnim.setText(Utils.differenceStrings(oldPath, newPath));
                    // fullPathText.setText(oldPath);

                    scroll.post(() -> pathScroll.fullScroll(View.FOCUS_RIGHT));
                  }

                  @Override
                  public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    // onAnimationEnd(animation);
                  }
                })
            .setStartDelay(PATH_ANIM_START_DELAY)
            .start();
      } else if (newPath.length() < oldPath.length() && oldPath.contains(newPath)) {
        // navigate backwards
        fullPathAnim.setAnimation(slideOut);
        fullPathAnim
            .animate()
            .setListener(
                new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    fullPathAnim.setVisibility(View.GONE);
                    fullPathText.setText(newPath);

                    scroll.post(() -> pathScroll.fullScroll(View.FOCUS_RIGHT));
                  }

                  @Override
                  public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathAnim.setText(Utils.differenceStrings(newPath, oldPath));
                    fullPathText.setText(newPath);

                    scroll.post(() -> pathScroll.fullScroll(View.FOCUS_LEFT));
                  }
                })
            .setStartDelay(PATH_ANIM_START_DELAY)
            .start();
      } else if (oldPath.isEmpty()) {
        // case when app starts
        fullPathAnim.setAnimation(slideIn);
        fullPathAnim.setText(newPath);
        fullPathAnim
            .animate()
            .setListener(
                new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathText.setText("");
                    scroll.post(() -> pathScroll.fullScroll(View.FOCUS_RIGHT));
                  }

                  @Override
                  public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    new Handler()
                        .postDelayed(
                            () -> {
                              fullPathAnim.setVisibility(View.GONE);
                              fullPathText.setText(newPath);
                            },
                            PATH_ANIM_END_DELAY);
                  }

                  @Override
                  public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    // onAnimationEnd(animation);
                  }
                })
            .setStartDelay(PATH_ANIM_START_DELAY)
            .start();
      } else {
        // completely different path
        // first slide out of old path followed by slide in of new path
        fullPathAnim.setAnimation(slideOut);
        fullPathAnim
            .animate()
            .setListener(
                new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationStart(Animator animator) {
                    super.onAnimationStart(animator);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathAnim.setText(oldPath);
                    fullPathText.setText("");

                    scroll.post(() -> pathScroll.fullScroll(View.FOCUS_LEFT));
                  }

                  @Override
                  public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);

                    // fullPathAnim.setVisibility(View.GONE);
                    fullPathAnim.setText(newPath);
                    fullPathText.setText("");
                    fullPathAnim.setAnimation(slideIn);

                    fullPathAnim
                        .animate()
                        .setListener(
                            new AnimatorListenerAdapter() {
                              @Override
                              public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                new Handler()
                                    .postDelayed(
                                        () -> {
                                          fullPathAnim.setVisibility(View.GONE);
                                          fullPathText.setText(newPath);
                                        },
                                        PATH_ANIM_END_DELAY);
                              }

                              @Override
                              public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                // we should not be having anything here in path bar
                                fullPathAnim.setVisibility(View.VISIBLE);
                                fullPathText.setText("");
                                scroll.post(() -> pathScroll.fullScroll(View.FOCUS_RIGHT));
                              }
                            })
                        .start();
                  }

                  @Override
                  public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    // onAnimationEnd(animation);
                  }
                })
            .setStartDelay(PATH_ANIM_START_DELAY)
            .start();
      }
    } else {
      showButtons(buttonPathInterface);
      fullPathText.setText(newPath);
    }
  }

  private void sendScroll(final HorizontalScrollView scrollView) {
    new Handler().postDelayed(() -> scrollView.fullScroll(View.FOCUS_RIGHT), 100);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  private Drawable getDrawable(int id) {
    return ResourcesCompat.getDrawable(mainActivity.getResources(), id, null);
  }
}
