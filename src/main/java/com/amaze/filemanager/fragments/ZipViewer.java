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
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
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
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class ZipViewer extends Fragment {

    private UtilitiesProviderInterface utilsProvider;
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
    SharedPreferences Sp;
    ZipViewer zipViewer = this;
    public Archive archive;
    public ArrayList<FileHeader> wholelistRar = new ArrayList<>();
    public ArrayList<FileHeader> elementsRar = new ArrayList<>();
    public ArrayList<ZipObj> wholelist = new ArrayList<>();
    public ArrayList<ZipObj> elements = new ArrayList<>();
    public MainActivity mainActivity;
    public RecyclerView listView;
    View rootView;
    boolean addheader = true;
    public SwipeRefreshLayout swipeRefreshLayout;
    StickyRecyclerHeadersDecoration headersDecor;
    LinearLayoutManager mLayoutManager;
    DividerItemDecoration dividerItemDecoration;
    boolean showDividers;
    public int paddingTop;
    int mToolbarHeight, hidemode;
    View mToolbarContainer;
    public Resources res;
    int openmode;   //0 for zip 1 for rar
    boolean stopAnims=true;

    public boolean isOpen = false;  // flag states whether to open file after service extracts it

    public static final String KEY_CACHE_FILES = "cache_files";
    public static final String KEY_PATH = "path";
    public static final String KEY_URI = "uri";
    public static final String KEY_OPEN_MODE = "open_mode";
    public static final String KEY_FILE = "file";
    public static final String KEY_WHOLE_LIST = "whole_list";
    public static final String KEY_ELEMENTS = "elements";
    public static final String KEY_OPEN = "is_open";

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
                if(stopAnims)

                    if ((!rarAdapter.stoppedAnimation) )
                    {
                        stopAnim();
                    }
                    rarAdapter.stoppedAnimation = true;

                stopAnims=false;
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
    public void stopAnim(){
        for (int j = 0; j < listView.getChildCount(); j++)
        {    View v=listView.getChildAt(j);
            if(v!=null)v.clearAnimation();
        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        s = getArguments().getString(KEY_PATH);
        Uri uri=Uri.parse(s);
        f = new File(uri.getPath());
        mToolbarContainer = getActivity().findViewById(R.id.lin);
        mToolbarContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(stopAnims)
                     {
                        if ((!rarAdapter.stoppedAnimation) )
                        {
                            stopAnim();
                        }
                        rarAdapter.stoppedAnimation = true;

                    }stopAnims=false;
                return false;
            }
        });
        hidemode = Sp.getInt("hidemode", 0);
        listView.setVisibility(View.VISIBLE);
        mLayoutManager = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(mLayoutManager);
        res = getResources();
        mainActivity.supportInvalidateOptionsMenu();
        if (utilsProvider.getAppTheme().equals(AppTheme.DARK))
            rootView.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        else
            listView.setBackgroundColor(getResources().getColor(android.R.color.background_light));

        gobackitem = Sp.getBoolean("goBack_checkbox", false);
        coloriseIcons = Sp.getBoolean("coloriseIcons", true);
        Calendar calendar = Calendar.getInstance();
        showSize = Sp.getBoolean("showFileSize", false);
        showLastModified = Sp.getBoolean("showLastModified", true);
        showDividers = Sp.getBoolean("showDividers", true);
        year = ("" + calendar.get(Calendar.YEAR)).substring(2, 4);
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
                openmode = 1;
                SetupRar(null);
            } else {
                openmode = 0;
                SetupZip(null);
            }
        } else {

            f = new File(savedInstanceState.getString(KEY_FILE));
            s = savedInstanceState.getString(KEY_URI);
            uri = Uri.parse(s);
            f = new File(uri.getPath());
            files = savedInstanceState.getParcelableArrayList(KEY_CACHE_FILES);
            isOpen = savedInstanceState.getBoolean(KEY_OPEN);
            if (f.getPath().endsWith(".rar")) {
                openmode = 1;
                SetupRar(savedInstanceState);
            } else {
                openmode = 0;
                SetupZip(savedInstanceState);
            }

        }
        String fileName=null;
        try {
            if (uri.getScheme().equals(KEY_FILE)) {
                fileName = uri.getLastPathSegment();
            } else {
                Cursor cursor = null;
                try {
                    cursor = getActivity().getContentResolver().query(uri, new String[]{
                            MediaStore.Images.ImageColumns.DISPLAY_NAME
                    }, null, null, null);

                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
                    }
                } finally {

                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(fileName==null || fileName.trim().length()==0)fileName=f.getName();
        try {
            mainActivity.setActionBarTitle(fileName);
        } catch (Exception e) {
            mainActivity.setActionBarTitle(getResources().getString(R.string.zip_viewer));
        }
        mainActivity.supportInvalidateOptionsMenu();
        mToolbarHeight = getToolbarHeight(getActivity());
        paddingTop = (mToolbarHeight) + dpToPx(72);
        mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                paddingTop = mToolbarContainer.getHeight();
                mToolbarHeight = mainActivity.toolbar.getHeight();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }

        });

    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        putDatatoSavedInstance(outState);
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
            mainActivity.updateViews(new ColorDrawable(getResources().getColor(R.color.holo_dark_action_mode)));
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if (mainActivity.colourednavigation)
                    window.setNavigationBarColor(getResources().getColor(android.R.color.black));
            }
            if (Build.VERSION.SDK_INT < 19)
                getActivity().findViewById(R.id.action_bar).setVisibility(View.GONE);
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
                     rarAdapter.toggleChecked(true,"");
                    mode.invalidate();
                    return true;
                case R.id.ex:

                    Toast.makeText(getActivity(), getResources().getString(R.string.extracting), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), ExtractService.class);
                    ArrayList<String> a = new ArrayList<>();
                    for (int i : rarAdapter.getCheckedItemPositions()) {
                        a.add(openmode==0 ? elements.get(i).getName() : elementsRar.get(i).getFileNameString());
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

            new DeleteTask(getActivity().getContentResolver(), getActivity(), this).execute((files));
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
                File cacheFile = new File(files.get(files.size()-1).getPath());
                if (cacheFile != null && cacheFile.exists())
                    utilsProvider.getFutils().openFile(cacheFile, zipViewer.mainActivity);

                // reset the flag and cache file, as it's root is already in the list for deletion
                isOpen = false;
                files.remove(files.size()-1);
            }
        }
    };

    void putDatatoSavedInstance(Bundle outState) {
        if (openmode == 0) {

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

    void SetupRar(Bundle savedInstanceState) {

        if (savedInstanceState == null)
            loadRarlist(f.getPath());
        else {
            String path = savedInstanceState.getString(KEY_FILE);
            if (path != null && path.length() > 0) {
                f = new File(path);
                current = savedInstanceState.getString(KEY_PATH);
                new RarHelperTask(this, current).execute(f);
            } else loadRarlist(f.getPath());
        }
    }

    void SetupZip(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            loadlist(s);
        else {
            wholelist = savedInstanceState.getParcelableArrayList(KEY_WHOLE_LIST);
            elements = savedInstanceState.getParcelableArrayList(KEY_ELEMENTS);
            current = savedInstanceState.getString(KEY_PATH);
            f = new File(savedInstanceState.getString(KEY_FILE));
            createviews(elements, current);
        }
    }

    public void loadRarlist(String path) {
        File f = new File(path);
        new RarHelperTask(this, "").execute(f);

    }

    public boolean cangoBackRar() {
        return !(current == null || current.trim().length() == 0);
    }

    public void goBackRar() {
        String path;
        try {
            path = current.substring(0, current.lastIndexOf("\\"));
        } catch (Exception e) {
            path = "";
        }
        new RarHelperTask(this, path).execute(f);
    }

    public boolean canGoBack() {
        if (openmode == 1) return cangoBackRar();
        else return !(current == null || current.trim().length() == 0);
    }

    public void goBack() {
        if (openmode == 1) {
            goBackRar();
            return;
        }
        new ZipHelperTask(this, new File(current).getParent()).execute(s);
    }

    void refresh() {
        switch (openmode) {
            case 0:
                new ZipHelperTask(this, current).execute(s);
                break;
            case 1:
                new RarHelperTask(this, current).execute(f);
                break;
        }
    }


    public void bbar() {
        if (current != null && current.length()!=0)
            mainActivity.updatePath("/" + current,  false, OpenMode.FILE,folder,file);
        else     mainActivity.updatePath("/", false,OpenMode.FILE,folder,file);


    }
    int file=0,folder=0;
    public void createviews(ArrayList<ZipObj> zipEntries, String dir) {
        if(rarAdapter==null) {
            zipViewer.rarAdapter = new RarAdapter(zipViewer.getActivity(), utilsProvider, zipEntries, zipViewer,true);
            zipViewer.listView.setAdapter(zipViewer.rarAdapter);
        }
        else rarAdapter.generate(zipEntries,true);
        folder=0;
        file=0;
        for (ZipObj zipEntry:zipEntries)
        if(zipEntry.isDirectory())folder++;
        else file++;
        createViews(dir);
        openmode = 0;
    }

    public void createRarviews(ArrayList<FileHeader> zipEntries, String dir) {
        if(rarAdapter==null){
            zipViewer.rarAdapter = new RarAdapter(zipViewer.getActivity(), utilsProvider, zipEntries, zipViewer);
            zipViewer.listView.setAdapter(zipViewer.rarAdapter);
        }else
        rarAdapter.generate(zipEntries);
        folder=0;
        file=0;
        for (FileHeader zipEntry:zipEntries)
            if(zipEntry.isDirectory())folder++;
            else file++;
        openmode = 1;
        createViews(dir);
    }

    void createViews(String dir) {
        stopAnims=true;
        if (!addheader) {
            listView.removeItemDecoration(dividerItemDecoration);
            listView.removeItemDecoration(headersDecor);
            addheader = true;
        }
        if (addheader) {
            dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST, true, showDividers);
            listView.addItemDecoration(dividerItemDecoration);
            headersDecor = new StickyRecyclerHeadersDecoration(rarAdapter);
            listView.addItemDecoration(headersDecor);
            addheader = false;
        }
        final FastScroller fastScroller=(FastScroller)rootView.findViewById(R.id.fastscroll);
        fastScroller.setRecyclerView(listView,1);
        fastScroller.setPressedHandleColor(mainActivity.getColorPreference().getColor(ColorUsage.ACCENT));
        ((AppBarLayout)mToolbarContainer).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                fastScroller.updateHandlePosition(verticalOffset,112);
            }
        });
        listView.stopScroll();
        zipViewer.current = dir;
        zipViewer.bbar();
        swipeRefreshLayout.setRefreshing(false);
    }

    public void loadlist(String path) {
        new ZipHelperTask(this, "").execute(path);

    }
}
