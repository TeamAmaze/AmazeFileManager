/* Diego Felipe Lassa <diegoflassa@gmail.com>
 *
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
import android.os.FileObserver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.text.format.Formatter;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.database.models.Tab;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.MediaStoreHack;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.ui.LayoutElement;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.icons.IconHolder;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.SmbStreamer.Streamer;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;
import com.amaze.filemanager.utils.files.FileListSorter;
import com.amaze.filemanager.utils.files.Futils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainFragment extends android.support.v4.app.Fragment {

    public ActionMode mActionMode;
    public BitmapDrawable folder, apk, DARK_IMAGE, DARK_VIDEO;
    public LinearLayout buttons;
    public int sortby, dsort, asc;
    public String home, goback;
    public boolean selection, results = false, SHOW_HIDDEN, CIRCULAR_IMAGES, SHOW_PERMISSIONS,
            SHOW_SIZE, SHOW_LAST_MODIFIED;
    public LinearLayout pathbar;
    public OpenMode openMode = OpenMode.FILE;

    public boolean GO_BACK_ITEM, SHOW_THUMBS, COLORISE_ICONS, SHOW_DIVIDERS, SHOW_HEADERS;

    /**
     * {@link MainFragment#IS_LIST} boolean to identify if the view is a list or grid
     */
    public boolean IS_LIST = true;
    public IconHolder ic;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    public int file_count, folder_count, columns;
    public String smbPath;
    public ArrayList<BaseFile> searchHelper = new ArrayList<>();
    public int no;

    private String CURRENT_PATH = "";
    /**
     * This is not an exact copy of the elements in the adapter
     */
    private ArrayList<LayoutElement> LIST_ELEMENTS;
    private RecyclerAdapter adapter;
    private SharedPreferences sharedPref;
    private Resources res;

    // ATTRIBUTES FOR APPEARANCE AND COLORS
    private int accentColor, primaryColor, primaryTwoColor;
    private LinearLayoutManager mLayoutManager;
    private GridLayoutManager mLayoutManagerGrid;
    private boolean addheader = false;
    private DividerItemDecoration dividerItemDecoration;
    private AppBarLayout mToolbarContainer;
    private TextView pathname, mFullPath;
    private boolean stopAnims = true;
    private View nofilesview;

    private android.support.v7.widget.RecyclerView listView;
    private UtilitiesProviderInterface utilsProvider;
    private Futils utils;
    private HashMap<String, Bundle> scrolls = new HashMap<>();
    private MainFragment ma = this;
    private View rootView;
    private View actionModeView;
    private FastScroller fastScroller;
    private Bitmap mFolderBitmap;
    private CustomFileObserver customFileObserver;
    private DataUtils dataUtils = DataUtils.getInstance();

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

        setRetainInstance(true);

        utilsProvider = getMainActivity();
        utils = utilsProvider.getFutils();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        res = getResources();

        no = getArguments().getInt("no", 1);
        home = getArguments().getString("home");
        CURRENT_PATH = getArguments().getString("lastpath");

        IS_LIST = !checkPathIsGrid(CURRENT_PATH);

        accentColor = getMainActivity().getColorPreference().getColor(ColorUsage.ACCENT);
        primaryColor = getMainActivity().getColorPreference().getColor(ColorUsage.PRIMARY);
        primaryTwoColor = getMainActivity().getColorPreference().getColor(ColorUsage.PRIMARY_TWO);

        SHOW_PERMISSIONS = sharedPref.getBoolean("showPermissions", false);
        SHOW_SIZE = sharedPref.getBoolean("showFileSize", false);
        SHOW_DIVIDERS = sharedPref.getBoolean("showDividers", true);
        SHOW_HEADERS = sharedPref.getBoolean("showHeaders", true);
        GO_BACK_ITEM = sharedPref.getBoolean("goBack_checkbox", false);
        CIRCULAR_IMAGES = sharedPref.getBoolean("circularimages", true);
        SHOW_LAST_MODIFIED = sharedPref.getBoolean("showLastModified", true);
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
        fastScroller.setPressedHandleColor(accentColor);
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
        pathname = (TextView) getActivity().findViewById(R.id.pathname);
        mFullPath = (TextView) getActivity().findViewById(R.id.fullpath);
        //String itemsstring = res.getString(R.string.items);// TODO: 23/5/2017 use or delete
        apk = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_doc_apk_grid));
        mToolbarContainer.setBackgroundColor(MainActivity.currentTab == 1 ? primaryTwoColor : primaryColor);

        if (!sharedPref.getBoolean("intelliHideToolbar", true)) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) getActivity()
                    .findViewById(R.id.action_bar).getLayoutParams();
            params.setScrollFlags(0);
            mToolbarContainer.setExpanded(true, true);
        }

        //   listView.setPadding(listView.getPaddingLeft(), paddingTop, listView.getPaddingRight(), listView.getPaddingBottom());
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(false);
        //getMainActivity() = (MainActivity) getActivity();
        initNoFileLayout();
        SHOW_HIDDEN = sharedPref.getBoolean("showHidden", false);
        COLORISE_ICONS = sharedPref.getBoolean("coloriseIcons", true);
        mFolderBitmap = BitmapFactory.decodeResource(res, R.drawable.ic_grid_folder_new);
        goback = getString(R.string.goback);
        folder = new BitmapDrawable(res, mFolderBitmap);
        getSortModes();
        DARK_IMAGE = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_doc_image_dark));
        DARK_VIDEO = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.ic_doc_video_dark));
        this.setRetainInstance(false);
        HFile f = new HFile(OpenMode.UNKNOWN, CURRENT_PATH);
        f.generateMode(getActivity());
        getMainActivity().initiatebbar();
        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT) && !IS_LIST) {
            listView.setBackgroundColor(Utils.getColor(getContext(), R.color.grid_background_light));
        } else {
            listView.setBackgroundDrawable(null);
        }

        listView.setHasFixedSize(true);
        columns = Integer.parseInt(sharedPref.getString("columns", "-1"));
        if (IS_LIST) {
            mLayoutManager = new LinearLayoutManager(getContext());
            listView.setLayoutManager(mLayoutManager);
        } else {
            if (columns == -1 || columns == 0)
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), 3);
            else
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
            listView.setLayoutManager(mLayoutManagerGrid);
        }
        // use a linear layout manager
        //View footerView = getActivity().getLayoutInflater().inflate(R.layout.divider, null);// TODO: 23/5/2017 use or delete
        dividerItemDecoration = new DividerItemDecoration(getActivity(), false, SHOW_DIVIDERS);
        listView.addItemDecoration(dividerItemDecoration);
        mSwipeRefreshLayout.setColorSchemeColors(accentColor);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        listView.setItemAnimator(animator);
        mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if ((columns == 0 || columns == -1)) {
                    int screen_width = listView.getWidth();
                    int dptopx = Utils.dpToPx(115, getContext());
                    columns = screen_width / dptopx;
                    if (columns == 0 || columns == -1) columns = 3;
                    if (!IS_LIST) mLayoutManagerGrid.setSpanCount(columns);
                }
                if (savedInstanceState != null && !IS_LIST)
                    onSavedInstanceState(savedInstanceState);
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
                onSavedInstanceState(savedInstanceState);
        }
    }

    void switchToGrid() {
        IS_LIST = false;

        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);
        folder = new BitmapDrawable(res, mFolderBitmap);
        fixIcons(true);

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

            // will always be grid, set alternate white background
            listView.setBackgroundColor(Utils.getColor(getContext(), R.color.grid_background_light));
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
        createViews(getLayoutElements(), false, CURRENT_PATH, openMode, results, checkPathIsGrid(CURRENT_PATH));
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
            outState.putParcelableArrayList("list", getLayoutElements());
            outState.putString("CURRENT_PATH", CURRENT_PATH);
            outState.putBoolean("selection", selection);
            outState.putInt("openMode", openMode.ordinal());
            outState.putInt("folder_count", folder_count);
            outState.putInt("file_count", file_count);

            if (selection) {
                ArrayList<String> selectedPaths = new ArrayList<>();
                for(LayoutElement e : adapter.getCheckedItems()) {
                    selectedPaths.add(e.getDesc());
                }
                outState.putStringArrayList("position", selectedPaths);
            }

            outState.putBoolean("results", results);

            if (openMode == OpenMode.SMB) {
                outState.putString("SmbPath", smbPath);
            }
        }
    }

    void onSavedInstanceState(final Bundle savedInstanceState) {
        Bundle b = new Bundle();
        String cur = savedInstanceState.getString("CURRENT_PATH");

        if (cur != null) {
            b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);

            openMode = OpenMode.getOpenMode(savedInstanceState.getInt("openMode", 0));
            if (openMode == OpenMode.SMB)
                smbPath = savedInstanceState.getString("SmbPath");
            putLayoutElements(savedInstanceState.<LayoutElement>getParcelableArrayList("list"));
            CURRENT_PATH = cur;
            folder_count = savedInstanceState.getInt("folder_count", 0);
            file_count = savedInstanceState.getInt("file_count", 0);
            results = savedInstanceState.getBoolean("results");
            getMainActivity().updatePath(CURRENT_PATH, results, openMode, folder_count, file_count);
            createViews(getLayoutElements(), true, (CURRENT_PATH), openMode, results, !IS_LIST);
            if (savedInstanceState.getBoolean("selection")) {
                for (String path : savedInstanceState.getStringArrayList("position")) {
                    adapter.toggleChecked(true, path);
                }
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

        void initMenu(Menu menu) {
            /*
            menu.findItem(R.id.cpy).setIcon(icons.getCopyDrawable());
            menu.findItem(R.id.cut).setIcon(icons.getCutDrawable());
            menu.findItem(R.id.delete).setIcon(icons.getDeleteDrawable());
            menu.findItem(R.id.all).setIcon(icons.getAllDrawable());
            */
        }

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            actionModeView = getActivity().getLayoutInflater().inflate(R.layout.actionmode, null);
            mode.setCustomView(actionModeView);

            getMainActivity().setPagingEnabled(false);
            getMainActivity().floatingActionButton.hideMenuButton(true);

            // translates the drawable content down
            // if (getMainActivity().isDrawerLocked) getMainActivity().translateDrawerList(true);

            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.openwith, menu);
            if (getMainActivity().mReturnIntent)
                showOption(R.id.openmulti, menu);
            //hideOption(R.id.setringtone,menu);
            mode.setTitle(getResources().getString(R.string.select));

            getMainActivity().updateViews(new ColorDrawable(res.getColor(R.color.holo_dark_action_mode)));

            // do not allow drawer to open when item gets selected
            if (!getMainActivity().isDrawerLocked) {

                getMainActivity().mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED,
                        getMainActivity().mDrawerLinear);
            }
            return true;
        }

        /**
         * the following method is called each time
         * the action mode is shown. Always called after
         * onCreateActionMode, but
         * may be called multiple times if the mode is invalidated.
         */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<LayoutElement> positions = adapter.getCheckedItems();
            TextView textView1 = (TextView) actionModeView.findViewById(R.id.item_count);
            textView1.setText(String.valueOf(positions.size()));
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
            if (getMainActivity().mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16)
                    showOption(R.id.openmulti, menu);
            //tv.setText(positions.size());
            if (!results) {
                hideOption(R.id.openparent, menu);
                if (positions.size() == 1) {
                    showOption(R.id.addshortcut, menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.share, menu);

                    File x = new File(adapter.getCheckedItems().get(0).getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith, menu);
                        hideOption(R.id.share, menu);
                        hideOption(R.id.openmulti, menu);
                    }

                    if (getMainActivity().mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);

                } else {
                    try {
                        showOption(R.id.share, menu);
                        if (getMainActivity().mReturnIntent)
                            if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
                        for (LayoutElement e : adapter.getCheckedItems()) {
                            File x = new File(e.getDesc());
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

                    File x = new File(adapter.getCheckedItems().get(0).getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith, menu);
                        hideOption(R.id.share, menu);
                        hideOption(R.id.openmulti, menu);
                    }
                    if (getMainActivity().mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);

                } else {
                    hideOption(R.id.openparent, menu);

                    if (getMainActivity().mReturnIntent)
                        if (Build.VERSION.SDK_INT >= 16)
                            showOption(R.id.openmulti, menu);
                    try {
                        for (LayoutElement e : adapter.getCheckedItems()) {
                            File x = new File(e.getDesc());
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
            ArrayList<LayoutElement> plist = adapter.getCheckedItems();
            switch (item.getItemId()) {
                case R.id.openmulti:
                    if (Build.VERSION.SDK_INT >= 16) {
                        Intent intentresult = new Intent();
                        ArrayList<Uri> resulturis = new ArrayList<>();
                        for (LayoutElement element : plist) {
                            try {
                                resulturis.add(Uri.fromFile(new File(element.getDesc())));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        final ClipData clipData = new ClipData(
                                null, new String[]{"*/*"}, new ClipData.Item(resulturis.get(0)));
                        for (int i = 1; i < resulturis.size(); i++) {
                            clipData.addItem(new ClipData.Item(resulturis.get(i)));
                        }
                        intentresult.setClipData(clipData);
                        mode.finish();
                        getActivity().setResult(FragmentActivity.RESULT_OK, intentresult);
                        getActivity().finish();
                    }
                    return true;
                case R.id.about:
                    LayoutElement x = plist.get(0);
                    GeneralDialogCreation.showPropertiesDialogWithPermissions((x).generateBaseFile(),
                            x.getPermissions(), (BaseActivity) getActivity(), BaseActivity.rootMode,
                            utilsProvider.getAppTheme());
                    /*
                    PropertiesSheet propertiesSheet = new PropertiesSheet();
                    Bundle arguments = new Bundle();
                    arguments.putParcelable(PropertiesSheet.KEY_FILE, x.generateBaseFile());
                    arguments.putString(PropertiesSheet.KEY_PERMISSION, x.getPermissions());
                    arguments.putBoolean(PropertiesSheet.KEY_ROOT, BaseActivity.rootMode);
                    propertiesSheet.setArguments(arguments);
                    propertiesSheet.show(getFragmentManager(), PropertiesSheet.TAG_FRAGMENT);
                    */
                    mode.finish();
                    return true;
                /*
                case R.id.setringtone:
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
                    return true;
                */
                case R.id.delete:
                    GeneralDialogCreation.deleteFilesDialog(getContext(), getLayoutElements(),
                            getMainActivity(), plist, utilsProvider.getAppTheme());
                    return true;
                case R.id.share:
                    ArrayList<File> arrayList = new ArrayList<>();
                    for (LayoutElement e: plist) {
                        arrayList.add(new File(e.getDesc()));
                    }
                    if (arrayList.size() > 100)
                        Toast.makeText(getActivity(), getResources().getString(R.string.share_limit),
                                Toast.LENGTH_SHORT).show();
                    else {

                        switch (getLayoutElement(0).getMode()) {
                            case DROPBOX:
                            case BOX:
                            case GDRIVE:
                            case ONEDRIVE:
                                utils.shareCloudFile(getLayoutElement(0).getDesc(),
                                        getLayoutElement(0).getMode(), getContext());
                                break;
                            default:
                                utils.shareFiles(arrayList, getActivity(), utilsProvider.getAppTheme(), accentColor);
                                break;
                        }
                    }
                    return true;
                case R.id.openparent:
                    loadlist(new File(plist.get(0).getDesc()).getParent(), false, OpenMode.FILE);
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
                    f = plist.get(0).generateBaseFile();
                    rename(f);
                    mode.finish();
                    return true;
                case R.id.hide:
                    for (int i1 = 0; i1 < plist.size(); i1++) {
                        hide(plist.get(i1).getDesc());
                    }
                    updateList();
                    mode.finish();
                    return true;
                case R.id.ex:
                    getMainActivity().mainActivityHelper.extractFile(new File(plist.get(0).getDesc()));
                    mode.finish();
                    return true;
                case R.id.cpy:
                    getMainActivity().MOVE_PATH = null;
                    ArrayList<BaseFile> copies = new ArrayList<>();
                    for (int i2 = 0; i2 < plist.size(); i2++) {
                        copies.add(plist.get(i2).generateBaseFile());
                    }
                    getMainActivity().COPY_PATH = copies;
                    getMainActivity().supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.cut:
                    getMainActivity().COPY_PATH = null;
                    ArrayList<BaseFile> copie = new ArrayList<>();
                    for (int i3 = 0; i3 < plist.size(); i3++) {
                        copie.add(plist.get(i3).generateBaseFile());
                    }
                    getMainActivity().MOVE_PATH = copie;
                    getMainActivity().supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.compress:
                    ArrayList<BaseFile> copies1 = new ArrayList<>();
                    for (int i4 = 0; i4 < plist.size(); i4++) {
                        copies1.add(plist.get(i4).generateBaseFile());
                    }
                    GeneralDialogCreation.showCompressDialog((MainActivity) getActivity(), copies1, CURRENT_PATH);
                    mode.finish();
                    return true;
                case R.id.openwith:
                    Futils.openunknown(new File(plist.get(0).getDesc()), getActivity(), true);
                    return true;
                case R.id.addshortcut:
                    addShortcut(plist.get(0));
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
            //if (getMainActivity().isDrawerLocked) getMainActivity().translateDrawerList(false);

            getMainActivity().floatingActionButton.showMenuButton(true);
            if (!results) adapter.toggleChecked(false, CURRENT_PATH);
            else adapter.toggleChecked(false);
            getMainActivity().setPagingEnabled(true);

            getMainActivity().updateViews(new ColorDrawable(MainActivity.currentTab == 1 ?
                    primaryTwoColor : primaryColor));

            if (!getMainActivity().isDrawerLocked) {
                getMainActivity().mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
                        getMainActivity().mDrawerLinear);
            }
        }
    };

    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // load the list on a load broadcast
            switch (openMode) {
                case ROOT:
                case FILE:
                    // local file system don't need an explicit load, we've set an observer to
                    // take actions on creation/moving/deletion/modification of file on current path
                    break;
                default:
                    updateList();
                    break;
            }
        }
    };

    public void home() {
        ma.loadlist((ma.home), false, OpenMode.FILE);
    }

    /**
     * method called when list item is clicked in the adapter
     *
     * @param position the position
     * @param e the list item
     * @param imageView the check {@link RoundedImageView} that is to be animated
     */
    public void onListItemClicked(int position, LayoutElement e, ImageView imageView) {
        if (results) {
            // check to initialize search results
            // if search task is been running, cancel it
            MainActivityHelper.cancelSearch(getMainActivity());

            mRetainSearchTask = true;
            results = false;
        } else {
            mRetainSearchTask = false;
            MainActivityHelper.SEARCH_TEXT = null;
        }

        if (selection) {
            if (!e.getSize().equals(goback)) {
                // the first {goback} item if back navigation is enabled
                adapter.toggleChecked(position, imageView);
            } else {
                selection = false;
                if (mActionMode != null)
                    mActionMode.finish();
                mActionMode = null;
            }
        } else {
            if (!e.getSize().equals(goback)) {
                // hiding search view if visible
                if (MainActivity.isSearchViewEnabled) getMainActivity().hideSearchView();

                String path;
                if (!e.hasSymlink()) {

                    path = e.getDesc();
                } else {

                    path = e.getSymlink();
                }

                // check if we're trying to click on encrypted file
                if (!e.isDirectory() &&
                        e.getDesc().endsWith(CryptUtil.CRYPT_EXTENSION)) {
                    // decrypt the file
                    getMainActivity().isEncryptOpen = true;

                    getMainActivity().encryptBaseFile = new BaseFile(getActivity().getExternalCacheDir().getPath()
                            + "/"
                            + e.generateBaseFile().getName().replace(CryptUtil.CRYPT_EXTENSION, ""));

                    EncryptDecryptUtils.decryptFile(getContext(), getMainActivity(), ma, openMode,
                            e.generateBaseFile(), getActivity().getExternalCacheDir().getPath(),
                            utilsProvider);
                    return;
                }

                if (e.isDirectory()) {

                    computeScroll();
                    loadlist(path, false, openMode);
                } else {

                    if (getMainActivity().mReturnIntent) {
                        returnIntentResults(e.generateBaseFile());
                        return;
                    }

                    switch (e.getMode()) {
                        case SMB:
                            try {
                                SmbFile smbFile = new SmbFile(e.getDesc());
                                launchSMB(smbFile, e.getlongSize(), getMainActivity());
                            } catch (MalformedURLException ex) {
                                ex.printStackTrace();
                            }
                            break;
                        case OTG:
                            utils.openFile(OTGUtil.getDocumentFile(e.getDesc(), getContext(), false),
                                    (MainActivity) getActivity());
                            break;
                        case DROPBOX:
                        case BOX:
                        case GDRIVE:
                        case ONEDRIVE:
                            Toast.makeText(getContext(), getResources().getString(R.string.please_wait), Toast.LENGTH_LONG).show();
                            CloudUtil.launchCloud(e.generateBaseFile(), openMode, getMainActivity());
                            break;
                        default:
                            utils.openFile(new File(e.getDesc()), (MainActivity) getActivity());
                            break;
                    }

                    dataUtils.addHistoryFile(e.getDesc());
                }
            } else {
                goBackItemClick();
            }
        }
    }

    /**
     * Queries database to find entry for the specific path
     *
     * @param path the path to match with
     * @return the entry
     */
    private static EncryptedEntry findEncryptedEntry(Context context, String path) throws Exception {

        CryptHandler handler = new CryptHandler(context);

        EncryptedEntry matchedEntry = null;
        // find closest path which matches with database entry
        for (EncryptedEntry encryptedEntry : handler.getAllEntries()) {
            if (path.contains(encryptedEntry.getPath())) {

                if (matchedEntry == null || matchedEntry.getPath().length() < encryptedEntry.getPath().length()) {
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

    public void returnIntentResults(BaseFile baseFile) {

        getMainActivity().mReturnIntent = false;

        Intent intent = new Intent();
        if (getMainActivity().mRingtonePickerIntent) {

            Uri mediaStoreUri = MediaStoreHack.getUriFromFile(baseFile.getPath(), getActivity());
            System.out.println(mediaStoreUri.toString() + "\t" + MimeTypes.getMimeType(new File(baseFile.getPath())));
            intent.setDataAndType(mediaStoreUri, MimeTypes.getMimeType(new File(baseFile.getPath())));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, mediaStoreUri);
            getActivity().setResult(FragmentActivity.RESULT_OK, intent);
            getActivity().finish();
        } else {

            Log.d("pickup", "file");

            switch (baseFile.getMode()) {
                case FILE:
                case ROOT:
                    intent.setData(Uri.fromFile(new File(baseFile.getPath())));
                    break;
                case OTG:
                    intent.setData(OTGUtil.getDocumentFile(baseFile.getPath(), getContext(), true).getUri());
                    break;
                case SMB:
                case DROPBOX:
                case GDRIVE:
                case ONEDRIVE:
                case BOX:
                    Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.smb_launch_error),
                            Toast.LENGTH_LONG).show();

                    getMainActivity().mReturnIntent = true;
                    return;
            }

            getActivity().setResult(FragmentActivity.RESULT_OK, intent);
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
            nofilesview.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
            ((TextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
        }
    }

    /**
     * Probably checks if path is supposed to be shown as a list or as a grid of files.
     * @param path path to check
     * @return should be shown as grid
     */
    public boolean checkPathIsGrid(String path) {
        boolean grid = false, both_contain = false;
        int i1 = -1, i2 = -1;
        for (String s : dataUtils.getGridFiles()) {
            i1++;
            if ((path).contains(s)) {
                grid = true;
                break;
            }
        }
        for (String s : dataUtils.getListfiles()) {
            i2++;
            if (path.contains(s)) {
                if (grid) both_contain = true;
                grid = false;
                break;
            }
        }
        
        if (!both_contain) return grid;
        String path1 = dataUtils.getGridFiles().get(i1), path2 = dataUtils.getListfiles().get(i2);

        if (path1.contains(path2))
            return true;
        else if (path2.contains(path1))
            return false;
        else
            return grid;
    }

    /**
     * Loading adapter after getting a list of elements
     *
     * @param bitmap   the list of objects for the adapter
     * @param back     if we're coming back from any directory and want the scroll to be restored
     * @param path     the path for the adapter
     * @param openMode the type of file being created
     * @param results  is the list of elements a result from search
     * @param grid     whether to set grid view or list view
     */
    public void createViews(ArrayList<LayoutElement> bitmap, boolean back, String path,
                            final OpenMode openMode, boolean results, boolean grid) {
        if (bitmap != null && isAdded()) {
            synchronized (bitmap) {
                boolean isOtg = path.equals(OTGUtil.PREFIX_OTG + "/"),
                            isOnTheCloud = path.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")
                                    || path.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")
                                    || path.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")
                                    || path.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/");

                if (GO_BACK_ITEM && !path.equals("/") && (openMode == OpenMode.FILE || openMode == OpenMode.ROOT)
                        && !isOtg && !isOnTheCloud && (bitmap.size() == 0 || !bitmap.get(0).getSize().equals(goback))) {
                    Bitmap iconBitmap = BitmapFactory.decodeResource(res, R.drawable.ic_arrow_left_white_24dp);
                    bitmap.add(0, new LayoutElement(new BitmapDrawable(res, iconBitmap),
                            "..", "", "", goback, 0, false, true, ""));
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
                putLayoutElements(bitmap);
                if (grid && IS_LIST)
                    switchToGrid();
                else if (!grid && !IS_LIST) switchToList();
                if (adapter == null) {
                    adapter = new RecyclerAdapter(ma, utilsProvider, bitmap, ma.getActivity(), SHOW_HEADERS);
                } else {
                    adapter.setItems(getLayoutElements());
                }
                stopAnims = true;
                this.openMode = openMode;
                if (openMode != OpenMode.CUSTOM)
                    dataUtils.addHistoryFile(path);
                //mSwipeRefreshLayout.setRefreshing(false);

                listView.setAdapter(adapter);
                if (!addheader) {
                    //listView.removeItemDecoration(headersDecor);
                    listView.removeItemDecoration(dividerItemDecoration);
                    addheader = true;
                }
                if (addheader && IS_LIST) {
                    dividerItemDecoration = new DividerItemDecoration(getActivity(), true, SHOW_DIVIDERS);
                    listView.addItemDecoration(dividerItemDecoration);
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
                getMainActivity().updatePaths(no);
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

                if (buttons.getVisibility() == View.VISIBLE) getMainActivity().bbar(this);

                startFileObserver();
                //getMainActivity().invalidateFab(openMode);
            }
        } else {
            // list loading cancelled
            // TODO: Add support for cancelling list loading
            loadlist(home, true, OpenMode.FILE);
        }
    }

    private void startFileObserver() {

        AppConfig.runInBackground(new Runnable() {
            @Override
            public void run() {
                switch (openMode) {
                    case ROOT:
                    case FILE:
                        // watch the current directory
                        File file = new File(CURRENT_PATH);

                        if (file.isDirectory() && file.canRead()) {

                            if (customFileObserver != null) {
                                // already a watcher instantiated, first it should be stopped
                                customFileObserver.stopWatching();
                            }

                            customFileObserver = new CustomFileObserver(CURRENT_PATH);
                            customFileObserver.startWatching();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * Show dialog to rename a file
     *
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

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                dialog.cancel();
            }
        });

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                String name = dialog.getInputEditText().getText().toString();
                if (f.isSmb()){
                    if (f.isDirectory() && !name.endsWith("/"))
                        name = name + "/";
                }
                getMainActivity().mainActivityHelper.rename(openMode, f.getPath(),
                        CURRENT_PATH + "/" + name, getActivity(), BaseActivity.rootMode);
            }
        });

        builder.positiveText(R.string.save);
        builder.negativeText(R.string.cancel);
        builder.positiveColor(accentColor).negativeColor(accentColor).widgetColor(accentColor);
        final MaterialDialog materialDialog = builder.build();
        materialDialog.show();
        Log.d(getClass().getSimpleName(), f.getNameString(getContext()));

        // place cursor at the starting of edit text by posting a runnable to edit text
        // this is done because in case android has not populated the edit text layouts yet, it'll
        // reset calls to selection if not posted in message queue
        materialDialog.getInputEditText().post(new Runnable() {
            @Override
            public void run() {

                if (!f.isDirectory()) {

                    materialDialog.getInputEditText().setSelection(f.getNameString(getContext()).length());
                }
            }
        });
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

        HFile currentFile = new HFile(openMode, CURRENT_PATH);
        if (!results) {
            if (!mRetainSearchTask) {
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
                            CURRENT_PATH.equals(OTGUtil.PREFIX_OTG + "/")
                            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")
                            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")
                            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")
                            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")
                            )
                        getMainActivity().exit();
                    else if (utils.canGoBack(getContext(), currentFile)) {
                        loadlist(currentFile.getParent(getContext()), true, openMode);
                    } else getMainActivity().exit();
                }
            } else {
                // case when we had pressed on an item from search results and wanna go back
                // leads to resuming the search task

                if (MainActivityHelper.SEARCH_TEXT != null) {

                    // starting the search query again :O
                    getMainActivity().mainFragment = (MainFragment) getMainActivity().getFragment().getTab();

                    // getting parent path to resume search from there
                    String parentPath = new HFile(openMode, CURRENT_PATH).getParent(getActivity());
                    // don't fuckin' remove this line, we need to change
                    // the path back to parent on back press
                    CURRENT_PATH = parentPath;

                    MainActivityHelper.createSearch(getMainActivity(), MainActivityHelper.SEARCH_TEXT,
                            parentPath, openMode, BaseActivity.rootMode,
                            sharedPref.getBoolean(MainActivityHelper.KEY_REGEX, false),
                            sharedPref.getBoolean(MainActivityHelper.KEY_REGEX_MATCHES, false));
                } else loadlist(CURRENT_PATH, true, OpenMode.UNKNOWN);

                mRetainSearchTask = false;
            }
        } else {
            // to go back after search list have been popped
            MainActivityHelper.cancelSearch(getMainActivity());
            loadlist(new File(CURRENT_PATH).getPath(), true, OpenMode.UNKNOWN);
            results = false;
        }
    }

    public void reauthenticateSmb() {
        if (smbPath != null) {
            try {
                getMainActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int i;
                        if ((i = dataUtils.containsServer(smbPath)) != -1) {
                            getMainActivity().showSMBDialog(dataUtils.getServers().get(i)[0], smbPath, true);
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
        HFile currentFile = new HFile(openMode, CURRENT_PATH);
        if (!results) {
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode == OpenMode.SMB) {

                    try {
                        if (!CURRENT_PATH.equals(smbPath)) {
                            String path = (new SmbFile(CURRENT_PATH).getParent());
                            loadlist((path), true, OpenMode.SMB);
                        } else loadlist(home, false, OpenMode.FILE);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else if (CURRENT_PATH.equals("/") || CURRENT_PATH.equals(home) ||
                        CURRENT_PATH.equals(OTGUtil.PREFIX_OTG)
                        || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")
                        || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")
                        || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")
                        || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")
                        )
                    getMainActivity().exit();
                else if (utils.canGoBack(getContext(), currentFile)) {
                    loadlist(currentFile.getParent(getContext()), true, openMode);
                } else getMainActivity().exit();
            }
        } else {
            loadlist(currentFile.getPath(), true, openMode);
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
     * <p>
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

        //startFileObserver();
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

        if (customFileObserver != null)
            customFileObserver.stopWatching();
    }

    void fixIcons(boolean forceReload) {
        if (getLayoutElements() == null) return;
        BitmapDrawable iconDrawable;

        synchronized (getLayoutElements()) {
            for (LayoutElement layoutElement : getLayoutElements()) {
                if (forceReload || layoutElement.getImageId() == null) {
                    iconDrawable = layoutElement.isDirectory() ?
                            folder : Icons.loadMimeIcon(layoutElement.getDesc(), !IS_LIST, res);
                    layoutElement.setImageId(iconDrawable);
                }
            }
        }
    }

    public ArrayList<LayoutElement> addToSmb(SmbFile[] mFile, String path) throws SmbException {
        ArrayList<LayoutElement> a = new ArrayList<>();
        if (searchHelper.size() > 500) searchHelper.clear();
        for (SmbFile aMFile : mFile) {
            if (dataUtils.getHiddenfiles().contains(aMFile.getPath()))
                continue;
            String name = aMFile.getName();
            name = (aMFile.isDirectory() && name.endsWith("/")) ? name.substring(0, name.length() - 1) : name;
            if (path.equals(smbPath)) {
                if (name.endsWith("$")) continue;
            }
            if (aMFile.isDirectory()) {
                folder_count++;
                LayoutElement layoutElement = new LayoutElement(folder, name, aMFile.getPath(),
                        "", "", "", 0, false, aMFile.lastModified() + "", true);
                layoutElement.setMode(OpenMode.SMB);
                searchHelper.add(layoutElement.generateBaseFile());
                a.add(layoutElement);
            } else {
                file_count++;
                try {
                    LayoutElement layoutElement = new LayoutElement(
                            Icons.loadMimeIcon(aMFile.getPath(), !IS_LIST, res), name,
                            aMFile.getPath(), "", "", Formatter.formatFileSize(getContext(),
                            aMFile.length()), aMFile.length(), false,
                            aMFile.lastModified() + "", false);
                    layoutElement.setMode(OpenMode.SMB);
                    searchHelper.add(layoutElement.generateBaseFile());
                    a.add(layoutElement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }

    // method to add search result entry to the LIST_ELEMENT arrayList
    private LayoutElement addTo(BaseFile mFile) {
        File f = new File(mFile.getPath());
        String size = "";
        if (!dataUtils.getHiddenfiles().contains(mFile.getPath())) {
            if (mFile.isDirectory()) {
                size = "";
                LayoutElement layoutElement = new LayoutElement(folder, f.getPath(), mFile.getPermission(),
                        mFile.getLink(), size, 0, true, false, mFile.getDate() + "");

                layoutElement.setMode(mFile.getMode());
                addLayoutElement(layoutElement);
                folder_count++;
                return layoutElement;
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
                    LayoutElement layoutElement = new LayoutElement(Icons.loadMimeIcon(f.getPath(), !IS_LIST, res),
                            f.getPath(), mFile.getPermission(), mFile.getLink(), size, longSize, false, false, mFile.getDate() + "");
                    layoutElement.setMode(mFile.getMode());
                    addLayoutElement(layoutElement);
                    file_count++;
                    return layoutElement;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void hide(String path) {

        dataUtils.addHiddenFile(path);
        if (new File(path).isDirectory()) {
            File f1 = new File(path + "/" + ".nomedia");
            if (!f1.exists()) {
                try {
                    getMainActivity().mainActivityHelper.mkFile(new HFile(OpenMode.FILE, f1.getPath()), this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Futils.scanFile(path, getActivity());
        }

    }

    public String getCurrentPath() {
        return CURRENT_PATH;
    }

    private void addShortcut(LayoutElement path) {
        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getActivity().getApplicationContext(),
                MainActivity.class);
        shortcutIntent.putExtra("path", path.getDesc());
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, new File(path.getDesc()).getName());
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getActivity(), R.mipmap.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getActivity().sendBroadcast(addIntent);
    }

    // This method is used to implement the modification for the pre Searching
    public void onSearchPreExecute(String query) {
        pathname.setText(getMainActivity().getString(R.string.empty));
        mFullPath.setText(getMainActivity().getString(R.string.searching) + " " + query);
    }


    // adds search results based on result boolean. If false, the adapter is initialised with initial
    // values, if true, new values are added to the adapter.
    public void addSearchResult(BaseFile a, String query) {
        if (listView != null) {

            // initially clearing the array for new result set
            if (!results) {
                getLayoutElements().clear();
                file_count = 0;
                folder_count = 0;
            }

            // adding new value to LIST_ELEMENTS
            LayoutElement layoutElementAdded = addTo(a);
            if (!results) {
                createViews(getLayoutElements(), false, (CURRENT_PATH), openMode, false, !IS_LIST);
                pathname.setText(getMainActivity().getString(R.string.empty));
                mFullPath.setText(getMainActivity().getString(R.string.searching) + " " + query);
                results = true;
            } else {
                adapter.addItem(layoutElementAdded);
            }
            stopAnimation();
        }
    }

    public void onSearchCompleted(final String query) {
        if (!results) {
            // no results were found
            getLayoutElements().clear();
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Collections.sort(getLayoutElements(), new FileListSorter(dsort, sortby, asc));
                return null;
            }

            @Override
            public void onPostExecute(Void c) {
                createViews(getLayoutElements(), true, (CURRENT_PATH), openMode, true, !IS_LIST);// TODO: 7/7/2017 this is really inneffient, use RecycleAdapter's createHeaders()
                pathname.setText(getMainActivity().getString(R.string.empty));
                mFullPath.setText(getMainActivity().getString(R.string.searchresults) + " " + query);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void launchSMB(final SmbFile smbFile, final long si, final Activity activity) {
        final Streamer s = Streamer.getInstance();
        new Thread() {
            public void run() {
                try {
                    /*
                    List<SmbFile> subtitleFiles = new ArrayList<SmbFile>();

                    // finding subtitles
                    for (Layoutelements layoutelement : LIST_ELEMENTS) {
                        SmbFile smbFile = new SmbFile(layoutelement.getDesc());
                        if (smbFile.getName().contains(smbFile.getName())) subtitleFiles.add(smbFile);
                    }
                    */

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

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    public synchronized void addLayoutElement(LayoutElement layoutElement) {
        this.LIST_ELEMENTS.add(layoutElement);
    }

    public synchronized LayoutElement getLayoutElement(int index) {
        return this.LIST_ELEMENTS.get(index);
    }

    public synchronized void putLayoutElements(ArrayList<LayoutElement> layoutElements) {
        this.LIST_ELEMENTS = layoutElements;
    }

    public synchronized ArrayList<LayoutElement> getLayoutElements() {
        return this.LIST_ELEMENTS;
    }

    public synchronized int getLayoutElementSize() {
        return this.LIST_ELEMENTS.size();
    }

    public synchronized void removeLayoutElement(int index) {
        this.LIST_ELEMENTS.remove(index);
    }

    /**
     * Inner class which monitors any change in local filesystem and updates the adapter
     * Makes use of inotify in Linux
     */
    private class CustomFileObserver extends FileObserver {

        CustomFileObserver(String path) {
            super(path);
        }

        @Override
        public void onEvent(int event, String path) {

            synchronized (getLayoutElements()) {

                switch (event) {
                    case CREATE:
                    case MOVED_TO:
                        /*HFile fileCreated = new HFile(openMode, CURRENT_PATH + "/" + path);
                        addLayoutElement(fileCreated.generateLayoutElement(MainFragment.this, utilsProvider));
                        Log.d(getClass().getSimpleName(), "ADDED: " + CURRENT_PATH + "/" + path);*/
                        break;
                    case DELETE:
                    case MOVED_FROM:
                        /*for (int i = 0; i < getLayoutElementSize(); i++) {
                            File currentFile = new File(getLayoutElement(i).getDesc());
                            if (currentFile.getName().equals(path)) {
                                removeLayoutElement(i);
                                break;
                            }
                        }
                        Log.d(getClass().getSimpleName(), "REMOVED: " + CURRENT_PATH + "/" + path);*/
                        break;
                    case DELETE_SELF:
                    case MOVE_SELF:
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                goBack();
                            }
                        });
                        return;
                    case ATTRIB:
                    case MODIFY:
                        // just generate adapter list without making any change to it's content
                        break;
                    default:
                        return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        /*if (listView.getVisibility() == View.VISIBLE) {
                            if (getLayoutElements().size() == 0) {

                                // no item left in list, recreate views
                                createViews(getLayoutElements(), true, CURRENT_PATH, openMode, results, !IS_LIST);
                            } else {

                                // we already have some elements in list view, invalidate the adapter
                                adapter.setItems(getLayoutElements());
                            }
                        } else {
                            // there was no list view, means the directory was empty
                            loadlist(CURRENT_PATH, true, openMode);
                        }*/

                        computeScroll();
                        loadlist(CURRENT_PATH, true, openMode);
                    }
                });
            }
        }

    }
}
