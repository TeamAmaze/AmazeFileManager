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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.RarAdapter;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.asynctasks.RarHelperTask;
import com.amaze.filemanager.services.asynctasks.ZipHelperTask;
import com.amaze.filemanager.ui.ZipObj;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.utils.BottomBarButtonPath;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class ZipViewer extends Fragment implements BottomBarButtonPath {

    private static final int ZIP_FILE = 0, RAR_FILE = 1;

    private static final String KEY_CACHE_FILES = "cache_files";
    private static final String KEY_PATH = "path";
    private static final String KEY_URI = "uri";
    private static final String KEY_OPEN_MODE = "open_mode";
    private static final String KEY_FILE = "file";
    private static final String KEY_WHOLE_LIST = "whole_list";
    private static final String KEY_ELEMENTS = "elements";
    private static final String KEY_OPEN = "is_open";

    public String s;
    public File f;

    /**
     * files to be deleted from cache
     * with a Map maintaining key - the root of directory created (for deletion purposes after we exit out of here
     * and value - the path of file to open
     */
    public ArrayList<BaseFile> files;
    public Boolean selection = false;
    public String current;
    public String skin, accentColor, iconskin, year;
    public RarAdapter rarAdapter;
    public ActionMode mActionMode;
    public boolean coloriseIcons, showSize, showLastModified, gobackitem;
    public Archive archive;
    public ArrayList<FileHeader> wholelistRar = new ArrayList<>();
    public ArrayList<FileHeader> elementsRar = new ArrayList<>();
    public ArrayList<ZipObj> wholelist = new ArrayList<>();
    public ArrayList<ZipObj> elements = new ArrayList<>();
    public MainActivity mainActivity;
    public RecyclerView listView;
    public SwipeRefreshLayout swipeRefreshLayout;
    public Resources res;
    public boolean isOpen = false;  // flag states whether to open file after service extracts it

    private UtilitiesProviderInterface utilsProvider;
    private View rootView;
    private boolean addheader = true;
    private LinearLayoutManager mLayoutManager;
    private DividerItemDecoration dividerItemDecoration;
    private boolean showDividers;
    private View mToolbarContainer;
    private int openmode;   //0 for zip 1 for rar
    private boolean stopAnims = true;
    private int file = 0, folder = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.main_frag, container, false);
        mainActivity = (MainActivity) getActivity();
        listView = (RecyclerView) rootView.findViewById(R.id.listView);
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (stopAnims && !rarAdapter.stoppedAnimation) {
                    stopAnim();
                }
                rarAdapter.stoppedAnimation = true;

                stopAnims = false;
                return false;
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        return rootView;
    }

    public void stopAnim() {
        for (int j = 0; j < listView.getChildCount(); j++) {
            View v = listView.getChildAt(j);
            if (v != null) v.clearAnimation();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        s = getArguments().getString(KEY_PATH);
        f = new File(Uri.parse(s).getPath());

        mToolbarContainer = mainActivity.getAppbar().getAppbarLayout();
        mToolbarContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (stopAnims) {
                    if ((!rarAdapter.stoppedAnimation)) {
                        stopAnim();
                    }
                    rarAdapter.stoppedAnimation = true;
                }
                stopAnims = false;
                return false;
            }
        });

        listView.setVisibility(View.VISIBLE);
        mLayoutManager = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(mLayoutManager);
        res = getResources();

        if (utilsProvider.getAppTheme().equals(AppTheme.DARK)) {
            rootView.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
        } else {
            listView.setBackgroundColor(Utils.getColor(getContext(), android.R.color.background_light));
        }

        gobackitem = sp.getBoolean("goBack_checkbox", false);
        coloriseIcons = sp.getBoolean("coloriseIcons", true);
        showSize = sp.getBoolean("showFileSize", false);
        showLastModified = sp.getBoolean("showLastModified", true);
        showDividers = sp.getBoolean("showDividers", true);
        year = ("" + Calendar.getInstance().get(Calendar.YEAR)).substring(2, 4);
        skin = mainActivity.getColorPreference().getColorAsString(ColorUsage.PRIMARY);
        accentColor = mainActivity.getColorPreference().getColorAsString(ColorUsage.ACCENT);
        iconskin = mainActivity.getColorPreference().getColorAsString(ColorUsage.ICON_SKIN);

        //mainActivity.findViewById(R.id.buttonbarframe).setBackgroundColor(Color.parseColor(skin));

        if (savedInstanceState == null && f != null) {
            files = new ArrayList<>();
            // adding a cache file to delete where any user interaction elements will be cached
            String fileName = f.getName().substring(0, f.getName().lastIndexOf("."));
            files.add(new BaseFile(getActivity().getExternalCacheDir().getPath() + "/" + fileName));
            if (f.getPath().endsWith(".rar")) {
                openmode = RAR_FILE;
                loadFileList(f.getPath());
            } else {
                openmode = ZIP_FILE;
                loadFileList(s);
            }
        } else {
            onRestoreInstanceState(savedInstanceState);
        }
        mainActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (openmode == ZIP_FILE) {
            outState.putParcelableArrayList(KEY_WHOLE_LIST, wholelist);
            outState.putParcelableArrayList(KEY_ELEMENTS, elements);
        }

        outState.putInt(KEY_OPEN_MODE, openmode);
        outState.putString(KEY_PATH, current);
        outState.putString(KEY_URI, s);
        outState.putString(KEY_FILE, f.getPath());
        outState.putParcelableArrayList(KEY_CACHE_FILES, files);
        outState.putBoolean(KEY_OPEN, isOpen);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        f = new File(savedInstanceState.getString(KEY_FILE));
        s = savedInstanceState.getString(KEY_URI);
        f = new File(Uri.parse(s).getPath());
        files = savedInstanceState.getParcelableArrayList(KEY_CACHE_FILES);
        isOpen = savedInstanceState.getBoolean(KEY_OPEN);
        if (f.getPath().endsWith(".rar")) {
            openmode = RAR_FILE;
            String path = savedInstanceState.getString(KEY_FILE);
            if (path != null && path.length() > 0) {
                f = new File(path);
                current = savedInstanceState.getString(KEY_PATH);
                new RarHelperTask(this, current).execute(f);
            } else {
                loadFileList(f.getPath());
            }
        } else {
            openmode = ZIP_FILE;
            wholelist = savedInstanceState.getParcelableArrayList(KEY_WHOLE_LIST);
            elements = savedInstanceState.getParcelableArrayList(KEY_ELEMENTS);
            current = savedInstanceState.getString(KEY_PATH);
            f = new File(savedInstanceState.getString(KEY_FILE));
            createZipViews(elements, current);
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

        View v;

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            v = getActivity().getLayoutInflater().inflate(R.layout.actionmode, null);
            mode.setCustomView(v);
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            hideOption(R.id.cpy, menu);
            hideOption(R.id.cut, menu);
            hideOption(R.id.delete, menu);
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.openwith, menu);
            showOption(R.id.all, menu);
            hideOption(R.id.compress, menu);
            hideOption(R.id.hide, menu);
            showOption(R.id.ex, menu);
            mode.setTitle(getResources().getString(R.string.select));
            mainActivity.updateViews(new ColorDrawable(Utils.getColor(getContext(), R.color.holo_dark_action_mode)));
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if (mainActivity.colourednavigation)
                    window.setNavigationBarColor(Utils.getColor(getContext(), android.R.color.black));
            }
            if (Build.VERSION.SDK_INT < 19) {
                mainActivity.getAppbar().getToolbar().setVisibility(View.GONE);
            }
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = rarAdapter.getCheckedItemPositions();
            ((TextView) v.findViewById(R.id.item_count)).setText(positions.size() + "");

            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.all:
                    rarAdapter.toggleChecked(true, "");
                    mode.invalidate();
                    return true;
                case R.id.ex:

                    Toast.makeText(getActivity(), getResources().getString(R.string.extracting), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), ExtractService.class);
                    ArrayList<String> a = new ArrayList<>();
                    for (int i : rarAdapter.getCheckedItemPositions()) {
                        a.add(openmode == ZIP_FILE ? elements.get(i).getName() : elementsRar.get(i).getFileNameString());
                    }
                    intent.putExtra(ExtractService.KEY_PATH_ZIP, f.getPath());
                    intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, a);
                    ServiceWatcherUtil.runService(getContext(), intent);
                    mode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            if (rarAdapter != null) rarAdapter.toggleChecked(false, "");
            selection = false;
            mainActivity.updateViews(mainActivity.getColorPreference().getDrawable(ColorUsage.getPrimary(MainActivity.currentTab)));
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if (mainActivity.colourednavigation)
                    window.setNavigationBarColor(mainActivity.skinStatusBar);
            }
            mActionMode = null;
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainActivity.supportInvalidateOptionsMenu();

        // needed to remove any extracted file from cache, when onResume was not called
        // in case of opening any unknown file inside the zip

        if (files.get(0).exists()) {
            new DeleteTask(getActivity().getContentResolver(), getActivity(), this).execute(files);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mainActivity.floatingActionButton.hideMenuButton(true);
        Intent intent = new Intent(getActivity(), ExtractService.class);
        getActivity().bindService(intent, mServiceConnection, 0);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unbindService(mServiceConnection);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // open file if pending
            if (isOpen) {
                // open most recent entry added to files to be deleted from cache
                File cacheFile = new File(files.get(files.size() - 1).getPath());
                if (cacheFile != null && cacheFile.exists())
                    utilsProvider.getFutils().openFile(cacheFile, mainActivity, mainActivity.sharedPref);

                // reset the flag and cache file, as it's root is already in the list for deletion
                isOpen = false;
                files.remove(files.size() - 1);
            }
        }
    };

    private void loadFileList(String path) {
        if(openmode == ZIP_FILE) {
            new ZipHelperTask(this, "").execute(path);

        } else {
            File f = new File(path);
            new RarHelperTask(this, "").execute(f);
        }
    }

    public boolean canGoBack() {
        if (openmode == RAR_FILE) return !(current == null || current.trim().length() == 0);
        else return !(current == null || current.trim().length() == 0);
    }

    public void goBack() {
        if (openmode == RAR_FILE) {
            String path;
            try {
                path = current.substring(0, current.lastIndexOf("\\"));
            } catch (Exception e) {
                path = "";
            }
            new RarHelperTask(this, path).execute(f);
        } else {
            new ZipHelperTask(this, new File(current).getParent()).execute(s);
        }
    }

    private void refresh() {
        switch (openmode) {
            case ZIP_FILE:
                new ZipHelperTask(this, current).execute(s);
                break;
            case RAR_FILE:
                new RarHelperTask(this, current).execute(f);
                break;
        }
    }

    private void updateBottomBar() {
        String path = current != null && current.length() != 0? f.getName() + "/" + current:f.getName();
        mainActivity.getAppbar().getBottomBar().updatePath(path, false, null, OpenMode.FILE, folder, file, this);
    }

    public void createZipViews(ArrayList<ZipObj> zipEntries, String dir) {
        if (rarAdapter == null) {
            rarAdapter = new RarAdapter(getActivity(), utilsProvider, zipEntries, this, true);
            listView.setAdapter(rarAdapter);
        } else rarAdapter.generate(zipEntries, true);
        folder = 0;
        file = 0;
        for (ZipObj zipEntry : zipEntries)
            if (zipEntry.isDirectory()) folder++;
            else file++;
        openmode = ZIP_FILE;
        createViews(dir);
    }

    public void createRarViews(ArrayList<FileHeader> zipEntries, String dir) {
        if (rarAdapter == null) {
            rarAdapter = new RarAdapter(getActivity(), utilsProvider, zipEntries, this);
            listView.setAdapter(rarAdapter);
        } else
            rarAdapter.generate(zipEntries);
        folder = 0;
        file = 0;
        for (FileHeader zipEntry : zipEntries)
            if (zipEntry.isDirectory()) folder++;
            else file++;
        openmode = RAR_FILE;
        createViews(dir);
    }

    private void createViews(String dir) {
        stopAnims = true;
        if (!addheader) {
            listView.removeItemDecoration(dividerItemDecoration);
            //listView.removeItemDecoration(headersDecor);
            addheader = true;
        }
        if (addheader) {
            dividerItemDecoration = new DividerItemDecoration(getActivity(), true, showDividers);
            listView.addItemDecoration(dividerItemDecoration);
            //headersDecor = new StickyRecyclerHeadersDecoration(rarAdapter);
            //listView.addItemDecoration(headersDecor);
            addheader = false;
        }
        final FastScroller fastScroller = (FastScroller) rootView.findViewById(R.id.fastscroll);
        fastScroller.setRecyclerView(listView, 1);
        fastScroller.setPressedHandleColor(mainActivity.getColorPreference().getColor(ColorUsage.ACCENT));
        ((AppBarLayout) mToolbarContainer).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                fastScroller.updateHandlePosition(verticalOffset, 112);
            }
        });
        listView.stopScroll();
        current = dir;
        updateBottomBar();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void changePath(String path) {
        if(path.startsWith("/")) path = path.substring(1);

        if (openmode == ZIP_FILE) {
            new ZipHelperTask(this, path).execute(s);
        } else {
            new RarHelperTask(this, path).execute(f);
        }

        updateBottomBar();
    }

    @Override
    public String getPath() {
        if(current != null && current.length() != 0) return "/" + current;
        else return "";
    }

    @Override
    public int getRootDrawable() {
        return R.drawable.ic_compressed_white_24dp;
    }
}
