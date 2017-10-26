/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 *                      Emmanuel Messulam<emmanuelbendavid@gmail.com>
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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.ZipExplorerAdapter;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.asynctasks.RarHelperTask;
import com.amaze.filemanager.asynchronous.asynctasks.ZipHelperTask;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.ZipObjectParcelable;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.utils.BottomBarButtonPath;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class ZipExplorerFragment extends Fragment implements BottomBarButtonPath {
    public static final String KEY_PATH = "path";

    private static final int ZIP_FILE = 0, RAR_FILE = 1;

    private static final String KEY_CACHE_FILES = "cache_files";
    private static final String KEY_URI = "uri";
    private static final String KEY_OPEN_MODE = "open_mode";
    private static final String KEY_FILE = "file";
    private static final String KEY_WHOLE_LIST = "whole_list";
    private static final String KEY_ELEMENTS = "elements";
    private static final String KEY_OPEN = "is_open";

    public File realZipFile;

    /**
     * files to be deleted from cache
     * with a Map maintaining key - the root of directory created (for deletion purposes after we exit out of here
     * and value - the path of file to open
     */
    public ArrayList<HybridFileParcelable> files;
    public boolean selection = false;
    public String relativeDirectory = "";//Normally this would be "/" but for pathing issues it isn't
    public String skin, accentColor, iconskin, year;
    public ZipExplorerAdapter zipExplorerAdapter;
    public ActionMode mActionMode;
    public boolean coloriseIcons, showSize, showLastModified, gobackitem;
    public Archive archive;
    public ArrayList<FileHeader> elementsRar = new ArrayList<>();
    public ArrayList<ZipObjectParcelable> elements = new ArrayList<>();
    public MainActivity mainActivity;
    public RecyclerView listView;
    public SwipeRefreshLayout swipeRefreshLayout;
    public boolean isOpen = false;  // flag states whether to open file after service extracts it

    private UtilitiesProviderInterface utilsProvider;
    private View rootView;
    private boolean addheader = true;
    private LinearLayoutManager mLayoutManager;
    private DividerItemDecoration dividerItemDecoration;
    private boolean showDividers;
    private View mToolbarContainer;
    private int openmode;
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
        listView.setOnTouchListener((view, motionEvent) -> {
            if (stopAnims && !zipExplorerAdapter.stoppedAnimation) {
                stopAnim();
            }
            zipExplorerAdapter.stoppedAnimation = true;

            stopAnims = false;
            return false;
        });
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

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
        realZipFile = new File(Uri.parse(getArguments().getString(KEY_PATH)).getPath());

        mToolbarContainer = mainActivity.getAppbar().getAppbarLayout();
        mToolbarContainer.setOnTouchListener((view, motionEvent) -> {
            if (stopAnims) {
                if ((!zipExplorerAdapter.stoppedAnimation)) {
                    stopAnim();
                }
                zipExplorerAdapter.stoppedAnimation = true;
            }
            stopAnims = false;
            return false;
        });

        listView.setVisibility(View.VISIBLE);
        mLayoutManager = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(mLayoutManager);

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

        if (savedInstanceState == null && realZipFile != null) {
            files = new ArrayList<>();
            // adding a cache file to delete where any user interaction elements will be cached
            String fileName = realZipFile.getName().substring(0, realZipFile.getName().lastIndexOf("."));
            files.add(new HybridFileParcelable(getActivity().getExternalCacheDir().getPath() + "/" + fileName));
            if (realZipFile.getPath().endsWith(".rar")) {
                openmode = RAR_FILE;
            } else {
                openmode = ZIP_FILE;
            }

            changePath("");
        } else {
            onRestoreInstanceState(savedInstanceState);
        }
        mainActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (openmode == ZIP_FILE) {
            outState.putParcelableArrayList(KEY_ELEMENTS, elements);
        }

        outState.putInt(KEY_OPEN_MODE, openmode);
        outState.putString(KEY_PATH, relativeDirectory);
        outState.putString(KEY_URI, realZipFile.getPath());
        outState.putString(KEY_FILE, realZipFile.getPath());
        outState.putParcelableArrayList(KEY_CACHE_FILES, files);
        outState.putBoolean(KEY_OPEN, isOpen);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        realZipFile = new File(Uri.parse(savedInstanceState.getString(KEY_URI)).getPath());
        files = savedInstanceState.getParcelableArrayList(KEY_CACHE_FILES);
        isOpen = savedInstanceState.getBoolean(KEY_OPEN);
        if (realZipFile.getPath().endsWith(".rar")) {
            openmode = RAR_FILE;
            String path = savedInstanceState.getString(KEY_FILE);
            if (path != null && path.length() > 0) {
                realZipFile = new File(path);
                relativeDirectory = savedInstanceState.getString(KEY_PATH, "");
                changeRarPath(relativeDirectory);
            } else {
                changePath("");
            }
        } else {
            openmode = ZIP_FILE;
            elements = savedInstanceState.getParcelableArrayList(KEY_ELEMENTS);
            relativeDirectory = savedInstanceState.getString(KEY_PATH, "");
            realZipFile = new File(savedInstanceState.getString(KEY_FILE));
            createZipViews(elements, relativeDirectory);
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
            ArrayList<Integer> positions = zipExplorerAdapter.getCheckedItemPositions();
            ((TextView) v.findViewById(R.id.item_count)).setText(positions.size() + "");

            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.all:
                    zipExplorerAdapter.toggleChecked(true);
                    mode.invalidate();
                    return true;
                case R.id.ex:

                    Toast.makeText(getActivity(), getResources().getString(R.string.extracting), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), ExtractService.class);
                    ArrayList<String> a = new ArrayList<>();
                    for (int i : zipExplorerAdapter.getCheckedItemPositions()) {
                        a.add(openmode == ZIP_FILE ? elements.get(i).getName() : elementsRar.get(i).getFileNameString());
                    }
                    intent.putExtra(ExtractService.KEY_PATH_ZIP, realZipFile.getPath());
                    intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, a);
                    ServiceWatcherUtil.runService(getContext(), intent);
                    mode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            if (zipExplorerAdapter != null) zipExplorerAdapter.toggleChecked(false);
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
                if (cacheFile.exists()) {
                    FileUtils.openFile(cacheFile, mainActivity, mainActivity.getPrefs());
                }
                // reset the flag and cache file, as it's root is already in the list for deletion
                isOpen = false;
                files.remove(files.size() - 1);
            }
        }
    };

    public boolean canGoBack() {
        if (openmode == RAR_FILE) return !(relativeDirectory == null || relativeDirectory.trim().length() == 0);
        else return !(relativeDirectory == null || relativeDirectory.trim().length() == 0);
    }

    public void goBack() {
        if (openmode == RAR_FILE) {
            String path;
            try {
                path = relativeDirectory.substring(0, relativeDirectory.lastIndexOf("/"));
            } catch (Exception e) {
                path = "";
            }
            changeRarPath(path);
        } else {
            changeZipPath(new File(relativeDirectory).getParent());
        }
    }

    @Override
    public void changePath(String path) {
        if(path.startsWith("/")) path = path.substring(1);

        if (openmode == ZIP_FILE) {// TODO: 15/9/2017 put switch
            changeZipPath(path);
        } else {
            changeRarPath(path);
        }

        updateBottomBar();
    }

    @Override
    public String getPath() {
        if(relativeDirectory != null && relativeDirectory.length() != 0) return "/" + relativeDirectory;
        else return "";
    }

    @Override
    public int getRootDrawable() {
        return R.drawable.ic_compressed_white_24dp;
    }

    /**
     * The folders's path separator must be "/"
     */
    public void changeZipPath(final String folder) {
        swipeRefreshLayout.setRefreshing(true);
        new ZipHelperTask(getContext(), realZipFile.getPath(), folder, data -> {
            if (gobackitem && relativeDirectory != null && relativeDirectory.trim().length() != 0)
                elements.add(0, new ZipObjectParcelable(null, 0, 0, true));
            elements = data;
            createZipViews(data, folder);

            swipeRefreshLayout.setRefreshing(false);
            updateBottomBar();
        }).execute();
    }

    /**
     * The folders's path separator must be "/"
     */
    public void changeRarPath(final String folder) {
        swipeRefreshLayout.setRefreshing(true);
        new RarHelperTask(getContext(), realZipFile.getPath(), folder,
                data -> {
                    archive = data.first;
                    if(data.second != null) {
                        createRarViews(data.second, folder);
                        elementsRar = data.second;
                    }

                    swipeRefreshLayout.setRefreshing(false);
                    updateBottomBar();
                }).execute();
    }

    private void refresh() {
        changePath(relativeDirectory);
    }

    private void updateBottomBar() {
        String path = relativeDirectory != null && relativeDirectory.length() != 0? realZipFile.getName() + "/" + relativeDirectory : realZipFile.getName();
        mainActivity.getAppbar().getBottomBar().updatePath(path, false, null, OpenMode.FILE, folder, file, this);
    }

    private void createZipViews(ArrayList<ZipObjectParcelable> zipEntries, String dir) {
        if (zipExplorerAdapter == null) {
            zipExplorerAdapter = new ZipExplorerAdapter(getActivity(), utilsProvider, zipEntries, null, this, true);
            listView.setAdapter(zipExplorerAdapter);
        } else {
            zipExplorerAdapter.generateZip(zipEntries);
        }
        folder = 0;
        file = 0;
        for (ZipObjectParcelable zipEntry : zipEntries) {
            if (zipEntry.isDirectory()) folder++;
            else file++;
        }
        openmode = ZIP_FILE;
        createViews(dir);
    }

    private void createRarViews(ArrayList<FileHeader> rarEntries, String dir) {
        if (zipExplorerAdapter == null) {
            zipExplorerAdapter = new ZipExplorerAdapter(getActivity(), utilsProvider, null, rarEntries, this, false);
            listView.setAdapter(zipExplorerAdapter);
        } else {
            zipExplorerAdapter.generateRar(rarEntries);
        }

        folder = 0;
        file = 0;
        for (FileHeader zipEntry : rarEntries) {
            if (zipEntry.isDirectory()) folder++;
            else file++;
        }
        openmode = RAR_FILE;
        createViews(dir);
    }

    private void createViews(String dir) {
        stopAnims = true;
        if (!addheader) {
            listView.removeItemDecoration(dividerItemDecoration);
            //listView.removeItemDecoration(headersDecor);
            addheader = true;
        } else {
            dividerItemDecoration = new DividerItemDecoration(getActivity(), true, showDividers);
            listView.addItemDecoration(dividerItemDecoration);
            //headersDecor = new StickyRecyclerHeadersDecoration(zipExplorerAdapter);
            //listView.addItemDecoration(headersDecor);
            addheader = false;
        }
        final FastScroller fastScroller = (FastScroller) rootView.findViewById(R.id.fastscroll);
        fastScroller.setRecyclerView(listView, 1);
        fastScroller.setPressedHandleColor(mainActivity.getColorPreference().getColor(ColorUsage.ACCENT));
        ((AppBarLayout) mToolbarContainer).addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            fastScroller.updateHandlePosition(verticalOffset, 112);
        });
        listView.stopScroll();
        relativeDirectory = dir;
        updateBottomBar();
        swipeRefreshLayout.setRefreshing(false);
    }

}
