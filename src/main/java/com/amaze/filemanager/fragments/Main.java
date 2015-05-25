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
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.Recycleradapter;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.services.asynctasks.LoadSearchList;
import com.amaze.filemanager.services.asynctasks.LoadSmbList;
import com.amaze.filemanager.utils.DividerItemDecoration;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HidingScrollListener;
import com.amaze.filemanager.utils.HistoryManager;
import com.amaze.filemanager.utils.IconHolder;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Shortcuts;
import com.amaze.filemanager.utils.SpacesItemDecoration;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class Main extends android.support.v4.app.Fragment {
    public File[] file;
    public ArrayList<Layoutelements> list;
    public ArrayList<SmbFile> smbFiles;
    public Recycleradapter adapter;
    public Futils utils;
    public boolean selection;
    public boolean results = false,smbMode=false;
    public ActionMode mActionMode;
    public SharedPreferences Sp;
    public Drawable folder,apk,darkimage,darkvideo;
    Resources res;
    public LinearLayout buttons;
    public int sortby, dsort, asc;

    public String home, current = Environment.getExternalStorageDirectory().getPath();
    Shortcuts sh;
    HashMap<String, Bundle> scrolls = new HashMap<String, Bundle>();
    Main ma = this;
    public HistoryManager history,hidden;
    IconUtils icons;
    public boolean rootMode,showHidden,circularImages,showPermissions,showSize,showLastModified;
    View footerView;
    public LinearLayout pathbar;
    public CountDownTimer timer;
    private View rootView;
    public android.support.v7.widget.RecyclerView  listView;
    public boolean gobackitem,islist,showThumbs,coloriseIcons,showDividers;
    public IconHolder ic;
    public MainActivity mainActivity;
    public boolean showButtonOnStart = false;
    public String skin, fabSkin,iconskin;
    public int skinselection;
    public int theme;
    public int theme1;
    public float[] color;
    public ColorMatrixColorFilter colorMatrixColorFilter;
    public Animation animation,animation1;
    public String year,goback;
    ArrayList<String> hiddenfiles;
    String Intentpath,itemsstring;
    int no;
    TabHandler tabHandler;
    boolean savepaths;
    LinearLayoutManager mLayoutManager;
    GridLayoutManager mLayoutManagerGrid;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    boolean addheader=false;
    StickyRecyclerHeadersDecoration headersDecor;
    DividerItemDecoration dividerItemDecoration;
    public int paddingTop;
    int mToolbarHeight,hidemode;
    View mToolbarContainer;
    public int skin_color,icon_skin_color;
    boolean SmbAnonym=false;
    String smbUser,smbPass;
    public String smbPath;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity=(MainActivity)getActivity();
        no=getArguments().getInt("no", 1);
        home=getArguments().getString("home");
        current=getArguments().getString("lastpath");
        tabHandler=new TabHandler(getActivity(),null,null,1);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        savepaths=Sp.getBoolean("savepaths", true);
        skin = Sp.getString("skin_color", "#3f51b5");
        fabSkin = Sp.getString("fab_skin_color", "#e91e63");
        iconskin=Sp.getString("icon_skin_color", skin);
        skin_color=Color.parseColor(skin);
        icon_skin_color=Color.parseColor(iconskin);
        sh = new Shortcuts(getActivity());
        islist = Sp.getBoolean("view", true);
        Calendar calendar = Calendar.getInstance();
        year=(""+calendar.get(Calendar.YEAR)).substring(2, 4);
        theme=Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;
        hidemode=Sp.getInt("hidemode", 0);
        showPermissions=Sp.getBoolean("showPermissions", false);
        showSize=Sp.getBoolean("showFileSize",false);
        showDividers=Sp.getBoolean("showDividers",true);
        gobackitem=Sp.getBoolean("goBack_checkbox", false);
        circularImages=Sp.getBoolean("circularimages",true);
        showLastModified=Sp.getBoolean("showLastModified",true);
        icons = new IconUtils(Sp, getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.main_frag, container, false);
        listView = (android.support.v7.widget.RecyclerView) rootView.findViewById(R.id.listView);
        if(getArguments()!=null)
        Intentpath=getArguments().getString("path");
        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.load_list_anim);
        animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_newtab);
        mToolbarContainer=getActivity().findViewById(R.id.lin);
        buttons = (LinearLayout) getActivity().findViewById(R.id.buttons);
        pathbar = (LinearLayout) getActivity().findViewById(R.id.pathbar);
        showThumbs=Sp.getBoolean("showThumbs", true);
        ic=new IconHolder(getActivity(),showThumbs,!islist);
        res = getResources();
        goback=res.getString(R.string.goback);
        itemsstring=res.getString(R.string.items);
        apk=res.getDrawable(R.drawable.ic_doc_apk_grid);
        if(theme1==1) {

            mainActivity.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_dark_background)));
        } else {

            if(islist)  mainActivity.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.background_light)));

        } listView.setHasFixedSize(true);
        mLayoutManager=new LinearLayoutManager(getActivity());
        int columns=Integer.parseInt(Sp.getString("columns","3"));
        mLayoutManagerGrid=new GridLayoutManager(getActivity(),columns);
        if (islist) {
            listView.setLayoutManager(mLayoutManager);
        } else {
            listView.setLayoutManager(mLayoutManagerGrid);
        }
        mToolbarContainer.setBackgroundColor(skin_color);
     //   listView.setPadding(listView.getPaddingLeft(), paddingTop, listView.getPaddingRight(), listView.getPaddingBottom());
        return rootView;
    }public int dpToPx(int dp) {
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        mainActivity = (MainActivity) getActivity();
        utils = new Futils();
        String x=getSelectionColor();
        skinselection=Color.parseColor(x);
        color=calculatevalues(x);
        ColorMatrix colorMatrix = new ColorMatrix(calculatefilter(color));
        colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        history = new HistoryManager(getActivity(), "Table1");
        hidden = new HistoryManager(getActivity(), "Table2");
        hiddenfiles=hidden.readTable();
        rootMode = Sp.getBoolean("rootmode", false);
        showHidden=Sp.getBoolean("showHidden",false);
        coloriseIcons=Sp.getBoolean("coloriseIcons",false);
        if(islist){
            folder = res.getDrawable(R.drawable.ic_grid_folder_new);}
        else{folder = res.getDrawable(R.drawable.ic_grid_folder1);}
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);
        getSortModes();
        darkimage=res.getDrawable(R.drawable.ic_doc_image_dark);
        darkvideo=res.getDrawable(R.drawable.ic_doc_video_dark);
        this.setRetainInstance(false);
        File f;
        if(savepaths)
            f=new File(current);
        else f=new File(home);
        mainActivity.initiatebbar();

        // use a linear layout manager
        footerView=getActivity().getLayoutInflater().inflate(R.layout.divider, null);
           mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadlist(new File(current),false);
            }
        });
        mToolbarHeight=getToolbarHeight(getActivity());
        paddingTop = (mToolbarHeight) + dpToPx(72);
        if(hidemode==2)mToolbarHeight=paddingTop;
        mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else {
                    mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                paddingTop=mToolbarContainer.getHeight();

                if(hidemode!=2)mToolbarHeight=mainActivity.toolbar.getHeight();
            }

        });
        mSwipeRefreshLayout.setColorSchemeColors(Color.parseColor(fabSkin));
        mSwipeRefreshLayout.setProgressViewOffset(true, paddingTop, paddingTop + dpToPx(72));

        if (savedInstanceState == null){
            if(Intentpath!=null) {
                File file1=new File(Intentpath);
                if(file1.isDirectory()) {

                    loadlist(file1,false);
                }
                else {
                    utils.openFile(file1, mainActivity);
                    loadlist(f,false);
                }
            } else {

                loadlist(f, false);
            }
        } else {
            Bundle b = new Bundle();
            String cur = savedInstanceState.getString("current");
            if (cur != null) {
                b.putInt("index", savedInstanceState.getInt("index"));
                b.putInt("top", savedInstanceState.getInt("top"));
                scrolls.put(cur, b);
                retreiveSmbFromSavedInstance(savedInstanceState);
                list = savedInstanceState.getParcelableArrayList("list");
                if (savedInstanceState.getBoolean("results")) {
                    try {
                        createViews(list, true, new File(current));
                        ((TextView) ma.pathbar.findViewById(R.id.pathname)).setText(ma.utils.getString(ma.getActivity(), R.string.searchresults));
                        results = true;
                    } catch (Exception e) {
                    }
                }else{
                    createViews(list, true, new File(cur));
                }
                if (savedInstanceState.getBoolean("selection")) {

                    for (int i : savedInstanceState.getIntegerArrayList("position")) {
                        adapter.toggleChecked(i);
                    }
                }
            }
        }
     }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int index;
        View vi;
        if(listView!=null){
            if (islist) {

                index = (mLayoutManager).findFirstVisibleItemPosition();
                vi = listView.getChildAt(0);
            } else {
                index =  (mLayoutManagerGrid).findFirstVisibleItemPosition();
                vi = listView.getChildAt(0);
            }
            int top = (vi == null) ? 0 : vi.getTop();
            outState.putInt("index", index);
            outState.putInt("top", top);
            outState.putParcelableArrayList("list", list);
            outState.putString("current", current);
            outState.putBoolean("selection", selection);
            if (selection) {
                outState.putIntegerArrayList("position", adapter.getCheckedItemPositions());
            }
            if(results){
                outState.putBoolean("results", results);
            }
            if(smbMode)
                addSmbToSavedInstance(outState,smbUser,smbPass,smbFiles,SmbAnonym);
        }}

    public void home() {
        ma.loadlist(new File(ma.home), false);
    }



    public void onListItemClicked(int position, View v) {
        if (results) {
            String path = list.get(position).getDesc();
            if(selection)adapter.toggleChecked(position);
            else{

            final File f = new File(path);
            if (list.get(position).isDirectory()) {

                loadlist(f, false);
                results = false;
            } else {
                if (mainActivity.mReturnIntent) {
                    returnIntentResults(f);
                } else
                utils.openFile(f, (MainActivity) getActivity());
            }}
        }else if(smbMode){
            if(selection)adapter.toggleChecked(position);
            else{Toast.makeText(getActivity(),"Go",Toast.LENGTH_LONG).show();
                    loadSmblist(smbFiles.get(position), false);}
        } else if (selection == true) {
            if(!list.get(position).getSize().equals(goback)){
                adapter.toggleChecked(position);
            }else{selection = false;
                if(mActionMode!=null)
                    mActionMode.finish();
                mActionMode = null;}

        } else {
            if(!list.get(position).getSize().equals(goback)){

                String path;
                Layoutelements l=list.get(position);

                if(!l.hasSymlink()){

                    path= l.getDesc();
                }
                else {

                    path=l.getSymlink();
                }

                final File f = new File(path);

                if (l.isDirectory()) {

                    computeScroll();
                    loadlist(f, false);
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

    private void returnIntentResults (File file) {
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

    public void loadlist(final File f, boolean back) {
        if(mActionMode!=null){mActionMode.finish();}
        new LoadList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, f);

        try {
            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.load_list_anim);
        } catch (Exception e) {
            e.printStackTrace();
        }

        listView.setAnimation(animation);
    }
    public void loadSmblist(final SmbFile f, boolean back) {
        if(mActionMode!=null){mActionMode.finish();}
        new LoadSmbList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, f);

        try {
            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.load_list_anim);
        } catch (Exception e) {
            e.printStackTrace();
        }

        listView.setAnimation(animation);
    }

    @SuppressWarnings("unchecked")
    public void loadsearchlist(ArrayList<String[]> f) {

        new LoadSearchList(ma).execute(f);

    }


    public void createViews(ArrayList<Layoutelements> bitmap, boolean back, File f) {
        try {
            if (bitmap != null) {
                if(gobackitem)
                if (!f.getPath().equals("/")) {
                    if (bitmap.size() == 0 || !bitmap.get(0).getSize().equals(goback))
                        bitmap.add(0, utils.newElement(res.getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha), "..", "", "", goback,false, true,""));
                }
                adapter = new Recycleradapter(ma,
                        bitmap, ma.getActivity());
                mSwipeRefreshLayout.setRefreshing(false);
                try {    listView.setAdapter(adapter);
                    if(!addheader && islist){
                       listView.removeItemDecoration(dividerItemDecoration);
                        listView.removeItemDecoration(headersDecor);
                        addheader=true;}
                    if(addheader && islist){
                            dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST,showDividers);
                            listView.addItemDecoration(dividerItemDecoration);

                    headersDecor = new StickyRecyclerHeadersDecoration(adapter);
                    listView.addItemDecoration(headersDecor);addheader=false;}
                    results = false;
                    current = f.getPath();
                    if (back) {
                        if (scrolls.containsKey(current)) {
                            Bundle b = scrolls.get(current);
                            if(islist)
                            mLayoutManager.scrollToPositionWithOffset(b.getInt("index"),b.getInt("top"));
                        else    mLayoutManagerGrid.scrollToPositionWithOffset(b.getInt("index"),b.getInt("top"));
                        }
                    }
                    //floatingActionButton.show();
                    mainActivity.updatepaths();
                    listView.setOnScrollListener(new HidingScrollListener(mToolbarHeight,hidemode) {

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
                            mToolbarContainer.findViewById(R.id.lin).animate().translationY(-mToolbarHeight).setInterpolator(new AccelerateInterpolator(2)).start();
                        }

                    });
                    if (buttons.getVisibility() == View.VISIBLE) mainActivity.bbar(this);

                    mainActivity.updateDrawer(current);
                    mainActivity.updatepager();

                    } catch (Exception e) {
                }
            } else {//Toast.makeText(getActivity(),res.getString(R.string.error),Toast.LENGTH_LONG).show();
                loadlist(new File(current), true);
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
        View v;
        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            v=getActivity().getLayoutInflater().inflate(R.layout.actionmode,null);
            try {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                v.setMinimumWidth(getActivity().findViewById(R.id.tab_spinner).getWidth());
            } catch (Exception e) {
                e.printStackTrace();
            }
            mode.setCustomView(v);
            mainActivity.setPagingEnabled(false);
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.addshortcut,menu);
            hideOption(R.id.sethome, menu);
            hideOption(R.id.rename, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.about, menu);
            hideOption(R.id.openwith, menu);
            hideOption(R.id.ex, menu);
            if(mainActivity.mReturnIntent)
            showOption(R.id.openmulti,menu);
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
                if(mainActivity.colourednavigation)
                    window.setNavigationBarColor(res.getColor(android.R.color.black));
            }
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = adapter.getCheckedItemPositions();
            TextView textView1 = (TextView) v.findViewById(R.id.item_count);
            textView1.setText(positions.size() + "");
            textView1.setOnClickListener(null);
            hideOption(R.id.openmulti,menu);
            if(mainActivity.mReturnIntent)
                if(Build.VERSION.SDK_INT>=16)
            showOption(R.id.openmulti, menu);
            //tv.setText(positions.size());
            if(!results)
            {hideOption(R.id.openparent,menu);
                if (positions.size() == 1) {
                showOption(R.id.addshortcut,menu);
                showOption(R.id.permissions, menu);

                showOption(R.id.openwith, menu);
                showOption(R.id.about, menu);
                showOption(R.id.share,menu);
                showOption(R.id.rename, menu);

                File x = new File(list.get(adapter.getCheckedItemPositions().get(0))

                        .getDesc());

                if (x.isDirectory()) {
                    hideOption(R.id.openwith,menu);
                    showOption(R.id.sethome, menu);
                    hideOption(R.id.share,menu);
                    hideOption(R.id.openmulti, menu);
                } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk") || x.getName().toLowerCase().endsWith(".rar")|| x.getName().toLowerCase().endsWith(".tar")|| x.getName().toLowerCase().endsWith(".tar.gz")) {

                    showOption(R.id.ex, menu);

                    if(mainActivity.mReturnIntent)
                        if(Build.VERSION.SDK_INT>=16)
                    showOption(R.id.openmulti,menu);
                }
            } else {
                try {
                    if(mainActivity.mReturnIntent)
                        if(Build.VERSION.SDK_INT>=16)showOption(R.id.openmulti,menu);
                    for (int c : adapter.getCheckedItemPositions()) {
                        File x = new File(list.get(c).getDesc());
                        if (x.isDirectory()) {
                            hideOption(R.id.share, menu);
                            hideOption(R.id.openmulti,menu);
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

            }}else {
                if (positions.size() == 1) {
                    showOption(R.id.addshortcut,menu);
                    showOption(R.id.permissions, menu);
                    showOption(R.id.openparent,menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.about, menu);
                    showOption(R.id.share,menu);
                    showOption(R.id.rename, menu);

                    File x = new File(list.get(adapter.getCheckedItemPositions().get(0))

                            .getDesc());

                    if (x.isDirectory()) {
                        hideOption(R.id.openwith,menu);
                        showOption(R.id.sethome, menu);
                        hideOption(R.id.share,menu);
                        hideOption(R.id.openmulti,menu);
                    } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk") || x.getName().toLowerCase().endsWith(".rar")|| x.getName().toLowerCase().endsWith(".tar")|| x.getName().toLowerCase().endsWith(".tar.gz")) {

                        showOption(R.id.ex, menu);

                        if(mainActivity.mReturnIntent)
                            if(Build.VERSION.SDK_INT>=16)
                                showOption(R.id.openmulti,menu);
                    }
                } else {
                    hideOption(R.id.openparent,menu);

                    if(mainActivity.mReturnIntent)
                        if(Build.VERSION.SDK_INT>=16)
                            showOption(R.id.openmulti,menu);
                    try {
                        for (int c : adapter.getCheckedItemPositions()) {
                            File x = new File(list.get(c).getDesc());
                            if (x.isDirectory()) {
                                hideOption(R.id.share, menu);
                                hideOption(R.id.openmulti,menu);
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

            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            computeScroll();
            ArrayList<Integer> plist = adapter.getCheckedItemPositions();
            switch (item.getItemId()) {
                case R.id.openmulti:
                    if(Build.VERSION.SDK_INT>=16){
                    Intent intentresult=new Intent();
                    ArrayList<Uri> resulturis=new ArrayList<Uri>();
                        for(int k:plist){
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
                    getActivity().finish();}
                    return true;
                case R.id.sethome:
                    int pos = plist.get(0);
                        home = list.get(pos).getDesc();
                    Toast.makeText(getActivity(),
                            utils.getString(getActivity(), R.string.newhomedirectory) + " " + list.get(pos).getTitle(),
                            Toast.LENGTH_LONG).show();
                    mainActivity.updatepaths();
                    mode.finish();
                    return true;
                case R.id.about:
                    String x;
                        x=list.get((plist.get(0))).getDesc();
                    utils.showProps(new File(x), ma,rootMode);
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
                        utils.deleteFiles(list,ma, plist);


                    return true;
                case R.id.share:
                    ArrayList<File> arrayList=new ArrayList<File>();
                        for(int i:plist){
                            arrayList.add(new File(list.get(i).getDesc()));
                        }
                    utils.shareFiles(arrayList,getActivity());
                    return true;
                case R.id.openparent:
                    loadlist(new File(list.get(plist.get(0)).getDesc()).getParentFile(),false);
                    return true;
                case R.id.all:
                    if (adapter.areAllChecked(current)) {
                        adapter.toggleChecked(false,current);
                    } else {
                        adapter.toggleChecked(true,current);
                    }
                    mode.invalidate();

                    return true;
                case R.id.rename:

                    final ActionMode m = mode;
                    final File f;
                      f= new File(list.get(
                            (plist.get(0))).getDesc());
                    View dialog = getActivity().getLayoutInflater().inflate(
                            R.layout.dialog, null);
                    MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                    final EditText edit = (EditText) dialog
                            .findViewById(R.id.newname);
                    edit.setText(f.getName());
                    a.customView(dialog, true);
                    if(theme1==1)
                        a.theme(Theme.DARK);
                    a.title(utils.getString(getActivity(), R.string.rename));

                    a.callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog materialDialog) {

                            boolean b = utils.rename(f, edit.getText()
                                    .toString(),rootMode);
                            m.finish();
                            updateList();
                            if (b) {
                                Toast.makeText(getActivity(),
                                        utils.getString(getActivity(), R.string.renamed),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(),
                                        utils.getString(getActivity(), R.string.renameerror),
                                        Toast.LENGTH_LONG).show();

                            }
                        }

                        @Override
                        public void onNegative(MaterialDialog materialDialog) {

                            materialDialog.cancel();
                        }
                    });
                    a.positiveText(R.string.save);
                    a.negativeText(R.string.cancel);
                    a.positiveColor(Color.parseColor(fabSkin));
                    a.negativeColor(Color.parseColor(fabSkin));
                    a.build().show();
                    mode.finish();
                    return true;
                case R.id.hide:
                        for (int i1 = 0; i1 < plist.size(); i1++) {
                            hide(list.get(plist.get(i1)).getDesc());
                        }
                    updateList();mode.finish();
                    return true;
                case R.id.book:
                      for (int i1 = 0; i1 < plist.size(); i1++) {
                            try {
                                sh.addS(new File(list.get(plist.get(i1)).getDesc()));
                            } catch (Exception e) {
                            }
                        }
                    mainActivity.updateDrawer();
                    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                    mode.finish();
                    return true;
                case R.id.ex:
                    Intent intent = new Intent(getActivity(), ExtractService.class);
                    intent.putExtra("zip", list.get((plist.get(0))).getDesc());
                    getActivity().startService(intent);
                    mode.finish();
                    return true;
                case R.id.cpy:
                    mainActivity.MOVE_PATH=null;
                    ArrayList<String> copies = new ArrayList<String>();
                    for (int i2 = 0; i2 < plist.size(); i2++) {
                    copies.add(list.get(plist.get(i2)).getDesc());
                    }
                    mainActivity.COPY_PATH = copies;
                    mainActivity.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.cut:
                    mainActivity.COPY_PATH=null;
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
                    copies1.add(list.get(plist.get(i4)).getDesc());}
                    utils.showNameDialog((MainActivity) getActivity(), copies1, current);
                    mode.finish();
                    return true;
                case R.id.openwith:
                    utils.openunknown(new File(list.get((plist.get(0))).getDesc()), getActivity(), true);
                    return true;
                case R.id.permissions:
                    utils.setPermissionsDialog(list.get(plist.get(0)),ma);
                    mode.finish();
                    return true;
                case R.id.addshortcut:
                    addShortcut(list.get(plist.get(0)));
                    mode.finish();return true;
                default:
                    return false;
            }
        }

        // called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            selection = false;
            if(!results)adapter.toggleChecked(false, current);
            else adapter.toggleChecked(false);
            mainActivity.setPagingEnabled(true);
            ObjectAnimator anim = ObjectAnimator.ofInt(getActivity().findViewById(R.id.buttonbarframe), "backgroundColor", res.getColor(R.color.holo_dark_action_mode), skin_color);
            anim.setDuration(0);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if(mainActivity.colourednavigation)window.setNavigationBarColor(mainActivity.skinStatusBar);
            }
        }
    };

    public void computeScroll() {
        View vi = listView.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        int index;if(islist)
        index= mLayoutManager.findFirstVisibleItemPosition();
        else index=mLayoutManagerGrid.findFirstVisibleItemPosition();
        Bundle b = new Bundle();
        b.putInt("index", index);
        b.putInt("top", top);
        scrolls.put(current, b);
    }

    public void goBack() {
        File f = new File(current);
        if (!results) {
            if(selection){
            System.out.println("adapter checked false");
                adapter.toggleChecked(false);}
            else{
            if(current.equals("/") || current.equals(home))
                mainActivity.exit();
                else if (utils.canGoBack(f) ) {
                loadlist(f.getParentFile(), true);
            }else mainActivity.exit();}
        } else {
            loadlist(f, true);
        }
    }
    public void goBackItemClick() {
        File f = new File(current);
        if (!results) {
            if(selection){
                adapter.toggleChecked(false);}
            else{
                if(current.equals("/"))
                    mainActivity.exit();
                else if (utils.canGoBack(f) ) {
                    loadlist(f.getParentFile(), true);
                }else mainActivity.exit();}
        } else {
            loadlist(f, true);
        }
    }
    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };
    public void updateList(){
        computeScroll();
        ic.cleanup();
        loadlist(new File(current), true);}

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
        if(history!=null)
            history.end();
        if(hidden!=null)
            hidden.end();
    }
    @Override
    public void onStart() {
        super.onStart();
        history = new HistoryManager(getActivity(), "Table1");
    }

    public ArrayList<Layoutelements> addToSmb(SmbFile[] mFile) throws SmbException {
        Log.e("Connected",mFile.length+"");
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        smbFiles=new ArrayList<SmbFile>();
        for (int i = 0; i < mFile.length; i++) {
            smbFiles.add(mFile[i]);
                if (mFile[i].isDirectory()) {
                    a.add(new Layoutelements(folder,mFile[i].getName(), mFile[i].getPath(),"","","",false,"",true));
                } else {
                    try {
                        a.add(new Layoutelements(Icons.loadMimeIcon(getActivity(), mFile[i].getPath(),!islist),mFile[i].getName(), mFile[i].getPath(),"","",utils.readableFileSize(mFile[i].length()), false, "",false));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }}
            }
        Log.e("Connected",a.size()+"Size");
        return a;
    }
    public ArrayList<Layoutelements> addTo(ArrayList<String[]> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        for (int i = 0; i < mFile.size(); i++) {
            String[] ele=mFile.get(i);
            File f=new File(ele[0]);
            String size="";
            if(!hiddenfiles.contains(ele[0])){
                if (isDirectory(ele)) {
                    if(!ele[5].trim().equals("") && !ele[5].toLowerCase().trim().equals("null"))size=ele[5]+" "+itemsstring;
                    else size="";
                    a.add(utils.newElement(folder, f.getPath(),mFile.get(i)[2],mFile.get(i)[1],size,true,false,ele[4]));

                } else {

                    try {
                        if(!ele[5].trim().equals("") && !ele[5].toLowerCase().trim().equals("null"))size=utils.readableFileSize(Long.parseLong(ele[5]));
                        else size="";
                    } catch (NumberFormatException e) {
                        //e.printStackTrace();
                    }
                    try {
                        a.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f.getPath(),!islist), f.getPath(),mFile.get(i)[2],mFile.get(i)[1],size,false,false,ele[4]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }}
            }
        }
        return a;
    }

    public boolean isDirectory(String[] path){
        if(rootMode)
            if(path[1].length()!=0)return new File(path[0]).isDirectory();
               else return path[3].equals("-1");
        else
            return new File(path[0]).isDirectory();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    void addSmbToSavedInstance(Bundle savedInstance, String user, String pass, ArrayList<SmbFile> list, boolean anonym) {
        savedInstance.putBoolean("SmbMode", smbMode);
        savedInstance.putString("SmbPath",smbPath);
        if (!anonym) {
            savedInstance.putString("SmbUser", user);
            savedInstance.putString("SmbPass", pass);
            savedInstance.putBoolean("SmbAnonym", false);
        } else savedInstance.putBoolean("SmbAnonym", true);
        ArrayList<String> stringArrayList = new ArrayList<>();
        for (SmbFile smbFile : list) {
            stringArrayList.add(smbFile.getPath());
        }
        savedInstance.putStringArrayList("SmbFileList", stringArrayList);
    }

    void retreiveSmbFromSavedInstance(Bundle savedInstance) {
        smbMode = savedInstance.getBoolean("SmbMode");
        if (!smbMode) return;
        smbPath=savedInstance.getString("SmbPath");
        SmbAnonym = savedInstance.getBoolean("SmbAnonym");
        ArrayList<String> stringArrayList = savedInstance.getStringArrayList("SmbFileList");
        smbFiles=new ArrayList<SmbFile>();
        try {
            if(SmbAnonym) {
             for(String a:stringArrayList)
                 smbFiles.add( new SmbFile(a,NtlmPasswordAuthentication.ANONYMOUS));
            }else{
                NtlmPasswordAuthentication auth1 = new NtlmPasswordAuthentication(
                        null,smbUser= savedInstance.getString("SmbUser"),smbPass=savedInstance.getString("SmbPass"));
                for(String a:stringArrayList)
                    smbFiles.add( new SmbFile(a,auth1));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
       public SmbFile connectingWithSmbServer(String[] auth,boolean anonym) {
        try {
            String yourPeerIP=auth[0];
            String path = "smb://" + yourPeerIP;
            smbPath=path;
            SmbFile smbFile;
            if(anonym){
                SmbAnonym=true;
               smbFile = new SmbFile(path,NtlmPasswordAuthentication.ANONYMOUS);
            }
            else {
                SmbAnonym=false;
                smbUser=auth[1];
                smbPass=auth[2];
                NtlmPasswordAuthentication auth1 = new NtlmPasswordAuthentication(
                        null, auth[1], auth[2]);
                 smbFile = new SmbFile(path, auth1);
            }return smbFile;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Connected", e.getMessage());
            return null;
        }
    }
    public void updatehiddenfiles(){
        hiddenfiles=hidden.readTable();
    }
    public void initPoppyViewListeners(View poppy){
/*
        ImageView imageView = ((ImageView)poppy.findViewById(R.id.overflow));
        showPopup(imageView);
        final ImageView homebutton=(ImageView)poppy.findViewById(R.id.home);
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                home();if(mActionMode!=null){mActionMode.finish();}
            }
        });
        ImageView imageView1 = ((ImageView)poppy.findViewById(R.id.search));
        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search();
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }
        });*/
    }

    public void hide(String path){
        hidden.addPath(path);
        hiddenfiles=hidden.readTable();
        if(new File(path).isDirectory()){
            File f1 = new File(path + "/" + ".nomedia");
            if (!f1.exists()) {
                try {
                    boolean b = f1.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }    }
            utils.scanFile(path,getActivity());}

    }

    public  void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        Intent i=new Intent(getActivity(),MainActivity.class);
        i.putExtra("restart",true);
        i.setAction(Intent.ACTION_MAIN);
        activity.startActivity(i);
    }

    public String getSelectionColor(){

        String[] colors = new String[]{
                "#F44336","#74e84e40",
                "#e91e63","#74ec407a",
                "#9c27b0","#74ab47bc",
                "#673ab7","#747e57c2",
                "#3f51b5","#745c6bc0",
                "#2196F3","#74738ffe",
                "#03A9F4","#7429b6f6",
                "#00BCD4","#7426c6da",
                "#009688","#7426a69a",
                "#4CAF50","#742baf2b",
                "#8bc34a","#749ccc65",
                "#FFC107","#74ffca28",
                "#FF9800","#74ffa726",
                "#FF5722","#74ff7043",
                "#795548","#748d6e63",
                "#212121","#79bdbdbd",
                "#607d8b","#7478909c",
                "#004d40","#740E5D50"
        };
        return colors[ Arrays.asList(colors).indexOf(skin)+1];
    }

    public float[] calculatefilter(float[] values){
        float[] src= {

                values[0], 0, 0, 0, 0,
                0, values[1], 0, 0, 0,
                0, 0,  values[2],0, 0,
                0, 0, 0, 1, 0
        };
        return src;
    }
    public float[] calculatevalues(String color){
        int c=Color.parseColor(color);
        float r=(float)Color.red(c)/255;
        float g=(float)Color.green(c)/255;
        float b=(float)Color.blue(c)/255;
        return new float[]{r,g,b};
    }private void addShortcut(Layoutelements path) {
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
                        R.drawable.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getActivity().sendBroadcast(addIntent);
    }
}