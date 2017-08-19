package com.amaze.filemanager.ui.views.appbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.files.Futils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

/**
 * layout_appbar.xml contains the layout for AppBar and BottomBar
 *
 * BottomBar, it lays under the toolbar, used to show data of what is being displayed in the MainFragment,
 * for example directory, folder and file amounts, etc.
 *
 * @author Emmanuel
 *         on 2/8/2017, at 23:31.
 */

public class BottomBar implements View.OnTouchListener{
    private static final int PATH_ANIM_START_DELAY = 0;
    private static final int PATH_ANIM_END_DELAY = 0;

    private WeakReference<MainActivity> mainActivity;
    private AppBar appbar;
    private String newPath;

    private FrameLayout frame;
    private LinearLayout pathLayout;
    private LinearLayout buttons;
    private HorizontalScrollView scroll, pathScroll;
    private TextView pathText, fullPathText, fullPathAnim;

    private LinearLayout.LayoutParams buttonParams;
    private ImageButton buttonRoot;
    private ImageButton buttonStorage;
    private ArrayList<ImageView> arrowButtons = new ArrayList<>();
    private int lastUsedArrowButton = 0;
    private ArrayList<Button> folderButtons = new ArrayList<>();
    private int lastUsedFolderButton = 0;
    private Drawable arrow;

    private CountDownTimer timer;
    private boolean allowChangePaths;
    private GestureDetector gestureDetector;

