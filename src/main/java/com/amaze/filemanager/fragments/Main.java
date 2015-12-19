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


import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.IMyAidlInterface;
import com.amaze.filemanager.Loadlistener;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.Recycleradapter;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.services.asynctasks.SearchTask;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.ui.icons.IconHolder;
import com.amaze.filemanager.ui.icons.IconUtils;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HFile;
import com.amaze.filemanager.utils.HidingScrollListener;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.SmbStreamer.Streamer;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.io.File;
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

    public ArrayList<Layoutelements> LIST_ELEMENTS;
    public Recycleradapter adapter;
    public Futils utils;
    public ActionMode mActionMode;
    public SharedPreferences Sp;
    public Drawable folder, apk, DARK_IMAGE, DARK_VIDEO;
    public LinearLayout buttons;
    public int sortby, dsort, asc;
    public String home, CURRENT_PATH = "",year, goback;
    public boolean selection,results = false,ROOT_MODE, SHOW_HIDDEN, CIRCULAR_IMAGES, SHOW_PERMISSIONS, SHOW_SIZE, SHOW_LAST_MODIFIED;
    public LinearLayout pathbar;
    public int openMode = 0;
    public android.support.v7.widget.RecyclerView listView;
    public boolean GO_BACK_ITEM, IS_LIST=true, SHOW_THUMBS, COLORISE_ICONS, SHOW_DIVIDERS, TOP_FAB;
    public IconHolder ic;
    public MainActivity MAIN_ACTIVITY;
    public String skin, fabSkin, iconskin;
    public float[] color;
    public ColorMatrixColorFilter colorMatrixColorFilter;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    public int paddingTop;
    public int skin_color, icon_skin_color, theme1, theme, file_count, folder_count, columns;
    public String smbPath;
    public ArrayList<String> searchHelper = new ArrayList<String>();
    public SearchTask searchTask;
    public int skinselection;
    Resources res;
    HashMap<String, Bundle> scrolls = new HashMap<String, Bundle>();
    Main ma = this;
    IconUtils icons;
    View footerView;
    String itemsstring;
    public int no;
    TabHandler tabHandler;
    LinearLayoutManager mLayoutManager;
    GridLayoutManager mLayoutManagerGrid;
    boolean addheader = false;
    StickyRecyclerHeadersDecoration headersDecor;
    DividerItemDecoration dividerItemDecoration;
    int mToolbarHeight, hidemode;
    View mToolbarContainer;
    TextView pathname;
    boolean stopAnims = true,PERM_GRID;
    View nofilesview;
    String current_drive_id;
    DisplayMetrics displayMetrics;
    HFile f;
    Streamer s;
    private View rootView;
    private View actionModeView;
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
            if (MAIN_ACTIVITY.isDrawerLocked) MAIN_ACTIVITY.translateDrawerList(true);
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.openwith, menu);
            if (MAIN_ACTIVITY.mReturnIntent)
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
                if (MAIN_ACTIVITY.colourednavigation)
                    window.setNavigationBarColor(res.getColor(android.R.color.black));
            }

            if (!MAIN_ACTIVITY.isDrawerLocked)
                MAIN_ACTIVITY.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                        MAIN_ACTIVITY.mDrawerLinear);
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
            if (openMode == 1) {
                hideOption(R.id.addshortcut, menu);
                hideOption(R.id.openwith, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.hide, menu);
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
                        ArrayList<Uri> resulturis = new ArrayList<Uri>();
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
                    Layoutelements x;
                    x = LIST_ELEMENTS.get((plist.get(0)));
                    utils.showProps((x).getDesc(), x.getPermissions(), ma, ROOT_MODE);
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
                    utils.deleteFiles(LIST_ELEMENTS, ma, plist);


                    return true;
                case R.id.share:
                    ArrayList<File> arrayList = new ArrayList<File>();
                    for (int i : plist) {
                        arrayList.add(new File(LIST_ELEMENTS.get(i).getDesc()));
                    }
                    if (arrayList.size() > 100)
                        Toast.makeText(getActivity(), "Can't share more than 100 files", Toast.LENGTH_SHORT).show();
                    else
                        utils.shareFiles(arrayList, getActivity(), theme1, Color.parseColor
                                (fabSkin));
                    return true;
                case R.id.openparent:
                    loadlist(new File(LIST_ELEMENTS.get(plist.get(0)).getDesc()).getParent(), false, 0);
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
                    final String f;
                    f = (LIST_ELEMENTS.get(
                            (plist.get(0))).getDesc());
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
                    ArrayList<String> copies = new ArrayList<String>();
                    for (int i2 = 0; i2 < plist.size(); i2++) {
                        copies.add(LIST_ELEMENTS.get(plist.get(i2)).getDesc());
                    }
                    MAIN_ACTIVITY.COPY_PATH = copies;
                    MAIN_ACTIVITY.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.cut:
                    MAIN_ACTIVITY.COPY_PATH = null;
                    ArrayList<String> copie = new ArrayList<String>();
                    for (int i3 = 0; i3 < plist.size(); i3++) {
                        copie.add(LIST_ELEMENTS.get(plist.get(i3)).getDesc());
                    }
                    MAIN_ACTIVITY.MOVE_PATH = copie;
                    MAIN_ACTIVITY.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.compress:
                    ArrayList<String> copies1 = new ArrayList<String>();
                    for (int i4 = 0; i4 < plist.size(); i4++) {
                        copies1.add(LIST_ELEMENTS.get(plist.get(i4)).getDesc());
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
            if (MAIN_ACTIVITY.isDrawerLocked) MAIN_ACTIVITY.translateDrawerList(false);

            MAIN_ACTIVITY.floatingActionButton.showMenuButton(true);
            if (!results) adapter.toggleChecked(false, CURRENT_PATH);
            else adapter.toggleChecked(false);
            MAIN_ACTIVITY.setPagingEnabled(true);
            ObjectAnimator anim = ObjectAnimator.ofInt(getActivity().findViewById(R.id.buttonbarframe), "backgroundColor", res.getColor(R.color.holo_dark_action_mode), skin_color);
            anim.setDuration(0);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if (MAIN_ACTIVITY.colourednavigation)
                    window.setNavigationBarColor(MAIN_ACTIVITY.skinStatusBar);
            }

            if (!MAIN_ACTIVITY.isDrawerLocked)
                MAIN_ACTIVITY.mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
                        MAIN_ACTIVITY.mDrawerLinear);
        }
    };
    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MAIN_ACTIVITY = (MainActivity) getActivity();
        no = getArguments().getInt("no", 1);
        home = getArguments().getString("home");
        CURRENT_PATH = getArguments().getString("lastpath");
        tabHandler = new TabHandler(getActivity(), null, null, 1);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        skin = PreferenceUtils.getSkinColor(Sp.getInt("skin_color_position", 4));
        fabSkin = PreferenceUtils.getFabColor(Sp.getInt("fab_skin_color_position", 1));
        int icon=Sp.getInt("icon_skin_color_position", -1);
        icon=icon==-1?Sp.getInt("skin_color_position", 4):icon;
        iconskin = PreferenceUtils.getSkinColor(icon);
        skin_color = Color.parseColor(skin);
        icon_skin_color = Color.parseColor(iconskin);
        PERM_GRID = Sp.getBoolean("perm_grid", false);
        Calendar calendar = Calendar.getInstance();
        year = ("" + calendar.get(Calendar.YEAR)).substring(2, 4);
        theme = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme == 2 ? PreferenceUtils.hourOfDay() : theme;
        hidemode = Sp.getInt("hidemode", 0);
        TOP_FAB = hidemode == 0 ? Sp.getBoolean("topFab", true) : false;
        SHOW_PERMISSIONS = Sp.getBoolean("showPermissions", false);
        SHOW_SIZE = Sp.getBoolean("showFileSize", false);
        SHOW_DIVIDERS = Sp.getBoolean("showDividers", true);
        GO_BACK_ITEM = Sp.getBoolean("goBack_checkbox", false);
        CIRCULAR_IMAGES = Sp.getBoolean("circularimages", true);
        SHOW_LAST_MODIFIED = Sp.getBoolean("showLastModified", true);
        icons = new IconUtils(Sp, getActivity());
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
        listView = (android.support.v7.widget.RecyclerView) rootView.findViewById(R.id.listView);
        mToolbarContainer = getActivity().findViewById(R.id.lin);
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
        buttons = (LinearLayout) getActivity().findViewById(R.id.buttons);
        pathbar = (LinearLayout) getActivity().findViewById(R.id.pathbar);
        SHOW_THUMBS = Sp.getBoolean("showThumbs", true);
        res = getResources();
        pathname = (TextView) getActivity().findViewById(R.id.pathname);
        goback = res.getString(R.string.goback);
        itemsstring = res.getString(R.string.items);
        apk = res.getDrawable(R.drawable.ic_doc_apk_grid);
        mToolbarContainer.setBackgroundColor(skin_color);
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
        MAIN_ACTIVITY = (MainActivity) getActivity();
        initNoFileLayout();
        utils = new Futils();
        String x = getSelectionColor();
        skinselection = Color.parseColor(x);
        color = calculatevalues(x);
        ColorMatrix colorMatrix = new ColorMatrix(calculatefilter(color));
        colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        ROOT_MODE = Sp.getBoolean("rootmode", false);
        SHOW_HIDDEN = Sp.getBoolean("showHidden", false);
        COLORISE_ICONS = Sp.getBoolean("coloriseIcons", true);
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);
        getSortModes();
        DARK_IMAGE = res.getDrawable(R.drawable.ic_doc_image_dark);
        DARK_VIDEO = res.getDrawable(R.drawable.ic_doc_video_dark);
        this.setRetainInstance(false);
        f = new HFile(CURRENT_PATH);
        if (!f.isCustomPath() && !f.isSmb() && !f.isDirectory()) {
            File file = new File(CURRENT_PATH);
            utils.openFile(file, MAIN_ACTIVITY);
            CURRENT_PATH = (file.getParent());
        }
        MAIN_ACTIVITY.initiatebbar();
        IS_LIST=savedInstanceState!=null?savedInstanceState.getBoolean("IS_LIST",IS_LIST):IS_LIST;
        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);
        if (theme1 == 1) {

            listView.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_dark_background)));
        } else {

            if (IS_LIST)
                listView.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.background_light)));

        }
        listView.setHasFixedSize(true);
        columns = Integer.parseInt(Sp.getString("columns", "-1"));
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
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadlist((CURRENT_PATH), false, openMode);
            }
        });
        mToolbarHeight = getToolbarHeight(getActivity());
        paddingTop = (mToolbarHeight) + dpToPx(72);
        if (hidemode == 2) mToolbarHeight = paddingTop;
        mSwipeRefreshLayout.setProgressViewOffset(true, paddingTop, paddingTop + dpToPx(30));
        mSwipeRefreshLayout.setColorSchemeColors(Color.parseColor(fabSkin));
        DefaultItemAnimator animator = new DefaultItemAnimator();
        listView.setItemAnimator(animator);
        mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (hidemode != 2) mToolbarHeight = MAIN_ACTIVITY.toolbar.getHeight();
                else mToolbarHeight = paddingTop;
                paddingTop = mToolbarContainer.getHeight();
                if ((columns == 0 || columns == -1)) {
                    int screen_width = listView.getWidth();
                    int dptopx = dpToPx(115);
                    columns = screen_width / dptopx;
                    if (!IS_LIST) mLayoutManagerGrid.setSpanCount(columns);
                }
                mSwipeRefreshLayout.setProgressViewOffset(true, paddingTop, paddingTop + dpToPx(72));
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
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);
        fixIcons();

        if (theme1 == 1) {

            listView.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_dark_background)));
        } else {

            if (IS_LIST)
                listView.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.background_light)));
            else listView.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#f2f2f2")));
        }
        if (mLayoutManagerGrid == null)
            if (columns == -1 || columns == 0)
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), 3);
            else
                mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
        listView.setLayoutManager(mLayoutManagerGrid);
        adapter = null;
    }

    void setBackground(Drawable drawable){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            listView.setBackground(drawable);
        }else listView.setBackgroundDrawable(drawable);
    }

    void switchToList() {
        IS_LIST = true;
        if (theme1 == 1) {
                setBackground(new ColorDrawable(getResources().getColor(R.color.holo_dark_background)));
        } else {

            if (IS_LIST)
                setBackground(new ColorDrawable(getResources().getColor(android.R.color.background_light)));
            else setBackground(new ColorDrawable(Color.parseColor("#f2f2f2")));
        }
        ic = new IconHolder(getActivity(), SHOW_THUMBS, !IS_LIST);
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);
        fixIcons();
        if (mLayoutManager == null)
            mLayoutManager = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(mLayoutManager);
        adapter = null;
    }

    public void switchView(){
        createViews(LIST_ELEMENTS,false,CURRENT_PATH,openMode,results,checkforpath(CURRENT_PATH));
    }

    void retrieveFromSavedInstance(final Bundle savedInstanceState) {

        Bundle b = new Bundle();
        String cur = savedInstanceState.getString("CURRENT_PATH");
        if (cur != null) {
            b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);

            openMode = savedInstanceState.getInt("openMode", 0);
            if (openMode == 1)
                smbPath = savedInstanceState.getString("SmbPath");
            LIST_ELEMENTS = savedInstanceState.getParcelableArrayList("list");
            folder_count = savedInstanceState.getInt("folder_count", 0);
            file_count = savedInstanceState.getInt("file_count", 0);
            if (savedInstanceState.getBoolean("results")) {
                try {
                    createViews(LIST_ELEMENTS, true, (CURRENT_PATH), openMode, true,!IS_LIST);
                    pathname.setText(ma.utils.getString(ma.getActivity(), R.string.searchresults));
                    results = true;
                } catch (Exception e) {
                }
            } else {
                createViews(LIST_ELEMENTS, true, (cur), openMode, false, !IS_LIST);
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
            outState.putBoolean("IS_LIST", IS_LIST);
            outState.putParcelableArrayList("list", LIST_ELEMENTS);
            outState.putString("CURRENT_PATH", CURRENT_PATH);
            outState.putBoolean("selection", selection);
            outState.putInt("openMode", openMode);
            outState.putInt("folder_count", folder_count);
            outState.putInt("file_count", file_count);
            if (selection) {
                outState.putIntegerArrayList("position", adapter.getCheckedItemPositions());
            }
            if (results) {
                outState.putBoolean("results", results);
            }
            if (openMode == 1) {
                outState.putString("SmbPath", smbPath);
            }
        }
    }

    public void home() {
        ma.loadlist((ma.home), false, 0);
    }

    public void onListItemClicked(int position, View v) {
        if (position >= LIST_ELEMENTS.size()) return;
        if (results) {
            if (searchTask != null) {
                if (searchTask.getStatus() == AsyncTask.Status.RUNNING)
                    searchTask.cancel(true);
                searchTask = null;
            }
            String path = LIST_ELEMENTS.get(position).getDesc();
            if (selection) adapter.toggleChecked(position);
            else {

                final File f = new File(path);
                if (LIST_ELEMENTS.get(position).isDirectory()) {

                    loadlist(f.getPath(), false, 0);
                    results = false;
                } else {
                    MAIN_ACTIVITY.history.addPath(null,f.getPath(),MAIN_ACTIVITY.HISTORY,0);
                    if (MAIN_ACTIVITY.mReturnIntent) {
                        returnIntentResults(f);
                    } else
                        utils.openFile(f, (MainActivity) getActivity());
                }
            }
        }
        else if (openMode==3){
            /*if(LIST_ELEMENTS.get(position).getSymlink().equals("application/vnd.google-apps.folder")){
                loadlist(LIST_ELEMENTS.get(position).getPermissions(),false,3);
            }else {
                try {
                    driveUtil.getFile(LIST_ELEMENTS.get(position).getPermissions(), MAIN_ACTIVITY.getDriveClient(), new DriveUtil.FileReturn() {
                        @Override
                        public void getFile(com.google.api.services.drive.model.File f) {
                            launch(f);
                        }
                    });
                } catch (UserRecoverableAuthIOException e) {
                    MAIN_ACTIVITY.chooseAccount();
                }*/


        }
        else if (openMode == 1) {
            if (selection) adapter.toggleChecked(position);
            else {
                try {
                    if (LIST_ELEMENTS.get(position).isDirectory())
                        loadlist(LIST_ELEMENTS.get(position).getDesc(), false,openMode);
                    else launch(new SmbFile(LIST_ELEMENTS.get(position).getDesc()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else if (selection == true) {
            if (!LIST_ELEMENTS.get(position).getSize().equals(goback)) {
                adapter.toggleChecked(position);
            } else {
                selection = false;
                if (mActionMode != null)
                    mActionMode.finish();
                mActionMode = null;
            }

        } else {
            if (!LIST_ELEMENTS.get(position).getSize().equals(goback)) {

                String path;
                Layoutelements l = LIST_ELEMENTS.get(position);

                if (!l.hasSymlink()) {

                    path = l.getDesc();
                } else {

                    path = l.getSymlink();
                }

                final File f = new File(path);

                if (l.isDirectory()) {
                    openMode=0;
                    computeScroll();
                    loadlist(f.getPath(), false, openMode);
                } else {
                    MAIN_ACTIVITY.history.addPath(null,f.getPath(),MAIN_ACTIVITY.HISTORY,0);
                    if (MAIN_ACTIVITY.mReturnIntent) {
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
        MAIN_ACTIVITY.mReturnIntent = false;

        Intent intent = new Intent();
        if (MAIN_ACTIVITY.mRingtonePickerIntent) {
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
            intent.setType("audio/mp3");
            getActivity().setResult(getActivity().RESULT_OK, intent);
            getActivity().finish();
        } else {

            Log.d("pickup", "file");
            intent.setData(Uri.fromFile(file));
            getActivity().setResult(getActivity().RESULT_OK, intent);
            getActivity().finish();
        }
    }

    public void loadlist(String path, boolean back, int openMode) {
        if (mActionMode != null) {
            mActionMode.finish();
        }
        if(openMode==3)
            bindDrive(path);
        else new LoadList(back, ma,openMode).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (path));

    }

    void initNoFileLayout() {
        nofilesview = rootView.findViewById(R.id.nofilelayout);
        if (theme1 == 0)
            ((ImageView) nofilesview.findViewById(R.id.image)).setColorFilter(Color.parseColor("#666666"));
        else {
            nofilesview.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            ((TextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
        }
    }

    public boolean checkforpath(String path){
        if(PERM_GRID)return true;
        boolean grid=false;
        for(String s:MAIN_ACTIVITY.gridfiles)
        if((path).contains(s))grid= true;
        if(MAIN_ACTIVITY.listfiles.contains(path))grid=false;
        return grid;
    }

    public void createViews(ArrayList<Layoutelements> bitmap, boolean back, String f, int
            openMode, boolean results,boolean grid) {
        try {
            if (bitmap != null) {
                if (GO_BACK_ITEM)
                    if (!f.equals("/") && openMode == 0) {
                        if (bitmap.size() == 0 || !bitmap.get(0).getSize().equals(goback))
                            bitmap.add(0, utils.newElement(res.getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha), "..", "", "", goback, 0, false, true, ""));
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
                if(grid && IS_LIST)
                    switchToGrid();
                else if(!grid && !IS_LIST)switchToList();
                if (adapter == null)
                    adapter = new Recycleradapter(ma,
                            bitmap, ma.getActivity());
                else {
                    adapter.generate(LIST_ELEMENTS);
                }
                stopAnims = true;
                this.openMode = openMode;
                if (openMode == 0) {
                    MAIN_ACTIVITY.history.addPath(null,f,MAIN_ACTIVITY.HISTORY,0);
                }
                mSwipeRefreshLayout.setRefreshing(false);
                try {
                    listView.setAdapter(adapter);
                    if (!addheader) {
                        listView.removeItemDecoration(dividerItemDecoration);
                        listView.removeItemDecoration(headersDecor);
                        addheader = true;
                    }
                    if (addheader && IS_LIST) {
                        dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST, false, SHOW_DIVIDERS);
                        listView.addItemDecoration(dividerItemDecoration);

                        headersDecor = new StickyRecyclerHeadersDecoration(adapter);
                        listView.addItemDecoration(headersDecor);
                        addheader = false;
                    }
                    mToolbarContainer.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
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

                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    }).start();
                    if (!results) this.results = false;
                    if(openMode!=3 )CURRENT_PATH = f;
                    else if(android.util.Patterns.EMAIL_ADDRESS.matcher(f).matches()){
                        CURRENT_PATH=f;
                        current_drive_id=f;
                    }
                    else current_drive_id=f;
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
                    MAIN_ACTIVITY.updatepaths(no);
                    listView.stopScroll();
                    if (buttons.getVisibility() == View.VISIBLE) MAIN_ACTIVITY.bbar(this);

                } catch (Exception e) {
                }
            } else {//Toast.makeText(getActivity(),res.getString(R.string.error),Toast.LENGTH_LONG).show();
                loadlist(home, true, 0);
            }
        } catch (Exception e) {
        }

    }

    public void rename(final String f) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
        String name = new HFile(f).getName();
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
                    MAIN_ACTIVITY.mainActivityHelper.rename(f, CURRENT_PATH + name);
                else
                    MAIN_ACTIVITY.mainActivityHelper.rename((f), (CURRENT_PATH + "/" + name));

            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

                materialDialog.cancel();
            }
        });
        a.positiveText(R.string.save);
        a.negativeText(R.string.cancel);
        int color = Color.parseColor(fabSkin);
        a.positiveColor(color).negativeColor(color).widgetColor(color);
        a.build().show();
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
        if(openMode==3){
            //// TODO: 07-11-2015
            return;
        }
        if (openMode == 2) {
            loadlist(home, false, 0);
            return;
        }
        File f = new File(CURRENT_PATH);
        if (!results) {
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode == 1)
                    try {
                        if (!MAIN_ACTIVITY.Servers.contains(CURRENT_PATH)) {
                            String path = (new SmbFile(CURRENT_PATH).getParent());
                            loadlist((path), true, openMode);
                        } else loadlist(home, false, 0);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                else if (CURRENT_PATH.equals("/") || CURRENT_PATH.equals(home))
                    MAIN_ACTIVITY.exit();
                else if (utils.canGoBack(f)) {
                    loadlist(f.getParent(), true, openMode);
                } else MAIN_ACTIVITY.exit();
            }
        } else {
            if (searchTask != null) {
                if (searchTask.getStatus() == AsyncTask.Status.RUNNING)
                    searchTask.cancel(true);
                searchTask = null;
            }
            loadlist(f.getPath(), true, 0);
        }
    }

    public void goBackItemClick() {
        if (openMode == 2) {
            updateList();
            return;
        }
        File f = new File(CURRENT_PATH);
        if (!results) {
            if (selection) {
                adapter.toggleChecked(false);
            } else {
                if (openMode == 1)
                    try {
                        if (!CURRENT_PATH.equals(smbPath)) {
                            String path = (new SmbFile(CURRENT_PATH).getParent());
                            loadlist((path), true, 1);
                        } else loadlist(home, false, 0);
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
    }

    void fixIcons() {
        for (Layoutelements layoutelements : LIST_ELEMENTS) {
            Drawable ic = layoutelements.isDirectory() ? folder : Icons.loadMimeIcon(getActivity(), layoutelements.getDesc(), !IS_LIST, res);
            layoutelements.setImageId(ic);
        }
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
                    a.add(new Layoutelements(Icons.loadMimeIcon(getActivity(), mFile[i].getPath(), !IS_LIST, res), mFile[i].getName(), mFile[i].getPath(), "", "", utils.readableFileSize(mFile[i].length()), mFile[1].length(), false, mFile[i].lastModified() + "", false));
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
            if (!MAIN_ACTIVITY.hiddenfiles.contains(ele[0])) {
                if ((ele[6]).equals("true")) {
                    size = "";
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
                        a.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f.getPath(), !IS_LIST, res), f.getPath(), mFile.get(i)[2], mFile.get(i)[1], size, longSize, false, false, ele[4]));
                        file_count++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return a;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    public SmbFile connectingWithSmbServer(String[] auth, boolean anonym) {
        try {
            String yourPeerIP = auth[0], domain = "";
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
        MAIN_ACTIVITY.hiddenfiles = MAIN_ACTIVITY.history.readTable(MAIN_ACTIVITY.HIDDEN);
    }

    public void hide(String path) {
        MAIN_ACTIVITY.history.addPath(null,path,MAIN_ACTIVITY.HIDDEN,0);
        MAIN_ACTIVITY.hiddenfiles = MAIN_ACTIVITY.history.readTable(MAIN_ACTIVITY.HIDDEN);
        if (new File(path).isDirectory()) {
            File f1 = new File(path + "/" + ".nomedia");
            if (!f1.exists()) {
                try {
                    MAIN_ACTIVITY.mainActivityHelper.mkFile(f1.getPath(), this);
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

    public void addSearchResult(ArrayList<String[]> a) {
        if (listView != null) {
            if (!results) {
                LIST_ELEMENTS.clear();
                file_count = 0;
                folder_count = 0;
            }
            ArrayList<Layoutelements> arrayList1 = addTo(a);
            if (arrayList1.size() > 0)
                for (Layoutelements layoutelements : arrayList1)
                    LIST_ELEMENTS.add(layoutelements);
            if (!results) {
                createViews(LIST_ELEMENTS, false, (CURRENT_PATH), openMode, true,!IS_LIST);
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
                Collections.sort(LIST_ELEMENTS, new FileListSorter(dsort, sortby, asc, ROOT_MODE));
                return null;
            }

            @Override
            public void onPostExecute(Void c) {
                createViews(LIST_ELEMENTS, true, (CURRENT_PATH), openMode, true,!IS_LIST);
                pathname.setText(R.string.searchresults);
                results = true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    Loadlistener loadlistener=new Loadlistener.Stub() {
        @Override
        public void load(final List<Layoutelements> layoutelements, String driveId) throws RemoteException {
            System.out.println(layoutelements.size()+"\t"+driveId);
        }

        @Override
        public void error(final String message, final int mode) throws RemoteException {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MAIN_ACTIVITY, "Error " + message+mode, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    IMyAidlInterface aidlInterface;
    boolean mbound=false;
    public void bindDrive(String account) {
        Intent i = new Intent();
        i.setClassName("com.amaze.filemanager.driveplugin", "com.amaze.filemanager.driveplugin.MainService");
        i.putExtra("account",account);
        try {
           getActivity(). bindService((i), mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void unbindDrive(){
        if(mbound!=false)
            getActivity(). unbindService(mConnection);
    }
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            aidlInterface = (IMyAidlInterface.Stub.asInterface(service));
            mbound=true;
            try {
                aidlInterface.registerCallback(loadlistener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                aidlInterface.loadRoot();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mbound=false;
            Log.d("DriveConnection", "DisConnected");
            aidlInterface = null;
        }
    };

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