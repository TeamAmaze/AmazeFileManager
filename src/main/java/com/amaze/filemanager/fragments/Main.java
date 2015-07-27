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

package com.amaze.filemanager.fragments;


import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.Recycleradapter;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.services.asynctasks.SearchTask;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HFile;
import com.amaze.filemanager.utils.HidingScrollListener;
import com.amaze.filemanager.utils.HistoryManager;
import com.amaze.filemanager.ui.icons.IconHolder;
import com.amaze.filemanager.ui.icons.IconUtils;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Shortcuts;
import com.amaze.filemanager.utils.SmbStreamer.Streamer;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class Main extends android.support.v4.app.Fragment {
    public File[] file;
    public ArrayList<Layoutelements> list;
    public Recycleradapter adapter;
    public Futils utils;
    public boolean selection;
    public boolean results = false;
    public ActionMode mActionMode;
    public SharedPreferences Sp;
    public Drawable folder, apk, darkimage, darkvideo;
    Resources res;
    public LinearLayout buttons;
    public int sortby, dsort, asc;

    public String home, current = "";
    Shortcuts sh;
    HashMap<String, Bundle> scrolls = new HashMap<String, Bundle>();
    Main ma = this;
    public HistoryManager history, hidden;
    IconUtils icons;
    public boolean rootMode, showHidden, circularImages, showPermissions, showSize, showLastModified;
    View footerView;
    public LinearLayout pathbar;
    public int openMode=0;
    private View rootView;
    public android.support.v7.widget.RecyclerView listView;
    public boolean gobackitem, islist, showThumbs, coloriseIcons, showDividers, topFab;
    public IconHolder ic;
    public MainActivity mainActivity;
    public String skin, fabSkin, iconskin;
    public int theme;
    public int theme1;
    public float[] color;
    public ColorMatrixColorFilter colorMatrixColorFilter;
    public String year, goback;
    ArrayList<String> hiddenfiles;
    String itemsstring;
    int no;
    TabHandler tabHandler;
    LinearLayoutManager mLayoutManager;
    GridLayoutManager mLayoutManagerGrid;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    boolean addheader = false;
    StickyRecyclerHeadersDecoration headersDecor;
    DividerItemDecoration dividerItemDecoration;
    public int paddingTop;
    int mToolbarHeight, hidemode;
    View mToolbarContainer;
    public int skin_color, icon_skin_color, columns;
    public String smbPath;
    public ArrayList<String> searchHelper = new ArrayList<String>();
    public SearchTask searchTask;
    TextView pathname;
    public int skinselection;
    boolean stopAnims=true;
    public int file_count,folder_count;
    private View actionModeView;
    View nofilesview;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        no = getArguments().getInt("no", 1);
        home = getArguments().getString("home");
        current = getArguments().getString("lastpath");
        tabHandler = new TabHandler(getActivity(), null, null, 1);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        skin = PreferenceUtils.getSkinColor(Sp.getInt("skin_color_position", 4));
        fabSkin = PreferenceUtils.getFabColor(Sp.getInt("fab_skin_color_position", 1));
        iconskin = PreferenceUtils.getSkinColor(Sp.getInt("icon_skin_color_position", 4));
        skin_color = Color.parseColor(skin);
        icon_skin_color = Color.parseColor(iconskin);
        sh = new Shortcuts(getActivity(), "shortcut.xml");
        islist = Sp.getBoolean("view", true);
        Calendar calendar = Calendar.getInstance();
        year = ("" + calendar.get(Calendar.YEAR)).substring(2, 4);
        theme = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme == 2 ? PreferenceUtils.hourOfDay() : theme;
        hidemode = Sp.getInt("hidemode", 0);
        topFab = hidemode == 0 ? Sp.getBoolean("topFab", true) : false;
        showPermissions = Sp.getBoolean("showPermissions", false);
        showSize = Sp.getBoolean("showFileSize", false);
        showDividers = Sp.getBoolean("showDividers", true);
        gobackitem = Sp.getBoolean("goBack_checkbox", false);
        circularImages = Sp.getBoolean("circularimages", true);
        showLastModified = Sp.getBoolean("showLastModified", true);
        icons = new IconUtils(Sp, getActivity());
    }
    public void stopAnimation()
    {
        if ((!adapter.stoppedAnimation) )
        {
            for (int j = 0; j < listView.getChildCount(); j++)
            {    View v=listView.getChildAt(j);
                 if(v!=null)v.clearAnimation();
        }}
        adapter.stoppedAnimation = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.main_frag, container, false);
        listView = (android.support.v7.widget.RecyclerView) rootView.findViewById(R.id.listView);
        mToolbarContainer = getActivity().findViewById(R.id.lin);
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(adapter!=null && stopAnims)
                {
                    stopAnimation();
                    stopAnims=false;
                }
                return false;
            }
        });
        mToolbarContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (adapter != null && stopAnims) {
                    stopAnimation();
                    stopAnims = false;
                }
                return false;
            }
        });
        buttons = (LinearLayout) getActivity().findViewById(R.id.buttons);
        pathbar = (LinearLayout) getActivity().findViewById(R.id.pathbar);
        showThumbs = Sp.getBoolean("showThumbs", true);
        ic = new IconHolder(getActivity(), showThumbs, !islist);
        res = getResources();
        pathname = (TextView) getActivity().findViewById(R.id.pathname);
        goback = res.getString(R.string.goback);
        itemsstring = res.getString(R.string.items);
        apk = res.getDrawable(R.drawable.ic_doc_apk_grid);
        if (theme1 == 1) {

            mainActivity.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_dark_background)));
        } else {

            if (islist)
                mainActivity.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.background_light)));

        }
        listView.setHasFixedSize(true);
        columns = Integer.parseInt(Sp.getString("columns", "-1"));
        if (islist) {
            mLayoutManager = new LinearLayoutManager(getActivity());
            listView.setLayoutManager(mLayoutManager);
        } else {
            if(columns==-1 || columns==0)
                mLayoutManagerGrid=new GridLayoutManager(getActivity(),3);
            else
            mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
            listView.setLayoutManager(mLayoutManagerGrid);
        }
        mToolbarContainer.setBackgroundColor(skin_color);
        //   listView.setPadding(listView.getPaddingLeft(), paddingTop, listView.getPaddingRight(), listView.getPaddingBottom());
        return rootView;
    }
    DisplayMetrics displayMetrics;
    public int dpToPx(int dp) {
        if(displayMetrics==null)displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }
    HFile f;
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        mainActivity = (MainActivity) getActivity();
        initNoFileLayout();
        utils = new Futils();
        String x = getSelectionColor();
        skinselection = Color.parseColor(x);
        color = calculatevalues(x);
        ColorMatrix colorMatrix = new ColorMatrix(calculatefilter(color));
        colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        history = new HistoryManager(getActivity(), "Table1");
        hidden = new HistoryManager(getActivity(), "Table2");
        hiddenfiles = hidden.readTable();
        rootMode = Sp.getBoolean("rootmode", false);
        showHidden = Sp.getBoolean("showHidden", false);
        coloriseIcons = Sp.getBoolean("coloriseIcons", true);
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);
        getSortModes();
        darkimage = res.getDrawable(R.drawable.ic_doc_image_dark);
        darkvideo = res.getDrawable(R.drawable.ic_doc_video_dark);
        this.setRetainInstance(false);
         f= new HFile(current);
        if (!f.isCustomPath() && !f.isSmb() && !f.isDirectory()) {
            File file=new File(current);
            utils.openFile(file, mainActivity);
            current=(file.getParent());
        }
        mainActivity.initiatebbar();

        // use a linear layout manager
        footerView = getActivity().getLayoutInflater().inflate(R.layout.divider, null);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadlist((current), false,true);
            }
        });
        mToolbarHeight = getToolbarHeight(getActivity());
        paddingTop = (mToolbarHeight) + dpToPx(72);
        if (hidemode == 2) mToolbarHeight = paddingTop;
        mSwipeRefreshLayout.setProgressViewOffset(true, paddingTop, paddingTop + dpToPx(30));
        mSwipeRefreshLayout.setColorSchemeColors(Color.parseColor(fabSkin));
        DefaultItemAnimator animator=new DefaultItemAnimator();
        listView.setItemAnimator(animator);
        mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
             @Override
             public void onGlobalLayout() {
                 if (hidemode != 2) mToolbarHeight = mainActivity.toolbar.getHeight();
                 else mToolbarHeight = paddingTop;
                 paddingTop = mToolbarContainer.getHeight();
                 if (!islist && (columns == 0 || columns == -1)) {
                     int screen_width = listView.getWidth();
                     int dptopx = dpToPx(120);
                     columns = screen_width / dptopx;
                     mLayoutManagerGrid.setSpanCount(columns);
                 }paddingTop = mToolbarContainer.getHeight();

                 mSwipeRefreshLayout.setProgressViewOffset(true, paddingTop, paddingTop + dpToPx(72));
                 if(savedInstanceState!=null && !islist)retrieveFromSavedInstance(savedInstanceState);
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                     mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                 } else {
                     mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                 }
             }

         });
        if (savedInstanceState == null) {
            loadlist(current, false, true);

        } else {
            if(islist)
                retrieveFromSavedInstance(savedInstanceState);
        }
    }

    void retrieveFromSavedInstance(final Bundle savedInstanceState) {

        Bundle b = new Bundle();
        String cur = savedInstanceState.getString("current");
        if (cur != null) {
            b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);

            openMode = savedInstanceState.getInt("openMode", 0);
            if (openMode == 1)
                smbPath = savedInstanceState.getString("SmbPath");
            list = savedInstanceState.getParcelableArrayList("list");
            folder_count = savedInstanceState.getInt("folder_count", 0);
            file_count = savedInstanceState.getInt("file_count", 0);
            if (savedInstanceState.getBoolean("results")) {
                try {
                    createViews(list, true, (current), openMode,true);
                    pathname.setText(ma.utils.getString(ma.getActivity(), R.string.searchresults));
                    results = true;
                } catch (Exception e) {
                }
            } else {
                createViews(list, true, (cur), openMode,false);
            }
            if (savedInstanceState.getBoolean("selection")) {

                for (int i : savedInstanceState.getIntegerArrayList("position")) {
                    adapter.toggleChecked(i);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int index;
        View vi;
        if (listView != null) {
            if (islist) {

                index = (mLayoutManager).findFirstVisibleItemPosition();
                vi = listView.getChildAt(0);
            } else {
                index = (mLayoutManagerGrid).findFirstVisibleItemPosition();
                vi = listView.getChildAt(0);
            }
            int top = (vi == null) ? 0 : vi.getTop();
            outState.putInt("index", index);
            outState.putInt("top", top);
            outState.putParcelableArrayList("list", list);
            outState.putString("current", current);
            outState.putBoolean("selection", selection);
            outState.putInt("openMode", openMode);
            outState.putInt("folder_count",folder_count);
            outState.putInt("file_count",file_count);
            if (selection) {
                outState.putIntegerArrayList("position", adapter.getCheckedItemPositions());
            }
            if (results) {
                outState.putBoolean("results", results);
            }
            if (openMode==1)
            {
                outState.putString("SmbPath", smbPath);
            }}
    }

    public void home() {
        ma.loadlist((ma.home), false, false);
    }


    public void onListItemClicked(int position, View v) {
        if (position >= list.size()) return;
        if (results) {
            if (searchTask != null) {
                if (searchTask.getStatus() == AsyncTask.Status.RUNNING)
                    searchTask.cancel(true);
                searchTask = null;
            }
            String path = list.get(position).getDesc();
            if (selection) adapter.toggleChecked(position);
            else {

                final File f = new File(path);
                if (list.get(position).isDirectory()) {

                    loadlist(f.getPath(), false,false);
                    results = false;
                } else {
                    if (mainActivity.mReturnIntent) {
                        returnIntentResults(f);
                    } else
                        utils.openFile(f, (MainActivity) getActivity());
                }
            }
        } else if (openMode==1) {
            if (selection) adapter.toggleChecked(position);
            else {
                try {
                    SmbFile g = new SmbFile(list.get(position).getDesc());
                    if (g.isDirectory())
                        loadCustomList(g.getPath(), false);
                    else launch(g);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (SmbException e) {
                    e.printStackTrace();
                }
            }
        } else if (selection == true) {
            if (!list.get(position).getSize().equals(goback)) {
                adapter.toggleChecked(position);
            } else {
                selection = false;
                if (mActionMode != null)
                    mActionMode.finish();
                mActionMode = null;
            }

        } else {
            if (!list.get(position).getSize().equals(goback)) {

                String path;
                Layoutelements l = list.get(position);

                if (!l.hasSymlink()) {

                    path = l.getDesc();
                } else {

                    path = l.getSymlink();
                }

                final File f = new File(path);

                if (l.isDirectory()) {

                    computeScroll();
                    loadlist(f.getPath(), false,false);
                } else {
                    if (mainActivity.mReturnIntent) {
                        returnIntentResults(f);
                    } else {

                        utils.openFile(f, (MainActivity) getActivity());
                    }
                }

            } else {

                goBackItemClick();

            }
        }
    }

    private void returnIntentResults(File file) {
        mainActivity.mReturnIntent = false;

        Intent intent = new Intent();
        if (mainActivity.mRingtonePickerIntent) {

            Log.d("pickup", "ringtone");
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());

            intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
            getActivity().setResult(getActivity().RESULT_OK, intent);
            getActivity().finish();
        } else {

            Log.d("pickup", "file");
            intent.setData(Uri.fromFile(file));
            getActivity().setResult(getActivity().RESULT_OK, intent);
            getActivity().finish();
        }
    }

    public void loadlist(String path, boolean back,boolean checkpath) {
        if (mActionMode != null) {
            mActionMode.finish();
        }
        if(checkpath)
        {
            if (path.startsWith("smb:") || new HFile(path).isCustomPath())
                loadCustomList((path), false);
            else
                new LoadList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (path));
        }
        else  new LoadList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (path));

    }

    public void loadCustomList(String path,boolean back){
        new LoadList(back, ma,true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
    }


    @SuppressWarnings("unchecked")
    void initNoFileLayout(){
    nofilesview=rootView.findViewById(R.id.nofilelayout);
    if(theme1==0)
        ((ImageView)nofilesview.findViewById(R.id.image)).setColorFilter(Color.parseColor
                ("#666666"));
    else ((TextView)nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
    }

    public void createViews(ArrayList<Layoutelements> bitmap, boolean back, String f,int
            openMode,boolean results) {
        try {
            if (bitmap != null) {
                if (gobackitem)
                    if (!f.equals("/") && openMode==0) {
                        if (bitmap.size() == 0 || !bitmap.get(0).getSize().equals(goback))
                            bitmap.add(0, utils.newElement(res.getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha), "..", "", "", goback, 0, false, true, ""));
                    }

                if(bitmap.size()==0 && !results) {
                    nofilesview.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setEnabled(false);
                }else {
                    mSwipeRefreshLayout.setEnabled(true);
                    nofilesview.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);

                }
                list=bitmap;
                if(adapter==null)
                adapter = new Recycleradapter(ma,
                        bitmap, ma.getActivity());
                else {
                    adapter.generate(list);
                }
                stopAnims=true;
                this.openMode=openMode;
                if(openMode==0){
                    history.addPath(f);
                }
                mSwipeRefreshLayout.setRefreshing(false);
                try {
                    listView.setAdapter(adapter);
                    if (!addheader && islist) {
                        listView.removeItemDecoration(dividerItemDecoration);
                        listView.removeItemDecoration(headersDecor);
                        addheader = true;
                    }
                    if (addheader && islist) {
                        dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST, false, showDividers);
                        listView.addItemDecoration(dividerItemDecoration);

                        headersDecor = new StickyRecyclerHeadersDecoration(adapter);
                        listView.addItemDecoration(headersDecor);
                        addheader = false;
                    }
                    if(!results)this.results = false;
                    current = f;
                    if (back) {
                        if (scrolls.containsKey(current)) {
                            Bundle b = scrolls.get(current);
                            if (islist)
                                mLayoutManager.scrollToPositionWithOffset(b.getInt("index"), b.getInt("top"));
                            else
                                mLayoutManagerGrid.scrollToPositionWithOffset(b.getInt("index"), b.getInt("top"));
                        }
                    }
                    //floatingActionButton.show();
                    mainActivity.updatepaths(no);
                    listView.setOnScrollListener(new HidingScrollListener(mToolbarHeight, hidemode) {

                        @Override
                        public void onMoved(int distance) {
                            mToolbarContainer.setTranslationY(-distance);
                        }

                        @Override
                        public void onShow() {
                            mToolbarContainer.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
                        }

                        @Override
                        public void onHide() {
                            mToolbarContainer.animate().translationY(-mToolbarHeight)
                                    .setInterpolator(new AccelerateInterpolator(2)).start();
                        }

                    });
                    listView.stopScroll();
                    if (buttons.getVisibility() == View.VISIBLE) mainActivity.bbar(this);

                } catch (Exception e) {
                }
            } else {//Toast.makeText(getActivity(),res.getString(R.string.error),Toast.LENGTH_LONG).show();
                loadlist(home, true,false);
            }
        } catch (Exception e) {
        }

    }

    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        private void hideOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(false);
        }

        private void showOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(true);
        }

        public void initMenu(Menu menu) {
            /*menu.findItem(R.id.cpy).setIcon(icons.getCopyDrawable());
            menu.findItem(R.id.cut).setIcon(icons.getCutDrawable());
            menu.findItem(R.id.delete).setIcon(icons.getDeleteDrawable());
            menu.findItem(R.id.all).setIcon(icons.getAllDrawable());*/
        }

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            actionModeView = getActivity().getLayoutInflater().inflate(R.layout.actionmode,null);
            mode.setCustomView(actionModeView);

            mainActivity.setPagingEnabled(false);
            mainActivity.floatingActionButton.hideMenuButton(true);
            if (mainActivity.isDrawerLocked) mainActivity.translateDrawerList(true);
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.sethome, menu);
            hideOption(R.id.rename, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.about, menu);
            hideOption(R.id.openwith, menu);
            hideOption(R.id.ex, menu);
            if (mainActivity.mReturnIntent)
                showOption(R.id.openmulti, menu);
            //hideOption(R.id.setringtone,menu);
            mode.setTitle(utils.getString(getActivity(), R.string.select));
            /*if(Build.VERSION.SDK_INT<19)
                getActivity().findViewById(R.id.action_bar).setVisibility(View.GONE);*/
            // rootView.findViewById(R.id.buttonbarframe).setBackgroundColor(res.getColor(R.color.toolbar_cab));
            ObjectAnimator anim = ObjectAnimator.ofInt(getActivity().findViewById(R.id.buttonbarframe), "backgroundColor", skin_color, res.getColor(R.color.holo_dark_action_mode));
            anim.setDuration(0);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if (mainActivity.colourednavigation)
                    window.setNavigationBarColor(res.getColor(android.R.color.black));
            }

            if (!mainActivity.isDrawerLocked)
                mainActivity.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                        mainActivity.mDrawerLinear);
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = adapter.getCheckedItemPositions();
            TextView textView1 = (TextView) actionModeView.findViewById(R.id.item_count);
            textView1.setText(positions.size() + "");
            textView1.setOnClickListener(null);
            mode.setTitle(positions.size() + "");
            hideOption(R.id.openmulti, menu);
            if (openMode==1) {
                hideOption(R.id.addshortcut, menu);
                hideOption(R.id.permissions, menu);
                hideOption(R.id.ex, menu);
                hideOption(R.id.sethome, menu);
                hideOption(R.id.openwith, menu);
                hideOption(R.id.about, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.hide, menu);
                hideOption(R.id.book, menu);
                hideOption(R.id.compress, menu);
              showOption(R.id.rename,menu);
                return true;
            }
            if (mainActivity.mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16)
                    showOption(R.id.openmulti, menu);
            //tv.setText(positions.size());
            if (!results) {
                hideOption(R.id.openparent, menu);
                if (positions.size() == 1) {
                    showOption(R.id.addshortcut, menu);
                    showOption(R.id.permissions, menu);

                    showOption(R.id.openwith, menu);
                    showOption(R.id.about, menu);
                    showOption(R.id.share, menu);
                    showOption(R.id.rename, menu);

                    File x = new File(list.get(adapter.getCheckedItemPositions().get(0))

                            .getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith, menu);
                        showOption(R.id.sethome, menu);
                        hideOption(R.id.share, menu);
                        hideOption(R.id.openmulti, menu);
                    } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk") || x.getName().toLowerCase().endsWith(".rar") || x.getName().toLowerCase().endsWith(".tar") || x.getName().toLowerCase().endsWith(".tar.gz")) {

                        showOption(R.id.ex, menu);

                        if (mainActivity.mReturnIntent)
                            if (Build.VERSION.SDK_INT >= 16)
                                showOption(R.id.openmulti, menu);
                    }
                } else {
                    try {
                        showOption(R.id.share, menu);
                        if (mainActivity.mReturnIntent)
                            if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
                        for (int c : adapter.getCheckedItemPositions()) {
                            File x = new File(list.get(c).getDesc());
                            if (x.isDirectory()) {
                                hideOption(R.id.share, menu);
                                hideOption(R.id.openmulti, menu);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    hideOption(R.id.ex, menu);

                    hideOption(R.id.sethome, menu);

                    hideOption(R.id.openwith, menu);

                    //hideOption(R.id.setringtone, menu);

                    hideOption(R.id.permissions, menu);

                    hideOption(R.id.about, menu);

                }
            } else {
                if (positions.size() == 1) {
                    showOption(R.id.addshortcut, menu);
                    showOption(R.id.permissions, menu);
                    showOption(R.id.openparent, menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.about, menu);
                    showOption(R.id.share, menu);
                    showOption(R.id.rename, menu);

                    File x = new File(list.get(adapter.getCheckedItemPositions().get(0))

                            .getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith, menu);
                        showOption(R.id.sethome, menu);
                        hideOption(R.id.share, menu);
                        hideOption(R.id.openmulti, menu);
                    } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk") || x.getName().toLowerCase().endsWith(".rar") || x.getName().toLowerCase().endsWith(".tar") || x.getName().toLowerCase().endsWith(".tar.gz")) {

                        showOption(R.id.ex, menu);

                        if (mainActivity.mReturnIntent)
                            if (Build.VERSION.SDK_INT >= 16)
                                showOption(R.id.openmulti, menu);
                    }
                } else {
                    hideOption(R.id.openparent, menu);

                    if (mainActivity.mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);
                    try {
                        for (int c : adapter.getCheckedItemPositions()) {
                            File x = new File(list.get(c).getDesc());
                            if (x.isDirectory()) {
                                hideOption(R.id.share, menu);
                                hideOption(R.id.openmulti, menu);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    hideOption(R.id.ex, menu);

                    hideOption(R.id.sethome, menu);

                    hideOption(R.id.openwith, menu);

                    //hideOption(R.id.setringtone, menu);

                    hideOption(R.id.permissions, menu);

                    hideOption(R.id.about, menu);

                }
            }

            return true; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            computeScroll();
            ArrayList<Integer> plist = adapter.getCheckedItemPositions();
            switch (item.getItemId()) {
                case R.id.openmulti:
                    if (Build.VERSION.SDK_INT >= 16) {
                        Intent intentresult = new Intent();
                        ArrayList<Uri> resulturis = new ArrayList<Uri>();
                        for (int k : plist) {
                            try {
                                resulturis.add(Uri.fromFile(new File(list.get(k).getDesc())));
                            } catch (Exception e) {

                            }
                        }
                        final ClipData clipData = new ClipData(
                                null, new String[]{"*/*"}, new ClipData.Item(resulturis.get(0)));
                        for (int i = 1; i < resulturis.size(); i++) {
                            clipData.addItem(new ClipData.Item(resulturis.get(i)));
                        }
                        intentresult.setClipData(clipData);
                        mode.finish();
                        getActivity().setResult(getActivity().RESULT_OK, intentresult);
                        getActivity().finish();
                    }
                    return true;
                case R.id.sethome:
                    int pos = plist.get(0);
                    home = list.get(pos).getDesc();
                    Toast.makeText(getActivity(),
                            utils.getString(getActivity(), R.string.newhomedirectory) + " " + list.get(pos).getTitle(),
                            Toast.LENGTH_LONG).show();
                    mainActivity.updatepaths(no);
                    mode.finish();
                    return true;
                case R.id.about:
                    String x;
                    x = list.get((plist.get(0))).getDesc();
                    utils.showProps((x), ma, rootMode);
                    mode.finish();
                    return true;
                /*case R.id.setringtone:
                    File fx;
                    if(results)
                        fx=new File(slist.get((plist.get(0))).getDesc());
                        else
                        fx=new File(list.get((plist.get(0))).getDesc());

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, fx.getAbsolutePath());
                    values.put(MediaStore.MediaColumns.TITLE, "Amaze");
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
                    //values.put(MediaStore.MediaColumns.SIZE, fx.);
                    values.put(MediaStore.Audio.Media.ARTIST, R.string.app_name);
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    values.put(MediaStore.Audio.Media.IS_ALARM, false);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(fx.getAbsolutePath());
                    Uri newUri = getActivity().getContentResolver().insert(uri, values);
                    try {
                        RingtoneManager.setActualDefaultRingtoneUri(getActivity(), RingtoneManager.TYPE_RINGTONE, newUri);
                        //Settings.System.putString(getActivity().getContentResolver(), Settings.System.RINGTONE, newUri.toString());
                        Toast.makeText(getActivity(), "Successful" + fx.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (Throwable t) {

                        Log.d("ringtone", "failed");
                    }
                    return true;*/
                case R.id.delete:
                    utils.deleteFiles(list, ma, plist);


                    return true;
                case R.id.share:
                    ArrayList<File> arrayList = new ArrayList<File>();
                    for (int i : plist) {
                        arrayList.add(new File(list.get(i).getDesc()));
                    }
                    if (arrayList.size() > 100)
                        Toast.makeText(getActivity(), "Can't share more than 100 files", Toast.LENGTH_SHORT).show();
                    else
                        utils.shareFiles(arrayList, getActivity(), theme1,Color.parseColor
                                (fabSkin));
                    return true;
                case R.id.openparent:
                    loadlist(new File(list.get(plist.get(0)).getDesc()).getParent(), false,false);
                    return true;
                case R.id.all:
                    if (adapter.areAllChecked(current)) {
                        adapter.toggleChecked(false, current);
                    } else {
                        adapter.toggleChecked(true, current);
                    }
                    mode.invalidate();

                    return true;
                case R.id.rename:

                    final ActionMode m = mode;
                    final String f;
                    f = (list.get(
                            (plist.get(0))).getDesc());
                    rename(f);
                    mode.finish();
                    return true;
                case R.id.hide:
                    for (int i1 = 0; i1 < plist.size(); i1++) {
                        hide(list.get(plist.get(i1)).getDesc());
                    }
                    updateList();
                    mode.finish();
                    return true;
                case R.id.book:
                    for (int i1 = 0; i1 < plist.size(); i1++) {
                        try {
                            sh.addS((list.get(plist.get(i1)).getDesc()));
                        } catch (Exception e) {
                        }
                    }
                    mainActivity.updateDrawer();
                    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                    mode.finish();
                    return true;
                case R.id.ex:
                    mainActivity.extractFile(new File(list.get(plist.get(0)).getDesc()));
                    mode.finish();
                    return true;
                case R.id.cpy:
                    mainActivity.MOVE_PATH = null;
                    ArrayList<String> copies = new ArrayList<String>();
                    for (int i2 = 0; i2 < plist.size(); i2++) {
                        copies.add(list.get(plist.get(i2)).getDesc());
                    }
                    mainActivity.COPY_PATH = copies;
                    mainActivity.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.cut:
                    mainActivity.COPY_PATH = null;
                    ArrayList<String> copie = new ArrayList<String>();
                    for (int i3 = 0; i3 < plist.size(); i3++) {
                        copie.add(list.get(plist.get(i3)).getDesc());
                    }
                    mainActivity.MOVE_PATH = copie;
                    mainActivity.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.compress:
                    ArrayList<String> copies1 = new ArrayList<String>();
                    for (int i4 = 0; i4 < plist.size(); i4++) {
                        copies1.add(list.get(plist.get(i4)).getDesc());
                    }
                    utils.showNameDialog((MainActivity) getActivity(), copies1, current);
                    mode.finish();
                    return true;
                case R.id.openwith:
                    utils.openunknown(new File(list.get((plist.get(0))).getDesc()), getActivity(), true);
                    return true;
                case R.id.permissions:
                    utils.setPermissionsDialog(list.get(plist.get(0)), ma);
                    mode.finish();
                    return true;
                case R.id.addshortcut:
                    addShortcut(list.get(plist.get(0)));
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            selection = false;
            if (mainActivity.isDrawerLocked) mainActivity.translateDrawerList(false);

            mainActivity.floatingActionButton.showMenuButton(true);
            if (!results) adapter.toggleChecked(false, current);
            else adapter.toggleChecked(false);
            mainActivity.setPagingEnabled(true);
            ObjectAnimator anim = ObjectAnimator.ofInt(getActivity().findViewById(R.id.buttonbarframe), "backgroundColor", res.getColor(R.color.holo_dark_action_mode), skin_color);
            anim.setDuration(0);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if (mainActivity.colourednavigation)
                    window.setNavigationBarColor(mainActivity.skinStatusBar);
            }

            if (!mainActivity.isDrawerLocked)
                mainActivity.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
                        mainActivity.mDrawerLinear);
        }
    };

    public void rename(final String f) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
        String name =new HFile(f).getName();
        a.input("", name, false, new MaterialDialog.InputCallback() {
            @Override
            public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

            }
        });
        if (theme1 == 1) a.theme(Theme.DARK);
        a.title(utils.getString(getActivity(), R.string.rename));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                String name = materialDialog.getInputEditText().getText().toString();
                if (openMode == 1) try {
                    if (new SmbFile(f).isDirectory() && !name.endsWith("/"))
                        name = name + "/";
                } catch (SmbException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (openMode == 1)
                    mainActivity.rename(f, current + name);
                else
                    mainActivity.rename((f), (current + "/" + name));

            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

                materialDialog.cancel();
            }
        });
        a.positiveText(R.string.save);
        a.negativeText(R.string.cancel);
        int color=Color.parseColor(fabSkin);
        a.positiveColor(color).negativeColor(color).widgetColor(color);
        a.build().show();
    }
    public void computeScroll() {
        View vi = listView.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        int index;
        if (islist)
            index = mLayoutManager.findFirstVisibleItemPosition();
        else index = mLayoutManagerGrid.findFirstVisibleItemPosition();
        Bundle b = new Bundle();
        b.putInt("index", index);
        b.putInt("top", top);
        scrolls.put(current, b);
    }

    public void goBack() {
        if(openMode==2){
            loadlist(home,false,false);
            return;
        }
        File f = new File(current);
        if (!results) {
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode==1)
                    try {
                        if (!mainActivity.Servers.contains(current)) {
                            String path = (new SmbFile(current).getParent());
                            loadCustomList((path), true);
                        } else loadlist(home, false,true);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                else if (current.equals("/") || current.equals(home))
                    mainActivity.exit();
                else if (utils.canGoBack(f)) {
                    loadlist(f.getParent(), true,false);
                } else mainActivity.exit();
            }
        } else {
            if (searchTask != null) {
                if (searchTask.getStatus() == AsyncTask.Status.RUNNING)
                    searchTask.cancel(true);
                searchTask = null;
            }
            loadlist(f.getPath(), true,false);
        }
    }

    public void goBackItemClick() {
        if(openMode==2){
        updateList();
        return;
        }
        File f = new File(current);
        if (!results) {
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode==1)
                    try {
                        if (!current.equals(smbPath)) {
                            String path = (new SmbFile(current).getParent());
                            loadCustomList((path), true);
                        } else loadlist(home, false,false);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                else if (current.equals("/"))
                    mainActivity.exit();
                else if (utils.canGoBack(f)) {
                    loadlist(f.getParent(), true,false);
                } else mainActivity.exit();
            }
        } else {
            loadlist(f.getPath(), true,false);
        }
    }

    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    public void updateList() {
        computeScroll();
        ic.cleanup();
        loadlist((current), true,true);
    }

    public void getSortModes() {
        int t = Integer.parseInt(Sp.getString("sortby", "0"));
        if (t <= 3) {
            sortby = t;
            asc = 1;
        } else if (t > 3) {
            asc = -1;
            sortby = t - 4;
        }
        dsort = Integer.parseInt(Sp.getString("dirontop", "0"));

    }

    @Override
    public void onResume() {
        super.onResume();
        (getActivity()).registerReceiver(receiver2, new IntentFilter("loadlist"));
    }

    @Override
    public void onPause() {
        super.onPause();
        (getActivity()).unregisterReceiver(receiver2);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tabHandler.close();
        if (history != null)
            history.end();
        if (hidden != null)
            hidden.end();
    }

    public ArrayList<Layoutelements> addToSmb(SmbFile[] mFile) throws SmbException {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        if (searchHelper.size() > 500) searchHelper.clear();
        for (int i = 0; i < mFile.length; i++) {
            searchHelper.add(mFile[i].getPath());
            if (mFile[i].isDirectory()) {
                folder_count++;
                a.add(new Layoutelements(folder, mFile[i].getName(), mFile[i].getPath(), "", "", "", 0, false, mFile[i].lastModified() + "", true));
            } else {
                file_count++;
                try {
                    a.add(new Layoutelements(Icons.loadMimeIcon(getActivity(), mFile[i].getPath(), !islist, res), mFile[i].getName(), mFile[i].getPath(), "", "", utils.readableFileSize(mFile[i].length()), mFile[1].length(), false, mFile[i].lastModified() + "", false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }

    public ArrayList<Layoutelements> addTo(ArrayList<String[]> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        if (searchHelper.size() > 500) searchHelper.clear();
        for (int i = 0; i < mFile.size(); i++) {
            String[] ele = mFile.get(i);
            File f = new File(ele[0]);
            searchHelper.add(f.getPath());
            String size = "";
            if (!hiddenfiles.contains(ele[0])) {
                if (isDirectory(ele)) {
                    if (!ele[5].trim().equals("") && !ele[5].toLowerCase().trim().equals("null"))
                        size = ele[5] + " " + itemsstring;
                    else size = "";
                    a.add(utils.newElement(folder, f.getPath(), mFile.get(i)[2], mFile.get(i)[1], size, 0, true, false, ele[4]));
                    folder_count++;
                } else {
                    long longSize = 0;
                    try {
                        if (!ele[5].trim().equals("") && !ele[5].toLowerCase().trim().equals("null")) {
                            size = utils.readableFileSize(Long.parseLong(ele[5]));
                            longSize = Long.parseLong(ele[5]);
                        } else {
                            size = "";
                            longSize = 0;
                        }
                    } catch (NumberFormatException e) {
                        //e.printStackTrace();
                    }
                    try {
                        a.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f.getPath(), !islist, res), f.getPath(), mFile.get(i)[2], mFile.get(i)[1], size, longSize, false, false, ele[4]));
                        file_count++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return a;
    }

    public boolean isDirectory(String[] path) {
        if (rootMode)
            if (path[1].length() != 0) return new File(path[0]).isDirectory();
            else return path[3].equals("-1");
        else
            return new File(path[0]).isDirectory();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();


    }


    public SmbFile connectingWithSmbServer(String[] auth, boolean anonym) {
        try {
            String yourPeerIP = auth[0],domain="";
            String path = "smb://" + (anonym ? "" : (URLEncoder.encode(auth[1] + ":" + auth[2], "UTF-8") + "@")) + yourPeerIP + "/";
            smbPath = path;
            SmbFile smbFile = new SmbFile(path);
            return smbFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updatehiddenfiles() {
        hiddenfiles = hidden.readTable();
    }


    public void hide(String path) {
        hidden.addPath(path);
        hiddenfiles = hidden.readTable();
        if (new File(path).isDirectory()) {
            File f1 = new File(path + "/" + ".nomedia");
            if (!f1.exists()) {
                try {
                    mainActivity.mkFile(f1.getPath(), this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            utils.scanFile(path, getActivity());
        }

    }

    public void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        Intent i = new Intent(getActivity(), MainActivity.class);
        i.putExtra("restart", true);
        i.setAction(Intent.ACTION_MAIN);
        activity.startActivity(i);
    }

    public String getSelectionColor() {

        String[] colors = new String[]{
                "#F44336", "#74e84e40",
                "#e91e63", "#74ec407a",
                "#9c27b0", "#74ab47bc",
                "#673ab7", "#747e57c2",
                "#3f51b5", "#745c6bc0",
                "#2196F3", "#74738ffe",
                "#03A9F4", "#7429b6f6",
                "#00BCD4", "#7426c6da",
                "#009688", "#7426a69a",
                "#4CAF50", "#742baf2b",
                "#8bc34a", "#749ccc65",
                "#FFC107", "#74ffca28",
                "#FF9800", "#74ffa726",
                "#FF5722", "#74ff7043",
                "#795548", "#748d6e63",
                "#212121", "#79bdbdbd",
                "#607d8b", "#7478909c",
                "#004d40", "#740E5D50"
        };
        return colors[Arrays.asList(colors).indexOf(skin) + 1];
    }

    public float[] calculatefilter(float[] values) {
        float[] src = {

                values[0], 0, 0, 0, 0,
                0, values[1], 0, 0, 0,
                0, 0, values[2], 0, 0,
                0, 0, 0, 1, 0
        };
        return src;
    }

    public float[] calculatevalues(String color) {
        int c = Color.parseColor(color);
        float r = (float) Color.red(c) / 255;
        float g = (float) Color.green(c) / 255;
        float b = (float) Color.blue(c) / 255;
        return new float[]{r, g, b};
    }

    private void addShortcut(Layoutelements path) {
        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getActivity().getApplicationContext(),
                MainActivity.class);
        shortcutIntent.putExtra("path", path.getDesc());
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, new File(path.getDesc()).getName());

        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getActivity(),
                        R.mipmap.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getActivity().sendBroadcast(addIntent);
    }

    public void addSearchResult(ArrayList<String[]> a) {
        if (listView != null) {
            if (!results){
                list.clear();
                file_count=0;
                folder_count=0;
            }
            ArrayList<Layoutelements>  arrayList1= addTo(a);
            if (arrayList1.size() > 0)
            for(Layoutelements layoutelements:arrayList1)
            list.add(layoutelements);
            if (!results) {
                createViews(list, false, (current),openMode,true);
            }
            pathname.setText(R.string.searching);
            if (results) {
                adapter.addItem();
            }
            results = true;
            stopAnimation();
        }
    }
    public void onSearchCompleted() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Collections.sort(list, new FileListSorter(dsort, sortby, asc, rootMode));
                return null;
            }

            @Override
            public void onPostExecute(Void c) {
                createViews(list, true, (current),openMode,true);
                pathname.setText(R.string.searchresults);
                results = true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    Streamer s;

    private void launch(final SmbFile smbFile) {
        s = Streamer.getInstance();
        new Thread() {
            public void run() {
                try {
                    s.setStreamSrc(smbFile, null);//the second argument can be a list of subtitle files
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(smbFile.getPath()).getPath())).getEncodedPath());
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setDataAndType(uri, MimeTypes.getMimeType(new File(smbFile.getPath())));
                                PackageManager packageManager = getActivity().getPackageManager();
                                List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                                if (resInfos != null && resInfos.size() > 0)
                                    startActivity(i);
                                else
                                    Toast.makeText(getActivity(), "You will need to copy this file to storage to open it", Toast.LENGTH_SHORT).show();
                            } catch (ActivityNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}