    public BottomBar(AppBar appbar, MainActivity a) {
        mainActivity = new WeakReference<>(a);
        this.appbar = appbar;

        frame = (FrameLayout) a.findViewById(R.id.buttonbarframe);

        scroll = (HorizontalScrollView) a.findViewById(R.id.scroll);
        buttons = (LinearLayout) a.findViewById(R.id.buttons);

        pathLayout = (LinearLayout) a.findViewById(R.id.pathbar);
        pathScroll = (HorizontalScrollView) a.findViewById(R.id.scroll1);
        fullPathText = (TextView) a.findViewById(R.id.fullpath);
        fullPathAnim = (TextView) a.findViewById(R.id.fullpath_anim);

        pathText = (TextView) a.findViewById(R.id.pathname);

        scroll.setSmoothScrollingEnabled(true);
        pathScroll.setSmoothScrollingEnabled(true);

        buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = Gravity.CENTER_VERTICAL;

        buttonRoot = new ImageButton(a);
        buttonRoot.setImageDrawable(a.getResources().getDrawable(R.drawable.root));
        buttonRoot.setBackgroundColor(Color.TRANSPARENT);
        buttonRoot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                MainFragment m = mainActivity.get().getCurrentMainFragment();
                m.loadlist(("/"), false, m.openMode);
                timer.cancel();
                timer.start();
            }
        });
        buttonRoot.setLayoutParams(buttonParams);

        buttonStorage = new ImageButton(a);
        buttonStorage.setImageDrawable(a.getResources().getDrawable(R.drawable.ic_sd_storage_white_56dp));
        buttonStorage.setBackgroundColor(Color.TRANSPARENT);
        buttonStorage.setLayoutParams(buttonParams);

        arrow = mainActivity.get().getResources().getDrawable(R.drawable.abc_ic_ab_back_holo_dark);

        timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                Futils.crossfadeInverse(buttons, pathLayout);
            }
        };

        allowChangePaths = mainActivity.get().sharedPref.getBoolean(Preffrag.PREFERENCE_CHANGEPATHS, false);

        gestureDetector = new GestureDetector(a.getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                MainFragment m = mainActivity.get().getCurrentMainFragment();
                if (m.openMode == OpenMode.FILE) {
                    Futils.crossfade(buttons, pathLayout);
                    timer.cancel();
                    timer.start();
                    showButtons(m);
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if(allowChangePaths &&
                        (!mainActivity.get().getCurrentMainFragment().results || buttons.getVisibility() == View.VISIBLE)) {
                    GeneralDialogCreation.showChangePathsDialog(mainActivity, mainActivity.get().sharedPref);
                }
            }
        });
    }

    public void setClickListener() {// TODO: 15/8/2017 this is a horrible hack, if you see this, correct it
        frame.setOnTouchListener(this);
        scroll.setOnTouchListener(this);
        buttons.setOnTouchListener(this);
        pathLayout.setOnTouchListener(this);
        pathScroll .setOnTouchListener(this);
        fullPathText.setOnTouchListener(this);
        pathText.setOnTouchListener(this);
        scroll.setOnTouchListener(this);
        pathScroll.setOnTouchListener(this);

    }

    public void resetClickListener() {
        frame.setOnTouchListener(null);
    }

    public void setPathText(String text) {
        pathText.setText(text);
    }

    public void setFullPathText(String text) {
        fullPathText.setText(text);
    }

    public boolean areButtonsShowing() {
        return buttons.getVisibility() == View.VISIBLE;
    }

    public void showButtons(final MainFragment mainFrag) {
        final String path = mainFrag.getCurrentPath();
        if (buttons.getVisibility() == View.VISIBLE) {
            lastUsedArrowButton = 0;
            lastUsedFolderButton = 0;
            buttons.removeAllViews();
            buttons.setMinimumHeight(pathLayout.getHeight());

            Bundle bundle = Futils.getPaths(path, mainActivity.get());
            ArrayList<String> names = bundle.getStringArrayList("names");
            ArrayList<String> rnames = bundle.getStringArrayList("names");
            Collections.reverse(rnames);
            
            final ArrayList<String> rpaths = bundle.getStringArrayList("paths");
            Collections.reverse(rpaths);

            View view = new View(mainActivity.get());
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                    appbar.getToolbar().getContentInsetLeft(), LinearLayout.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params1);
            buttons.addView(view);

            for (int i = 0; i < names.size(); i++) {
                final int k = i;
                if (rpaths.get(i).equals("/")) {
                    buttons.addView(buttonRoot);
                } else if (Futils.isStorage(rpaths.get(i))) {
                    buttonStorage.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View p1) {
                            mainFrag.loadlist((rpaths.get(k)), false, mainFrag.openMode);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    buttons.addView(buttonStorage);
                } else {
                    Button button = createFolderButton(rnames.get(i));
                    buttons.addView(button);
                }

                if (names.size() - i != 1) {
                    buttons.addView(createArrow());
                }
            }

            scroll.post(new Runnable() {
                @Override
                public void run() {
                    sendScroll(scroll);
                    sendScroll(pathScroll);
                }
            });

            if (buttons.getVisibility() == View.VISIBLE) {
                timer.cancel();
                timer.start();
            }
        }
    }

    private ImageView createArrow() {
        ImageView buttonArrow;

        if(lastUsedArrowButton >= arrowButtons.size()) {
            buttonArrow = new ImageView(mainActivity.get());
            buttonArrow.setImageDrawable(arrow);
            buttonArrow.setLayoutParams(buttonParams);
            arrowButtons.add(buttonArrow);
        } else {
            buttonArrow = arrowButtons.get(lastUsedArrowButton);
        }

        lastUsedArrowButton++;

        return buttonArrow;
    }

    private Button createFolderButton(String text) {
        Button button;

        if(lastUsedFolderButton >= folderButtons.size()) {
            button = new Button(mainActivity.get());
            button.setTextColor(Utils.getColor(mainActivity.get(), android.R.color.white));
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

    public void updatePath(@NonNull final String news, boolean results, String query, OpenMode openmode,
                           int folderCount, int fileCount) {

        if (news.length() == 0) return;

        MainActivityHelper mainActivityHelper = mainActivity.get().mainActivityHelper;

        switch (openmode) {
            case SMB:
                newPath = mainActivityHelper.parseSmbPath(news);
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
            pathText.setText(mainActivity.get().getString(R.string.folderfilecount, folderCount, fileCount));
        } else {
            fullPathText.setText(mainActivity.get().getString(R.string.searchresults, query));
            pathText.setText("");
            return;
        }

        final String oldPath = fullPathText.getText().toString();
        if (oldPath.equals(newPath)) return;

        final Animation slideIn = AnimationUtils.loadAnimation(mainActivity.get(), R.anim.slide_in);
        Animation slideOut = AnimationUtils.loadAnimation(mainActivity.get(), R.anim.slide_out);

        if (newPath.length() > oldPath.length() && newPath.contains(oldPath) && oldPath.length() != 0) {
            // navigate forward
            fullPathAnim.setAnimation(slideIn);
            fullPathAnim.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fullPathAnim.setVisibility(View.GONE);
                            fullPathText.setText(newPath);
                        }
                    }, PATH_ANIM_END_DELAY);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathAnim.setText(Utils.differenceStrings(oldPath, newPath));
                    //fullPathText.setText(oldPath);

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            pathScroll.fullScroll(View.FOCUS_RIGHT);
                        }
                    });
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    //onAnimationEnd(animation);
                }
            }).setStartDelay(PATH_ANIM_START_DELAY).start();
        } else if (newPath.length() < oldPath.length() && oldPath.contains(newPath)) {
            // navigate backwards
            fullPathAnim.setAnimation(slideOut);
            fullPathAnim.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    fullPathAnim.setVisibility(View.GONE);
                    fullPathText.setText(newPath);

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            pathScroll.fullScroll(View.FOCUS_RIGHT);
                        }
                    });
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathAnim.setText(Utils.differenceStrings(newPath, oldPath));
                    fullPathText.setText(newPath);

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            pathScroll.fullScroll(View.FOCUS_LEFT);
                        }
                    });
                }
            }).setStartDelay(PATH_ANIM_START_DELAY).start();
        } else if (oldPath.isEmpty()) {
            // case when app starts
            fullPathAnim.setAnimation(slideIn);
            fullPathAnim.setText(newPath);
            fullPathAnim.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathText.setText("");
                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            pathScroll.fullScroll(View.FOCUS_RIGHT);
                        }
                    });
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fullPathAnim.setVisibility(View.GONE);
                            fullPathText.setText(newPath);
                        }
                    }, PATH_ANIM_END_DELAY);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    //onAnimationEnd(animation);
                }
            }).setStartDelay(PATH_ANIM_START_DELAY).start();
        } else {
            // completely different path
            // first slide out of old path followed by slide in of new path
            fullPathAnim.setAnimation(slideOut);
            fullPathAnim.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    super.onAnimationStart(animator);
                    fullPathAnim.setVisibility(View.VISIBLE);
                    fullPathAnim.setText(oldPath);
                    fullPathText.setText("");

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            pathScroll.fullScroll(View.FOCUS_LEFT);
                        }
                    });
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);

                    //fullPathAnim.setVisibility(View.GONE);
                    fullPathAnim.setText(newPath);
                    fullPathText.setText("");
                    fullPathAnim.setAnimation(slideIn);

                    fullPathAnim.animate().setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    fullPathAnim.setVisibility(View.GONE);
                                    fullPathText.setText(newPath);
                                }
                            }, PATH_ANIM_END_DELAY);
                        }

                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            // we should not be having anything here in path bar
                            fullPathAnim.setVisibility(View.VISIBLE);
                            fullPathText.setText("");
                            scroll.post(new Runnable() {
                                @Override
                                public void run() {
                                    pathScroll.fullScroll(View.FOCUS_RIGHT);
                                }
                            });
                        }
                    }).start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    //onAnimationEnd(animation);
                }
            }).setStartDelay(PATH_ANIM_START_DELAY).start();
        }
    }

    private void sendScroll(final HorizontalScrollView scrollView) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_RIGHT);
            }
        }, 100);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

}
