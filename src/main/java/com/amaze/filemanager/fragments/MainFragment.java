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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.EncryptedEntry;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.MediaStoreHack;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.services.EncryptService;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.ui.LayoutElements;
import com.amaze.filemanager.ui.icons.IconHolder;
import com.amaze.filemanager.ui.icons.IconUtils;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.CryptUtil;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.SmbStreamer.Streamer;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainFragment extends android.support.v4.app.Fragment {

    public ArrayList<LayoutElements> LIST_ELEMENTS;
    public RecyclerAdapter adapter;
    public ActionMode mActionMode;
    public SharedPreferences sharedPref;
    public BitmapDrawable folder, apk, DARK_IMAGE, DARK_VIDEO;
    public LinearLayout buttons;
    public int sortby, dsort, asc;
    public String home, CURRENT_PATH = "", goback;
    public boolean selection, results = false, SHOW_HIDDEN, CIRCULAR_IMAGES, SHOW_PERMISSIONS,
            SHOW_SIZE, SHOW_LAST_MODIFIED;
    public LinearLayout pathbar;
    public OpenMode openMode = OpenMode.FILE;
    public android.support.v7.widget.RecyclerView listView;

    public boolean GO_BACK_ITEM, SHOW_THUMBS, COLORISE_ICONS, SHOW_DIVIDERS;

    /**
     * {@link MainFragment#IS_LIST} boolean to identify if the view is a list or grid
     */
    public boolean IS_LIST = true;
    public IconHolder ic;
    public MainActivity MAIN_ACTIVITY;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    public int file_count, folder_count, columns;
    public String smbPath;
    public ArrayList<BaseFile> searchHelper = new ArrayList<>();
    public Resources res;
    public int no;

    // ATTRIBUTES FOR APPEARANCE AND COLORS
    public String fabSkin, iconskin;
    public float[] color;
    public int skin_color;
    public int skinTwoColor;
    public int icon_skin_color;

    private IconUtils icons;
    private View footerView;
    private String itemsstring;
    private TabHandler tabHandler;
    private LinearLayoutManager mLayoutManager;
    private GridLayoutManager mLayoutManagerGrid;
    private boolean addheader = false;
    private StickyRecyclerHeadersDecoration headersDecor;
    private DividerItemDecoration dividerItemDecoration;
    private int hidemode;
    private AppBarLayout mToolbarContainer;
    private TextView pathname, mFullPath;
    private boolean stopAnims = true;
    private View nofilesview;
    private DisplayMetrics displayMetrics;
    private HFile f;
    private Streamer s;

    private UtilitiesProviderInterface utilsProvider;
    private Futils utils;
    private HashMap<String, Bundle> scrolls = new HashMap<>();
    private MainFragment ma = this;
    private View rootView;
    private View actionModeView;
    private FastScroller fastScroller;
    private Bitmap mFolderBitmap;

    // defines the current visible tab, default either 0 or 1
    //private int mCurrentTab;

    /*
     * boolean identifying if the search task should be re-run on back press after pressing on
     * any of the search result
     */
    private boolean mRetainSearchTask = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MAIN_ACTIVITY = (MainActivity) getActivity();
        utilsProvider = MAIN_ACTIVITY;
        utils = utilsProvider.getFutils();

        setRetainInstance(true);
        no = getArguments().getInt("no", 1);
        home = getArguments().getString("home");
        CURRENT_PATH = getArguments().getString("lastpath");
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hidemode = sharedPref.getInt("hidemode", 0);

        fabSkin = MAIN_ACTIVITY.getColorPreference().getColorAsString(ColorUsage.ACCENT);
        iconskin = MAIN_ACTIVITY.getColorPreference().getColorAsString(ColorUsage.ICON_SKIN);
        skin_color = MAIN_ACTIVITY.getColorPreference().getColor(ColorUsage.PRIMARY);
        skinTwoColor = MAIN_ACTIVITY.getColorPreference().getColor(ColorUsage.PRIMARY_TWO);
        icon_skin_color = Color.parseColor(iconskin);

        SHOW_PERMISSIONS = sharedPref.getBoolean("showPermissions", false);
        SHOW_SIZE = sharedPref.getBoolean("showFileSize", false);
        SHOW_DIVIDERS = sharedPref.getBoolean("showDividers", true);
        GO_BACK_ITEM = sharedPref.getBoolean("goBack_checkbox", false);
        CIRCULAR_IMAGES = sharedPref.getBoolean("circularimages", true);
        SHOW_LAST_MODIFIED = sharedPref.getBoolean("showLastModified", true);
        icons = new IconUtils(sharedPref, getActivity());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        MAIN_ACTIVITY = (MainActivity) context;
    }

    public void stopAnimation() {
        if ((!adapter.stoppedAnimation)) {
            for (int j = 0; j < listView.getChildCount(); j++) {
                View v = listView.getChildAt(j);
                if (v != null) v.clearAnimation();
            }
        }
        adapter.stoppedAnimation = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.main_frag, container, false);
        setRetainInstance(true);
        listView = (android.support.v7.widget.RecyclerView) rootView.findViewById(R.id.listView);
        mToolbarContainer = (AppBarLayout) getActivity().findViewById(R.id.lin);
        fastScroller = (FastScroller) rootView.findViewById(R.id.fastscroll);
        fastScroller.setPressedHandleColor(Color.parseColor(fabSkin));
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (adapter != null && stopAnims) {
                    stopAnimation();
                    stopAnims = false;
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

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadlist((CURRENT_PATH), false, openMode);
            }
        });
        buttons = (LinearLayout) getActivity().findViewById(R.id.buttons);
        pathbar = (LinearLayout) getActivity().findViewById(R.id.pathbar);
        SHOW_THUMBS = sharedPref.getBoolean("showThumbs", true);
        res = getResources();
        pathname = (TextView) getActivity().findViewById(R.id.pathname);
        mFullPath = (TextView) getActivity().findViewById(R.id.fullpath);
        itemsstring = res.getString(R.string.items);
        apk = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_doc_apk_grid));
        mToolbarContainer.setBackgroundColor(MainActivity.currentTab==1 ? skinTwoColor : skin_color);

        if (!sharedPref.getBoolean("intelliHideToolbar", true)){
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) getActivity()
                    .findViewById(R.id.action_bar).getLayoutParams();
            params.setScrollFlags(0);
            mToolbarContainer.setExpanded(true, true);
        }

        //   listView.setPadding(listView.getPaddingLeft(), paddingTop, listView.getPaddingRight(), listView.getPaddingBottom());
        return rootView;
    }

    public int dpToPx(int dp) {
        if (displayMetrics == null) displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(false);
        //MAIN_ACTIVITY = (MainActivity) getActivity();
        initNoFileLayout();
        SHOW_HIDDEN = sharedPref.getBoolean("showHidden", false);
        COLORISE_ICONS = sharedPref.getBoolean("coloriseIcons", true);
        mFolderBitmap = BitmapFactory.decodeResource(res, R.drawable.ic_grid_folder_new);
        goback = res.getString(R.string.goback);
        folder = new BitmapDrawable(res, mFolderBitmap);
        getSortModes();
        DARK_IMAGE = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_doc_image_dark));
        DARK_VIDEO = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_doc_video_dark));
        this.setRetainInstance(false);
        f = new HFile(OpenMode.UNKNOWN, CURRENT_PATH);
        f.generateMode(getActivity());
        MAIN_ACTIVITY.initiatebbar();
        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT) && !IS_LIST)  listView.setBackgroundColor(getResources()
                .getColor(R.color.grid_background_light));
        else    listView.setBackgroundDrawable(null);

        listView.setHasFixedSize(true);
        columns = Integer.parseInt(sharedPref.getString("columns", "-1"));
        if (IS_LIST) {
            mLayoutManager = new LinearLayoutManager(getActivity());
            listView.setLayoutManager(mLayoutManager);
        } else {
            if (columns == -1 || columns == 0)
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), 3);
            else
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
            listView.setLayoutManager(mLayoutManagerGrid);
        }
        // use a linear layout manager
        footerView = getActivity().getLayoutInflater().inflate(R.layout.divider, null);
        dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST, false, SHOW_DIVIDERS);
        listView.addItemDecoration(dividerItemDecoration);
        mSwipeRefreshLayout.setColorSchemeColors(Color.parseColor(fabSkin));
        DefaultItemAnimator animator = new DefaultItemAnimator();
        listView.setItemAnimator(animator);
        mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if ((columns == 0 || columns == -1)) {
                    int screen_width = listView.getWidth();
                    int dptopx = dpToPx(115);
                    columns = screen_width / dptopx;
                    if (columns == 0 || columns == -1) columns = 3;
                    if (!IS_LIST) mLayoutManagerGrid.setSpanCount(columns);
                }
                if (savedInstanceState != null && !IS_LIST)
                    retrieveFromSavedInstance(savedInstanceState);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }

        });
        if (savedInstanceState == null) {
            loadlist(CURRENT_PATH, false, openMode);

        } else {
            if (IS_LIST)
                retrieveFromSavedInstance(savedInstanceState);
        }
    }

    void switchToGrid() {
        IS_LIST = false;

        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);
        folder = new BitmapDrawable(res, mFolderBitmap);
        fixIcons(true);

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

            // will always be grid, set alternate white background
            listView.setBackgroundColor(getResources().getColor(R.color.grid_background_light));
        }

        if (mLayoutManagerGrid == null)
            if (columns == -1 || columns == 0)
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), 3);
            else
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
        listView.setLayoutManager(mLayoutManagerGrid);
        adapter = null;
    }

    void switchToList() {
        IS_LIST = true;

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

            listView.setBackgroundDrawable(null);
        }

        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);
        folder = new BitmapDrawable(res, mFolderBitmap);
        fixIcons(true);
        if (mLayoutManager == null)
            mLayoutManager = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(mLayoutManager);
        adapter = null;
    }

    public void switchView() {
        createViews(LIST_ELEMENTS, false, CURRENT_PATH, openMode, results, checkforpath(CURRENT_PATH));
    }

    void retrieveFromSavedInstance(final Bundle savedInstanceState) {

        Bundle b = new Bundle();
        String cur = savedInstanceState.getString("CURRENT_PATH");
        if (cur != null) {
            b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);

            openMode = OpenMode.getOpenMode(savedInstanceState.getInt("openMode", 0));
            if (openMode == OpenMode.SMB)
                smbPath = savedInstanceState.getString("SmbPath");
            LIST_ELEMENTS = savedInstanceState.getParcelableArrayList("list");
            CURRENT_PATH = cur;
            folder_count = savedInstanceState.getInt("folder_count", 0);
            file_count = savedInstanceState.getInt("file_count", 0);
            results = savedInstanceState.getBoolean("results");
            MAIN_ACTIVITY.updatePath(CURRENT_PATH, results, openMode, folder_count, file_count);
            createViews(LIST_ELEMENTS, true, (CURRENT_PATH), openMode, results, !IS_LIST);
            if (savedInstanceState.getBoolean("selection")) {

                for (int i : savedInstanceState.getIntegerArrayList("position")) {
                    adapter.toggleChecked(i, null);
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
            if (IS_LIST) {

                index = (mLayoutManager).findFirstVisibleItemPosition();
                vi = listView.getChildAt(0);
            } else {
                index = (mLayoutManagerGrid).findFirstVisibleItemPosition();
                vi = listView.getChildAt(0);
            }
            int top = (vi == null) ? 0 : vi.getTop();
            outState.putInt("index", index);
            outState.putInt("top", top);
            //outState.putBoolean("IS_LIST", IS_LIST);
            outState.putParcelableArrayList("list", LIST_ELEMENTS);
            outState.putString("CURRENT_PATH", CURRENT_PATH);
            outState.putBoolean("selection", selection);
            outState.putInt("openMode", openMode.ordinal());
            outState.putInt("folder_count", folder_count);
            outState.putInt("file_count", file_count);
            if (selection) {
                outState.putIntegerArrayList("position", adapter.getCheckedItemPositions());
            }
            outState.putBoolean("results", results);
            if (openMode == OpenMode.SMB) {
                outState.putString("SmbPath", smbPath);
            }
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
            actionModeView = getActivity().getLayoutInflater().inflate(R.layout.actionmode, null);
            mode.setCustomView(actionModeView);

            MAIN_ACTIVITY.setPagingEnabled(false);
            MAIN_ACTIVITY.floatingActionButton.hideMenuButton(true);

            // translates the drawable content down
            // if (MAIN_ACTIVITY.isDrawerLocked) MAIN_ACTIVITY.translateDrawerList(true);

            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.openwith, menu);
            if (MAIN_ACTIVITY.mReturnIntent)
                showOption(R.id.openmulti, menu);
            //hideOption(R.id.setringtone,menu);
            mode.setTitle(getResources().getString(R.string.select));

            MAIN_ACTIVITY.updateViews(new ColorDrawable(res.getColor(R.color.holo_dark_action_mode)));

            // do not allow drawer to open when item gets selected
            if (!MAIN_ACTIVITY.isDrawerLocked) {

                MAIN_ACTIVITY.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED,
                        MAIN_ACTIVITY.mDrawerLinear);
            }
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
            if (openMode == OpenMode.SMB) {
                hideOption(R.id.addshortcut, menu);
                hideOption(R.id.openwith, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.compress, menu);
                return true;
            }
            if (MAIN_ACTIVITY.mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16)
                    showOption(R.id.openmulti, menu);
            //tv.setText(positions.size());
            if (!results) {
                hideOption(R.id.openparent, menu);
                if (positions.size() == 1) {
                    showOption(R.id.addshortcut, menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.share, menu);

                    File x = new File(LIST_ELEMENTS.get(adapter.getCheckedItemPositions().get(0))

                            .getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith, menu);
                        hideOption(R.id.share, menu);
                        hideOption(R.id.openmulti, menu);
                    }

                    if (MAIN_ACTIVITY.mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);

                } else {
                    try {
                        showOption(R.id.share, menu);
                        if (MAIN_ACTIVITY.mReturnIntent)
                            if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
                        for (int c : adapter.getCheckedItemPositions()) {
                            File x = new File(LIST_ELEMENTS.get(c).getDesc());
                            if (x.isDirectory()) {
                                hideOption(R.id.share, menu);
                                hideOption(R.id.openmulti, menu);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    hideOption(R.id.openwith, menu);

                }
            } else {
                if (positions.size() == 1) {
                    showOption(R.id.addshortcut, menu);
                    showOption(R.id.openparent, menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.share, menu);

                    File x = new File(LIST_ELEMENTS.get(adapter.getCheckedItemPositions().get(0))

                            .getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith, menu);
                        hideOption(R.id.share, menu);
                        hideOption(R.id.openmulti, menu);
                    }
                    if (MAIN_ACTIVITY.mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);

                } else {
                    hideOption(R.id.openparent, menu);

                    if (MAIN_ACTIVITY.mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);
                    try {
                        for (int c : adapter.getCheckedItemPositions()) {
                            File x = new File(LIST_ELEMENTS.get(c).getDesc());
                            if (x.isDirectory()) {
                                hideOption(R.id.share, menu);
                                hideOption(R.id.openmulti, menu);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    hideOption(R.id.openwith, menu);

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
                        ArrayList<Uri> resulturis = new ArrayList<>();
                        for (int k : plist) {
                            try {
                                resulturis.add(Uri.fromFile(new File(LIST_ELEMENTS.get(k).getDesc())));
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
                case R.id.about:
                    LayoutElements x;
                    x = LIST_ELEMENTS.get((plist.get(0)));
                    utils.showProps((x).generateBaseFile(), x.getPermissions(), ma, BaseActivity.rootMode, utilsProvider.getAppTheme());
                    /*PropertiesSheet propertiesSheet = new PropertiesSheet();
                    Bundle arguments = new Bundle();
                    arguments.putParcelable(PropertiesSheet.KEY_FILE, x.generateBaseFile());
                    arguments.putString(PropertiesSheet.KEY_PERMISSION, x.getPermissions());
                    arguments.putBoolean(PropertiesSheet.KEY_ROOT, BaseActivity.rootMode);
                    propertiesSheet.setArguments(arguments);
                    propertiesSheet.show(getFragmentManager(), PropertiesSheet.TAG_FRAGMENT);*/
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
                    utils.deleteFiles(LIST_ELEMENTS, ma, plist, utilsProvider.getAppTheme());
                    return true;
                case R.id.share:
                    ArrayList<File> arrayList = new ArrayList<>();
                    for (int i : plist) {
                        arrayList.add(new File(LIST_ELEMENTS.get(i).getDesc()));
                    }
                    if (arrayList.size() > 100)
                        Toast.makeText(getActivity(), getResources().getString(R.string.share_limit),
                                Toast.LENGTH_SHORT).show();
                    else
                        utils.shareFiles(arrayList, getActivity(), utilsProvider.getAppTheme(), Color.parseColor(fabSkin));
                    return true;
                case R.id.openparent:
                    loadlist(new File(LIST_ELEMENTS.get(plist.get(0)).getDesc()).getParent(), false, OpenMode.FILE);
                    return true;
                case R.id.all:
                    if (adapter.areAllChecked(CURRENT_PATH)) {
                        adapter.toggleChecked(false, CURRENT_PATH);
                    } else {
                        adapter.toggleChecked(true, CURRENT_PATH);
                    }
                    mode.invalidate();

                    return true;
                case R.id.rename:

                    final ActionMode m = mode;
                    final BaseFile f;
                    f = (LIST_ELEMENTS.get(
                            (plist.get(0)))).generateBaseFile();
                    rename(f);
                    mode.finish();
                    return true;
                case R.id.hide:
                    for (int i1 = 0; i1 < plist.size(); i1++) {
                        hide(LIST_ELEMENTS.get(plist.get(i1)).getDesc());
                    }
                    updateList();
                    mode.finish();
                    return true;
                case R.id.ex:
                    MAIN_ACTIVITY.mainActivityHelper.extractFile(new File(LIST_ELEMENTS.get(plist.get(0)).getDesc()));
                    mode.finish();
                    return true;
                case R.id.cpy:
                    MAIN_ACTIVITY.MOVE_PATH = null;
                    ArrayList<BaseFile> copies = new ArrayList<>();
                    for (int i2 = 0; i2 < plist.size(); i2++) {
                        copies.add(LIST_ELEMENTS.get(plist.get(i2)).generateBaseFile());
                    }
                    MAIN_ACTIVITY.COPY_PATH = copies;
                    MAIN_ACTIVITY.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.cut:
                    MAIN_ACTIVITY.COPY_PATH = null;
                    ArrayList<BaseFile> copie = new ArrayList<>();
                    for (int i3 = 0; i3 < plist.size(); i3++) {
                        copie.add(LIST_ELEMENTS.get(plist.get(i3)).generateBaseFile());
                    }
                    MAIN_ACTIVITY.MOVE_PATH = copie;
                    MAIN_ACTIVITY.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.compress:
                    ArrayList<BaseFile> copies1 = new ArrayList<>();
                    for (int i4 = 0; i4 < plist.size(); i4++) {
                        copies1.add(LIST_ELEMENTS.get(plist.get(i4)).generateBaseFile());
                    }
                    utils.showCompressDialog((MainActivity) getActivity(), copies1, CURRENT_PATH);
                    mode.finish();
                    return true;
                case R.id.openwith:
                    utils.openunknown(new File(LIST_ELEMENTS.get((plist.get(0))).getDesc()), getActivity(), true);
                    return true;
                case R.id.addshortcut:
                    addShortcut(LIST_ELEMENTS.get(plist.get(0)));
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

            // translates the drawer content up
            //if (MAIN_ACTIVITY.isDrawerLocked) MAIN_ACTIVITY.translateDrawerList(false);

            MAIN_ACTIVITY.floatingActionButton.showMenuButton(true);
            if (!results) adapter.toggleChecked(false, CURRENT_PATH);
            else adapter.toggleChecked(false);
            MAIN_ACTIVITY.setPagingEnabled(true);

            MAIN_ACTIVITY.updateViews(new ColorDrawable(MainActivity.currentTab==1 ?
                    skinTwoColor : skin_color));

            if (!MAIN_ACTIVITY.isDrawerLocked) {
                MAIN_ACTIVITY.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
                        MAIN_ACTIVITY.mDrawerLinear);
            }
        }
    };

    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    public void home() {
        ma.loadlist((ma.home), false, OpenMode.FILE);
    }

    /**
     * method called when list item is clicked in the adapter
     * @param position the {@link int} position of the list item
     * @param imageView the check {@link RoundedImageView} that is to be animated
     */
    public void onListItemClicked(int position, ImageView imageView) {
        if (position >= LIST_ELEMENTS.size()) return;

        if (results) {

            // check to initialize search results
            // if search task is been running, cancel it
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            SearchAsyncHelper fragment = (SearchAsyncHelper) fragmentManager
                    .findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);
            if (fragment != null) {

                if (fragment.mSearchTask.getStatus() == AsyncTask.Status.RUNNING) {

                    fragment.mSearchTask.cancel(true);
                }
                getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }

            mRetainSearchTask = true;
            results = false;
        } else {
            mRetainSearchTask = false;
            MainActivityHelper.SEARCH_TEXT = null;
        }
        if (selection) {
            if (!LIST_ELEMENTS.get(position).getSize().equals(goback)) {
                // the first {goback} item if back navigation is enabled
                adapter.toggleChecked(position, imageView);
            } else {
                selection = false;
                if (mActionMode != null)
                    mActionMode.finish();
                mActionMode = null;
            }

        } else {
            if (!LIST_ELEMENTS.get(position).getSize().equals(goback)) {

                // hiding search view if visible
                if (MainActivity.isSearchViewEnabled)   MAIN_ACTIVITY.hideSearchView();

                String path;
                LayoutElements l = LIST_ELEMENTS.get(position);
                if (!l.hasSymlink()) {

                    path = l.getDesc();
                } else {

                    path = l.getSymlink();
                }

                // check if we're trying to click on encrypted file
                if (!LIST_ELEMENTS.get(position).isDirectory() &&
                        LIST_ELEMENTS.get(position).getDesc().endsWith(CryptUtil.CRYPT_EXTENSION)) {
                    // decrypt the file
                    MAIN_ACTIVITY.isEncryptOpen = true;

                    MAIN_ACTIVITY.encryptBaseFile = new BaseFile(getActivity().getExternalCacheDir().getPath()
                            + "/"
                            + LIST_ELEMENTS.get(position).generateBaseFile().getName().replace(CryptUtil.CRYPT_EXTENSION, ""));

                    decryptFile(this, openMode, LIST_ELEMENTS.get(position).generateBaseFile(),
                            getActivity().getExternalCacheDir().getPath(),
                            utilsProvider);
                    return;
                }

                if (LIST_ELEMENTS.get(position).isDirectory()) {
                    computeScroll();
                    loadlist(path, false, openMode);
                } else {
                    if (l.getMode() == OpenMode.SMB) {
                        try {
                            SmbFile smbFile = new SmbFile(l.getDesc());
                            launch(smbFile, l.getlongSize(), MAIN_ACTIVITY);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    } else if (l.getMode() == OpenMode.OTG) {

                        utils.openFile(RootHelper.getDocumentFile(l.getDesc(), getContext(), false),
                                (MainActivity) getActivity());
                    }
                    else if (MAIN_ACTIVITY.mReturnIntent) {
                        returnIntentResults(new File(l.getDesc()));
                    } else {

                        utils.openFile(new File(l.getDesc()), (MainActivity) getActivity());
                    }
                    DataUtils.addHistoryFile(l.getDesc());
                }
            } else {
                goBackItemClick();
            }
        }
    }

    public static void decryptFile(final MainFragment main, OpenMode openMode, BaseFile sourceFile,
                                   String decryptPath,
                                   UtilitiesProviderInterface utilsProvider) {

        Intent decryptIntent = new Intent(main.getContext(), EncryptService.class);
        decryptIntent.putExtra(EncryptService.TAG_OPEN_MODE, openMode.ordinal());
        decryptIntent.putExtra(EncryptService.TAG_CRYPT_MODE,
                EncryptService.CryptEnum.DECRYPT.ordinal());
        decryptIntent.putExtra(EncryptService.TAG_SOURCE, sourceFile);
        decryptIntent.putExtra(EncryptService.TAG_DECRYPT_PATH, decryptPath);

        SharedPreferences preferences1 = PreferenceManager.getDefaultSharedPreferences(main.getContext());

        EncryptedEntry encryptedEntry;
        try {
            encryptedEntry = findEncryptedEntry(main.getContext(), sourceFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            encryptedEntry = null;
        }

        if (encryptedEntry == null) {

            // we couldn't find any entry in database or lost the key to decipher
            Toast.makeText(main.getContext(),
                    main.getActivity().getResources().getString(R.string.crypt_decryption_fail),
                    Toast.LENGTH_LONG).show();
            return;
        }

        RecyclerAdapter.DecryptButtonCallbackInterface decryptButtonCallbackInterface =
                new RecyclerAdapter.DecryptButtonCallbackInterface() {
                    @Override
                    public void confirm(Intent intent) {

                        ServiceWatcherUtil.runService(main.getContext(), intent);
                    }

                    @Override
                    public void failed() {
                        Toast.makeText(main.getContext(), main.getActivity().getResources().getString(R.string.crypt_decryption_fail_password),
                                Toast.LENGTH_LONG).show();
                    }
                };

        switch (encryptedEntry.getPassword()) {
            case Preffrag.ENCRYPT_PASSWORD_FINGERPRINT:
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        utilsProvider.getFutils().showDecryptFingerprintDialog(decryptIntent,
                                main, utilsProvider.getAppTheme(), decryptButtonCallbackInterface);
                    } else throw new Exception();
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(main.getContext(),
                            main.getResources().getString(R.string.crypt_decryption_fail),
                            Toast.LENGTH_LONG).show();
                }
                break;
            case Preffrag.ENCRYPT_PASSWORD_MASTER:
                utilsProvider.getFutils().showDecryptDialog(decryptIntent,
                        main, utilsProvider.getAppTheme(),
                        preferences1.getString(Preffrag.PREFERENCE_CRYPT_MASTER_PASSWORD,
                                Preffrag.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT),
                        decryptButtonCallbackInterface);
                break;
            default:
                utilsProvider.getFutils().showDecryptDialog(decryptIntent,
                        main, utilsProvider.getAppTheme(),
                        encryptedEntry.getPassword(),
                        decryptButtonCallbackInterface);
                break;
        }
    }

    /**
     * Queries database to find entry for the specific path
     * @param path  the path to match with
     * @return the entry
     */
    private static EncryptedEntry findEncryptedEntry(Context context, String path) throws Exception {

        CryptHandler handler = new CryptHandler(context);

        EncryptedEntry matchedEntry = null;
        // find closest path which matches with database entry
        for (EncryptedEntry encryptedEntry : handler.getAllEntries()) {
            if (path.contains(encryptedEntry.getPath())) {

                if (matchedEntry == null || (matchedEntry != null &&
                        matchedEntry.getPath().length()<encryptedEntry.getPath().length())) {
                    matchedEntry = encryptedEntry;
                }
            }
        }
        return matchedEntry;
    }

    public void updateTabWithDb(Tab tab) {
        CURRENT_PATH = tab.getPath();
        home = tab.getHome();
        loadlist(CURRENT_PATH, false, OpenMode.UNKNOWN);
    }

    private void returnIntentResults(File file) {
        MAIN_ACTIVITY.mReturnIntent = false;

        Intent intent = new Intent();
        if (MAIN_ACTIVITY.mRingtonePickerIntent) {

            Uri mediaStoreUri = MediaStoreHack.getUriFromFile(file.getPath(), getActivity());
            System.out.println(mediaStoreUri.toString() + "\t" + MimeTypes.getMimeType(file));
            intent.setDataAndType(mediaStoreUri, MimeTypes.getMimeType(file));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, mediaStoreUri);
            getActivity().setResult(getActivity().RESULT_OK, intent);
            getActivity().finish();
        } else {

            Log.d("pickup", "file");
            intent.setData(Uri.fromFile(file));
            getActivity().setResult(getActivity().RESULT_OK, intent);
            getActivity().finish();
        }
    }

    LoadList loadList;

    public void loadlist(String path, boolean back, OpenMode openMode) {
        if (mActionMode != null) {
            mActionMode.finish();
        }
        /*if(openMode==-1 && android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches())
            bindDrive(path);
        else */
        if (loadList != null) loadList.cancel(true);
        loadList = new LoadList(ma.getActivity(), utilsProvider, back, ma, openMode);
        loadList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (path));

    }

    void initNoFileLayout() {
        nofilesview = rootView.findViewById(R.id.nofilelayout);
        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
            ((ImageView) nofilesview.findViewById(R.id.image)).setColorFilter(Color.parseColor("#666666"));
        else {
            nofilesview.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            ((TextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
        }
    }

    public boolean checkforpath(String path) {
        boolean grid = false, both_contain = false;
        int index1 = -1, index2 = -1;
        for (String s : DataUtils.gridfiles) {
            index1++;
            if ((path).contains(s)) {
                grid = true;
                break;
            }
        }
        for (String s : DataUtils.listfiles) {
            index2++;
            if ((path).contains(s)) {
                if (grid) both_contain = true;
                grid = false;
                break;
            }
        }
        if (!both_contain) return grid;
        String path1 = DataUtils.gridfiles.get(index1), path2 = DataUtils.listfiles.get(index2);
        if (path1.contains(path2))
            return true;
        else if (path2.contains(path1))
            return false;
        else
            return grid;
    }

    /**
     * Loading adapter after getting a list of elements
     * @param bitmap the list of objects for the adapter
     * @param back
     * @param path the path for the adapter
     * @param openMode the type of file being created
     * @param results is the list of elements a result from search
     * @param grid whether to set grid view or list view
     */
    public void createViews(ArrayList<LayoutElements> bitmap, boolean back, String path, OpenMode
            openMode, boolean results, boolean grid) {
        try {
            if (bitmap != null) {
                if (GO_BACK_ITEM)
                    if (!path.equals("/") && (openMode == OpenMode.FILE || openMode == OpenMode.ROOT)
                            && !path.equals("otg:/")) {
                        if (bitmap.size() == 0 || !bitmap.get(0).getSize().equals(goback)) {

                            Bitmap iconBitmap = BitmapFactory.decodeResource(res, R.drawable.ic_arrow_left_white_24dp);
                            bitmap.add(0,
                                    utils.newElement(new BitmapDrawable(res, iconBitmap),
                                            "..", "", "", goback, 0, false, true, ""));
                        }
                    }

                if (bitmap.size() == 0 && !results) {
                    nofilesview.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setEnabled(false);
                } else {
                    mSwipeRefreshLayout.setEnabled(true);
                    nofilesview.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);

                }
                LIST_ELEMENTS = bitmap;
                if (grid && IS_LIST)
                    switchToGrid();
                else if (!grid && !IS_LIST) switchToList();
                if (adapter == null)
                    adapter = new RecyclerAdapter(ma, utilsProvider, bitmap, ma.getActivity());
                else {
                    adapter.generate(LIST_ELEMENTS);
                }
                stopAnims = true;
                this.openMode = openMode;
                if (openMode != OpenMode.CUSTOM)
                    DataUtils.addHistoryFile(path);
                //mSwipeRefreshLayout.setRefreshing(false);
                try {
                    listView.setAdapter(adapter);
                    if (!addheader) {
                        listView.removeItemDecoration(headersDecor);
                        listView.removeItemDecoration(dividerItemDecoration);
                        addheader = true;
                    }
                    if (addheader && IS_LIST) {
                        dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST, true, SHOW_DIVIDERS);
                        listView.addItemDecoration(dividerItemDecoration);
                        headersDecor = new StickyRecyclerHeadersDecoration(adapter);
                        listView.addItemDecoration(headersDecor);
                        addheader = false;
                    }
                    if (!results) this.results = false;
                    CURRENT_PATH = path;
                    if (back) {
                        if (scrolls.containsKey(CURRENT_PATH)) {
                            Bundle b = scrolls.get(CURRENT_PATH);
                            if (IS_LIST)
                                mLayoutManager.scrollToPositionWithOffset(b.getInt("index"), b.getInt("top"));
                            else
                                mLayoutManagerGrid.scrollToPositionWithOffset(b.getInt("index"), b.getInt("top"));
                        }
                    }
                    //floatingActionButton.show();
                    MAIN_ACTIVITY.updatePaths(no);
                    listView.stopScroll();
                    fastScroller.setRecyclerView(listView, IS_LIST ? 1 : columns);
                    mToolbarContainer.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                        @Override
                        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                            fastScroller.updateHandlePosition(verticalOffset, 112);
                            //    fastScroller.setPadding(fastScroller.getPaddingLeft(),fastScroller.getTop(),fastScroller.getPaddingRight(),112+verticalOffset);
                            //      fastScroller.updateHandlePosition();
                        }
                    });
                    fastScroller.registerOnTouchListener(new FastScroller.onTouchListener() {
                        @Override
                        public void onTouch() {
                            if (stopAnims && adapter != null) {
                                stopAnimation();
                                stopAnims = false;
                            }
                        }
                    });
                    if (buttons.getVisibility() == View.VISIBLE) MAIN_ACTIVITY.bbar(this);
                    //MAIN_ACTIVITY.invalidateFab(openMode);
                } catch (Exception e) {
                }
            } else {
                // list loading cancelled
                // TODO: Add support for cancelling list loading
                loadlist(home, true, OpenMode.FILE);
            }
        } catch (Exception e) {
        }
    }

    /**
     * Show dialog to rename a file
     * @param f the file to rename
     */
    public void rename(final BaseFile f) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        String name = f.getName();
        builder.input("", name, false, new MaterialDialog.InputCallback() {
            @Override
            public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

            }
        });
        builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        builder.title(getResources().getString(R.string.rename));
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                String name = materialDialog.getInputEditText().getText().toString();
                if (f.isSmb())
                    if (f.isDirectory() && !name.endsWith("/"))
                        name = name + "/";

                MAIN_ACTIVITY.mainActivityHelper.rename(openMode, f.getPath(),
                        CURRENT_PATH + "/" + name, getActivity(), BaseActivity.rootMode);
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

                materialDialog.cancel();
            }
        });
        builder.positiveText(R.string.save);
        builder.negativeText(R.string.cancel);
        int color = Color.parseColor(fabSkin);
        builder.positiveColor(color).negativeColor(color).widgetColor(color);
        builder.build().show();
    }

    public void computeScroll() {
        View vi = listView.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        int index;
        if (IS_LIST)
            index = mLayoutManager.findFirstVisibleItemPosition();
        else index = mLayoutManagerGrid.findFirstVisibleItemPosition();
        Bundle b = new Bundle();
        b.putInt("index", index);
        b.putInt("top", top);
        scrolls.put(CURRENT_PATH, b);
    }

    public void goBack() {
        if (openMode == OpenMode.CUSTOM) {
            loadlist(home, false, OpenMode.FILE);
            return;
        }

        File f = new File(CURRENT_PATH);
        if (!results && !mRetainSearchTask) {

            // normal case
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode == OpenMode.SMB) {
                    try {
                        if (!smbPath.equals(CURRENT_PATH)) {
                            String path = (new SmbFile(CURRENT_PATH).getParent());
                            loadlist((path), true, openMode);
                        } else loadlist(home, false, OpenMode.FILE);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else if (CURRENT_PATH.equals("/") || CURRENT_PATH.equals(home) ||
                        CURRENT_PATH.equals("otg:/"))
                    MAIN_ACTIVITY.exit();
                else if (utils.canGoBack(f)) {
                    loadlist(f.getParent(), true, openMode);
                } else MAIN_ACTIVITY.exit();
            }
        } else if (!results && mRetainSearchTask) {

            // case when we had pressed on an item from search results and wanna go back
            // leads to resuming the search task

            if (MainActivityHelper.SEARCH_TEXT!=null) {

                // starting the search query again :O
                MAIN_ACTIVITY.mainFragment = (MainFragment) MAIN_ACTIVITY.getFragment().getTab();
                FragmentManager fm = MAIN_ACTIVITY.getSupportFragmentManager();

                // getting parent path to resume search from there
                String parentPath = new File(CURRENT_PATH).getParent();
                // don't fuckin' remove this line, we need to change
                // the path back to parent on back press
                CURRENT_PATH = parentPath;

                MainActivityHelper.addSearchFragment(fm, new SearchAsyncHelper(),
                        parentPath, MainActivityHelper.SEARCH_TEXT, openMode, BaseActivity.rootMode,
                        sharedPref.getBoolean(SearchAsyncHelper.KEY_REGEX, false),
                        sharedPref.getBoolean(SearchAsyncHelper.KEY_REGEX_MATCHES, false));
            } else loadlist(CURRENT_PATH, true, OpenMode.UNKNOWN);

            mRetainSearchTask = false;
        } else {
            // to go back after search list have been popped
            FragmentManager fm = getActivity().getSupportFragmentManager();
            SearchAsyncHelper fragment = (SearchAsyncHelper) fm.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);
            if (fragment != null) {
                if (fragment.mSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
                    fragment.mSearchTask.cancel(true);
                }
            }
            loadlist(new File(CURRENT_PATH).getPath(), true, OpenMode.UNKNOWN);
            results = false;
        }
    }

    public void reauthenticateSmb() {
        if (smbPath != null) {
            try {
                MAIN_ACTIVITY.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int i=-1;
                        if((i=DataUtils.containsServer(smbPath))!=-1){
                            MAIN_ACTIVITY.showSMBDialog(DataUtils.getServers().get(i)[0], smbPath, true);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void goBackItemClick() {
        if (openMode == OpenMode.CUSTOM) {
            loadlist(home, false, OpenMode.FILE);
            return;
        }
        File f = new File(CURRENT_PATH);
        if (!results) {
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode == OpenMode.SMB)
                    try {
                        if (!CURRENT_PATH.equals(smbPath)) {
                            String path = (new SmbFile(CURRENT_PATH).getParent());
                            loadlist((path), true, OpenMode.SMB);
                        } else loadlist(home, false, OpenMode.FILE);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                else if (CURRENT_PATH.equals("/"))
                    MAIN_ACTIVITY.exit();
                else if (utils.canGoBack(f)) {
                    loadlist(f.getParent(), true, openMode);
                } else MAIN_ACTIVITY.exit();
            }
        } else {
            loadlist(f.getPath(), true, openMode);
        }
    }

    public void updateList() {
        computeScroll();
        ic.cleanup();
        loadlist((CURRENT_PATH), true, openMode);
    }

    /**
     * Assigns sort modes
     * A value from 0 to 3 defines sort mode as name/last modified/size/type in ascending order
     * Values from 4 to 7 defines sort mode as name/last modified/size/type in descending order
     *
     * Final value of {@link #sortby} varies from 0 to 3
     */
    public void getSortModes() {
        int t = Integer.parseInt(sharedPref.getString("sortby", "0"));
        if (t <= 3) {
            sortby = t;
            asc = 1;
        } else if (t > 3) {
            asc = -1;
            sortby = t - 4;
        }

        dsort = Integer.parseInt(sharedPref.getString("dirontop", "0"));
    }

    @Override
    public void onResume() {
        super.onResume();
        (getActivity()).registerReceiver(receiver2, new IntentFilter("loadlist"));
        fixIcons(false);
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

    void fixIcons(boolean forceReload) {
        if (LIST_ELEMENTS == null) return;
        BitmapDrawable iconDrawable;
        for (LayoutElements layoutElements : LIST_ELEMENTS) {
            if (forceReload || layoutElements.getImageId() == null) {
                iconDrawable = layoutElements.isDirectory() ?
                        folder : Icons.loadMimeIcon(layoutElements.getDesc(), !IS_LIST, res);
                layoutElements.setImageId(iconDrawable);
            }
        }
    }

    public ArrayList<LayoutElements> addToSmb(SmbFile[] mFile, String path) throws SmbException {
        ArrayList<LayoutElements> a = new ArrayList<>();
        if (searchHelper.size() > 500) searchHelper.clear();
        for (int i = 0; i < mFile.length; i++) {
            if (DataUtils.hiddenfiles.contains(mFile[i].getPath()))
                continue;
            String name = mFile[i].getName();
            name = (mFile[i].isDirectory() && name.endsWith("/")) ? name.substring(0, name.length() - 1) : name;
            if (path.equals(smbPath)) {
                if (name.endsWith("$")) continue;
            }
            if (mFile[i].isDirectory()) {
                folder_count++;
                LayoutElements layoutElements = new LayoutElements(folder, name, mFile[i].getPath(),
                        "", "", "", 0, false, mFile[i].lastModified() + "", true);
                layoutElements.setMode(OpenMode.SMB);
                searchHelper.add(layoutElements.generateBaseFile());
                a.add(layoutElements);
            } else {
                file_count++;
                try {
                    LayoutElements layoutElements = new LayoutElements(
                            Icons.loadMimeIcon(mFile[i].getPath(), !IS_LIST, res), name,
                            mFile[i].getPath(), "", "", Formatter.formatFileSize(getContext(),
                            mFile[i].length()), mFile[i].length(), false,
                            mFile[i].lastModified() + "", false);
                    layoutElements.setMode(OpenMode.SMB);
                    searchHelper.add(layoutElements.generateBaseFile());
                    a.add(layoutElements);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }

    // method to add search result entry to the LIST_ELEMENT arrayList
    private void addTo(BaseFile mFile) {
        File f = new File(mFile.getPath());
        String size = "";
        if (!DataUtils.hiddenfiles.contains(mFile.getPath())) {
            if (mFile.isDirectory()) {
                size = "";
                LayoutElements layoutElements = utils.newElement(folder, f.getPath(), mFile.getPermission(), mFile.getLink(), size, 0, true, false, mFile.getDate() + "");
                layoutElements.setMode(mFile.getMode());
                LIST_ELEMENTS.add(layoutElements);
                folder_count++;
            } else {
                long longSize = 0;
                try {
                    if (mFile.getSize() != -1) {
                        longSize = mFile.getSize();
                        size = Formatter.formatFileSize(getContext(), longSize);
                    } else {
                        size = "";
                        longSize = 0;
                    }
                } catch (NumberFormatException e) {
                    //e.printStackTrace();
                }
                try {
                    LayoutElements layoutElements = utils.newElement(Icons.loadMimeIcon(f.getPath(), !IS_LIST, res), f.getPath(), mFile.getPermission(), mFile.getLink(), size, longSize, false, false, mFile.getDate() + "");
                    layoutElements.setMode(mFile.getMode());
                    LIST_ELEMENTS.add(layoutElements);
                    file_count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void hide(String path) {

        DataUtils.addHiddenFile(path);
        if (new File(path).isDirectory()) {
            File f1 = new File(path + "/" + ".nomedia");
            if (!f1.exists()) {
                try {
                    MAIN_ACTIVITY.mainActivityHelper.mkFile(new HFile(OpenMode.FILE, f1.getPath()), this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Futils.scanFile(path, getActivity());
        }

    }

    private void addShortcut(LayoutElements path) {
        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getActivity().getApplicationContext(),
                MainActivity.class);
        shortcutIntent.putExtra("path", path.getDesc());
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

    // adds search results based on result boolean. If false, the adapter is initialised with initial
    // values, if true, new values are added to the adapter.
    public void addSearchResult(BaseFile a) {
        if (listView != null) {

            // initially clearing the array for new result set
            if (!results) {
                LIST_ELEMENTS.clear();
                file_count = 0;
                folder_count = 0;
            }

            // adding new value to LIST_ELEMENTS
            addTo(a);
            if (!results) {
                createViews(LIST_ELEMENTS, false, (CURRENT_PATH), openMode, false, !IS_LIST);
                pathname.setText(MAIN_ACTIVITY.getString(R.string.empty));
                mFullPath.setText(MAIN_ACTIVITY.getString(R.string.searching));
                results = true;
            } else {
                adapter.addItem();
            }
            stopAnimation();
        }
    }

    public void onSearchCompleted() {
        if (!results) {
            // no results were found
            LIST_ELEMENTS.clear();
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Collections.sort(LIST_ELEMENTS, new FileListSorter(dsort, sortby, asc));
                return null;
            }

            @Override
            public void onPostExecute(Void c) {
                createViews(LIST_ELEMENTS, true, (CURRENT_PATH), openMode, true, !IS_LIST);
                pathname.setText(MAIN_ACTIVITY.getString(R.string.empty));
                mFullPath.setText(MAIN_ACTIVITY.getString(R.string.searchresults));
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void launch(final SmbFile smbFile, final long si, final Activity activity) {
        final Streamer s = Streamer.getInstance();
        new Thread() {
            public void run() {
                try {
                    /*List<SmbFile> subtitleFiles = new ArrayList<SmbFile>();

                    // finding subtitles
                    for (Layoutelements layoutelement : LIST_ELEMENTS) {
                        SmbFile smbFile = new SmbFile(layoutelement.getDesc());
                        if (smbFile.getName().contains(smbFile.getName())) subtitleFiles.add(smbFile);
                    }*/

                    s.setStreamSrc(smbFile, si);
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(smbFile.getPath()).getPath())).getEncodedPath());
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setDataAndType(uri, MimeTypes.getMimeType(new File(smbFile.getPath())));
                                PackageManager packageManager = activity.getPackageManager();
                                List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                                if (resInfos != null && resInfos.size() > 0)
                                    activity.startActivity(i);
                                else
                                    Toast.makeText(activity,
                                            activity.getResources().getString(R.string.smb_launch_error),
                                            Toast.LENGTH_SHORT).show();
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

    @Override
    public void onDetach() {
        super.onDetach();
    }
}