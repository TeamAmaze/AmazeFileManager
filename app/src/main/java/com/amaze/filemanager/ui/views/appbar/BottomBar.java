package com.amaze.filemanager.ui.views.appbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.files.Futils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import static com.amaze.filemanager.R.id.pathbar;

/**
 * @author Emmanuel
 *         on 2/8/2017, at 23:31.
 */

public class BottomBar {
    private static final int PATH_ANIM_START_DELAY = 0;
    private static final int PATH_ANIM_END_DELAY = 0;

    private WeakReference<MainActivity> mainActivity;
    private AppBar appbar;
    private String newPath;

    private LinearLayout pathLayout;
    private LinearLayout buttons;
    private HorizontalScrollView scroll, pathScroll;
    private TextView pathText, fullPathText, fullPathAnim;

    private CountDownTimer timer;

    public BottomBar(AppBar appbar, MainActivity a) {
        mainActivity = new WeakReference<>(a);
        this.appbar = appbar;

        scroll = (HorizontalScrollView) a.findViewById(R.id.scroll);
        buttons = (LinearLayout) a.findViewById(R.id.buttons);

        pathLayout = (LinearLayout) a.findViewById(pathbar);
        pathScroll = (HorizontalScrollView) a.findViewById(R.id.scroll1);
        fullPathText = (TextView) a.findViewById(R.id.fullpath);
        fullPathAnim= (TextView) a.findViewById(R.id.fullpath_anim);

        pathText = (TextView) a.findViewById(R.id.pathname);

        scroll.setSmoothScrollingEnabled(true);
        pathScroll.setSmoothScrollingEnabled(true);

        timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                Futils.crossfadeInverse(buttons, pathLayout);
            }
        };
    }

    public void initiatebbar() {
        pathLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainFragment m = mainActivity.get().getCurrentMainFragment();
                if (m.openMode == OpenMode.FILE) {
                    bbar(m);
                    Futils.crossfade(buttons, pathLayout);
                    timer.cancel();
                    timer.start();
                }
            }
        });
        fullPathText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainFragment m = mainActivity.get().getCurrentMainFragment();
                if (m.openMode == OpenMode.FILE) {
                    bbar(m);
                    Futils.crossfade(buttons, pathLayout);
                    timer.cancel();
                    timer.start();
                }
            }
        });
    }

    public void resetClickListeners() {
        pathText.setOnClickListener(null);
        fullPathText.setOnClickListener(null);
    }

    public void setPathText(@StringRes int text) {
        pathText.setText(text);
    }

    public void setPathText(String text) {
        pathText.setText(text);
    }

    public void setFullPathText(@StringRes int text) {
        fullPathText.setText(text);
    }

    public void setFullPathText(String text) {
        fullPathText.setText(text);
    }

    public boolean areButtonsShowing() {
        return buttons.getVisibility() == View.VISIBLE;
    }

    public void bbar(final MainFragment mainFrag) {
        final String path = mainFrag.getCurrentPath();
        try {
            buttons.removeAllViews();
            buttons.setMinimumHeight(pathLayout.getHeight());
            Drawable arrow = mainActivity.get().getResources().getDrawable(R.drawable.abc_ic_ab_back_holo_dark);
            Bundle bundle = Futils.getPaths(path, mainActivity.get());
            ArrayList<String> names = bundle.getStringArrayList("names");
            ArrayList<String> rnames = bundle.getStringArrayList("names");
            Collections.reverse(rnames);

            ArrayList<String> paths = bundle.getStringArrayList("paths");
            final ArrayList<String> rpaths = bundle.getStringArrayList("paths");
            Collections.reverse(rpaths);

            View view = new View(mainActivity.get());
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                    appbar.getToolbar().getContentInsetLeft(), LinearLayout.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params1);
            buttons.addView(view);
            for (int i = 0; i < names.size(); i++) {
                final int k = i;
                ImageView v = new ImageView(mainActivity.get());
                v.setImageDrawable(arrow);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER_VERTICAL;
                v.setLayoutParams(params);
                final int index = i;
                if (rpaths.get(i).equals("/")) {
                    ImageButton ib = new ImageButton(mainActivity.get());
                    ib.setImageDrawable(mainActivity.get().getResources().getDrawable(R.drawable.root));
                    ib.setBackgroundColor(Color.TRANSPARENT);
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            mainFrag.loadlist(("/"), false, mainFrag.openMode);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    ib.setLayoutParams(params);
                    buttons.addView(ib);
                    if (names.size() - i != 1)
                        buttons.addView(v);
                } else if (Futils.isStorage(rpaths.get(i))) {
                    ImageButton ib = new ImageButton(mainActivity.get());
                    ib.setImageDrawable(mainActivity.get().getResources().getDrawable(R.drawable.ic_sd_storage_white_56dp));
                    ib.setBackgroundColor(Color.TRANSPARENT);
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            mainFrag.loadlist((rpaths.get(k)), false, mainFrag.openMode);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    ib.setLayoutParams(params);
                    buttons.addView(ib);
                    if (names.size() - i != 1)
                        buttons.addView(v);
                } else {
                    Button b = new Button(mainActivity.get());
                    b.setText(rnames.get(index));
                    b.setTextColor(Utils.getColor(mainActivity.get(), android.R.color.white));
                    b.setTextSize(13);
                    b.setLayoutParams(params);
                    b.setBackgroundResource(0);
                    b.setOnClickListener(new Button.OnClickListener() {

                        public void onClick(View p1) {
                            mainFrag.loadlist((rpaths.get(k)), false, mainFrag.openMode);
                            mainFrag.loadlist((rpaths.get(k)), false, mainFrag.openMode);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    b.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {

                            File file1 = new File(rpaths.get(index));
                            Futils.copyToClipboard(mainActivity.get(), file1.getPath());
                            Toast.makeText(mainActivity.get(), mainActivity.get().getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });

                    buttons.addView(b);
                    if (names.size() - i != 1)
                        buttons.addView(v);
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
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("BBar", "button view not available");
        }
    }

    public void updatePath(@NonNull final String news, boolean results, OpenMode openmode,
                           int folder_count, int file_count) {

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
            pathText.setText(folder_count + " " + mainActivity.get().getResources().getString(R.string.folders) + "" +
                    " " + file_count + " " + mainActivity.get().getResources().getString(R.string.files));
        } else {
            fullPathText.setText(R.string.searchresults);
            pathText.setText(R.string.empty);
            return;
        }
        final String oldPath = fullPathText.getText().toString();
        if (oldPath.equals(newPath)) return;

        final StringBuffer newPathBuilder, oldPathBuilder;

        // implement animation while setting text
        newPathBuilder = new StringBuffer().append(newPath);
        oldPathBuilder = new StringBuffer().append(oldPath);

        final Animation slideIn = AnimationUtils.loadAnimation(mainActivity.get(), R.anim.slide_in);
        Animation slideOut = AnimationUtils.loadAnimation(mainActivity.get(), R.anim.slide_out);

        if (newPath.length() > oldPath.length() &&
                newPathBuilder.delete(oldPath.length(), newPath.length()).toString().equals(oldPath) &&
                oldPath.length() != 0) {

            // navigate forward
            newPathBuilder.delete(0, newPathBuilder.length());
            newPathBuilder.append(newPath);
            newPathBuilder.delete(0, oldPath.length());
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
                    fullPathAnim.setText(newPathBuilder.toString());
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
        } else if (newPath.length() < oldPath.length() &&
                oldPathBuilder.delete(newPath.length(), oldPath.length()).toString().equals(newPath)) {

            // navigate backwards
            oldPathBuilder.delete(0, oldPathBuilder.length());
            oldPathBuilder.append(oldPath);
            oldPathBuilder.delete(0, newPath.length());
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
                    fullPathAnim.setText(oldPathBuilder.toString());
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

}
