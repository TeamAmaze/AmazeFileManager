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
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.StringDef;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Property;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.MyAdapter;
import com.amaze.filemanager.adapters.TabSpinnerAdapter;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.services.asynctasks.LoadSearchList;
import com.amaze.filemanager.services.asynctasks.SearchTask;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HistoryManager;
import com.amaze.filemanager.utils.IconHolder;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.Shortcuts;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


public class Main extends android.support.v4.app.Fragment {
    public File[] file;
    public ArrayList<Layoutelements> list, slist;
    public MyAdapter adapter;
    public Futils utils;
    public boolean selection;
    public boolean results = false;
    public ActionMode mActionMode;
    public SharedPreferences Sp;
    public Drawable folder,apk,darkimage,darkvideo;
    Resources res;
    public LinearLayout buttons;
    public int sortby, dsort, asc;
    public int uimode;
    public String home, current = Environment.getExternalStorageDirectory().getPath();
    Shortcuts sh;
    HashMap<String, Bundle> scrolls = new HashMap<String, Bundle>();
    Main ma = this;
    public HistoryManager history,hidden;
    IconUtils icons;
    HorizontalScrollView scroll,scroll1;
    public boolean rootMode,showHidden,circularImages,showPermissions,showSize,showLastModified;
    View footerView;
    public LinearLayout pathbar;
    private TextView textView;
    private ImageButton ib;
    public CountDownTimer timer;
    private View rootView;
    public ListView listView;
    public GridView gridView;
    public Boolean gobackitem,aBoolean,showThumbs,coloriseIcons;
    public IconHolder ic;
    private List<Tab> content;
    private ArrayList<String> list1;
    public MainActivity mainActivity;
    public String skin;
    public int skinselection;
    public int theme;
    public int theme1;
    public float[] color;
    public ColorMatrixColorFilter colorMatrixColorFilter;
    Animation animation,animation1;
    public String year,goback;
    ArrayList<String> hiddenfiles;
    private FloatingActionButton floatingActionButton;
    String Intentpath;
    String tag;
    private TranslateAnimation anim;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 //       tag=getArguments().getString("tag");
        System.out.println(tag+"created");
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        skin = Sp.getString("skin_color", "#03A9F4");
        sh = new Shortcuts(getActivity());
        aBoolean = Sp.getBoolean("view", true);
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        year=(""+calendar.get(Calendar.YEAR)).substring(2,4);
        theme=Integer.parseInt(Sp.getString("theme","0"));
        theme1 = theme;
        if (theme == 2) {
            if(hour<=6 || hour>=18) {
                theme1 = 1;
            } else
                theme1 = 0;
        }
        mainActivity=(MainActivity)getActivity();
        showPermissions=Sp.getBoolean("showPermissions",false);
        showSize=Sp.getBoolean("showFileSize",false);
        gobackitem=Sp.getBoolean("goBack_checkbox", true);
        circularImages=Sp.getBoolean("circularimages",true);
        showLastModified=Sp.getBoolean("showLastModified",true);
        icons = new IconUtils(Sp, getActivity());
        timer=new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                crossfadeInverse();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println(tag+"viewcreated");
        rootView = inflater.inflate(R.layout.main_frag, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView);
        gridView = (GridView) rootView.findViewById(R.id.gridView);
        if(getArguments()!=null)
        Intentpath=getArguments().getString("path");
        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.load_list_anim);
        animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_newtab);

        buttons = (LinearLayout) rootView.findViewById(R.id.buttons);
        pathbar = (LinearLayout) rootView.findViewById(R.id.pathbar);
        textView = (TextView) rootView.findViewById(R.id.fullpath);



        showThumbs=Sp.getBoolean("showThumbs", true);
        ic=new IconHolder(getActivity(),showThumbs,!aBoolean);
        res = getResources();
        goback=res.getString(R.string.goback);
        apk=res.getDrawable(R.drawable.ic_doc_apk_grid);
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        if(theme1==1) {
            rootView.findViewById(R.id.main_frag).setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        } else {
            //listView.setBackgroundColor(getResources().getColor(android.R.color.background_light));
            if (uimode==0 && aBoolean) {

                rootView.findViewById(R.id.main_frag).setBackgroundColor(getResources().getColor(android.R.color.background_light));
            }
        }
        if (aBoolean) {
            listView.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        } else {
            int columns=Integer.parseInt(Sp.getString("columns","3"));
            gridView.setNumColumns(columns);
            listView.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        System.out.println(tag+"activitycreated");
        getActivity().findViewById(R.id.buttonbarframe).setVisibility(View.VISIBLE);

        /*Animation animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_newtab);
        getActivity().findViewById(R.id.fab).setAnimation(animation1);*/

        list1 = new ArrayList<String>();


        //getActivity().findViewById(R.id.search).setVisibility(View.VISIBLE);
        //getActivity().findViewById(R.id.action_overflow).setVisibility(View.VISIBLE);
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
        if(aBoolean){
            folder = res.getDrawable(R.drawable.ic_grid_folder_new);}
        else{folder = res.getDrawable(R.drawable.ic_grid_folder1);}
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);
        getSortModes();
        darkimage=res.getDrawable(R.drawable.ic_doc_image_dark);
        darkvideo=res.getDrawable(R.drawable.ic_doc_video_dark);
        home = Sp.getString("home", mainActivity.val.get(0));
        this.setRetainInstance(false);
        File
            f=new File(Sp.getString("current",home));

        rootView.findViewById(R.id.buttonbarframe).setBackgroundColor(Color.parseColor(skin));
        initiatebbar();

        scroll = (HorizontalScrollView) getActivity().findViewById(R.id.scroll);
        scroll1 = (HorizontalScrollView) getActivity().findViewById(R.id.scroll1);
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        if (uimode == 1) {
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (5 * scale + 0.5f);

            if (aBoolean) {

                listView.setPadding(dpAsPixels, 0, dpAsPixels, 0);
                listView.setDivider(null);
                listView.setDividerHeight(dpAsPixels);
            } else {

                gridView.setPadding(dpAsPixels, 0, dpAsPixels, 0);
            }
        }
        footerView=getActivity().getLayoutInflater().inflate(R.layout.divider, null);
        if (aBoolean) {
            listView.addFooterView(footerView);
            listView.setFastScrollEnabled(true);
        } else {

            gridView.setFastScrollEnabled(true);
        }
        if (savedInstanceState == null){
            if(Intentpath!=null){File file1=new File(Intentpath);
            if(file1.isDirectory())loadlist(file1,false);
                else {utils.openFile(file1,mainActivity);
            loadlist(f,false);}
            }else
            loadlist(f, false);}
        else {
            Bundle b = new Bundle();
            String cur = savedInstanceState.getString("current");
           if(cur!=null){ b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);
            list = savedInstanceState.getParcelableArrayList("list");
            createViews(list, true, new File(cur));
            updatePath(cur);
            if (savedInstanceState.getBoolean("selection")) {

                for (int i : savedInstanceState.getIntegerArrayList("position")) {
                    adapter.toggleChecked(i);
                }
            }}
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int index;
        View vi;
        if(listView!=null){
            if (aBoolean) {

                index = listView.getFirstVisiblePosition();
                vi = listView.getChildAt(0);
            } else {
                index = gridView.getFirstVisiblePosition();
                vi = gridView.getChildAt(0);
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
        }}

    public void home() {
        ma.loadlist(new File(ma.home), false);
    }



    public void onListItemClicked(int position, View v) {
        if (results) {
            String path = slist.get(position).getDesc();


            final File f = new File(path);
            if (slist.get(position).isDirectory(rootMode)) {

                loadlist(f, false);
                results = false;
            } else {
                if (mainActivity.mReturnIntent) {
                    returnIntentResults(f);
                } else
                utils.openFile(f, (MainActivity) getActivity());
            }
        } else if (selection == true) {
            if(!list.get(position).getSize().equals(goback)){
                adapter.toggleChecked(position);;
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

                if (l.isDirectory(rootMode)) {

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

                goBack();

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
        new LoadList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (f));

        try {
            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.load_list_anim);
        } catch (Exception e) {
            e.printStackTrace();
        }

        listView.setAnimation(animation);
        gridView.setAnimation(animation);

        // Spinner

    }

    @SuppressWarnings("unchecked")
    public void loadsearchlist(ArrayList<String[]> f) {

        new LoadSearchList(ma).execute(f);

    }


    public void createViews(ArrayList<Layoutelements> bitmap, boolean back, File f) {
        try {
            if (bitmap != null) {
                TextView footerText = (TextView) footerView.findViewById(R.id.footerText);
                if (bitmap.size() == 0) {
                    footerText.setText(res.getString(R.string.nofiles));
                } else {
                    footerText.setText(res.getString(R.string.tapnhold));
                }if(gobackitem)
                if (!f.getPath().equals("/")) {
                    if (bitmap.size() == 0 || !bitmap.get(0).getSize().equals(goback))
                        bitmap.add(0, utils.newElement(res.getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha), "..", "", "", goback, "", true));
                }
                adapter = new MyAdapter(getActivity(), R.layout.rowlayout,
                        bitmap, ma);
                try {

                    if (aBoolean) {
                        listView.setAdapter(adapter);
                    } else {
                        gridView.setAdapter(adapter);
                    }


                    results = false;
                    current = f.getPath();
                    if (back) {
                        if (scrolls.containsKey(current)) {
                            Bundle b = scrolls.get(current);

                            //listView.setSelectionFromTop(b.getInt("index"), b.getInt("top"));
                        }
                    }
                    floatingActionButton.show();

                    mainActivity.updatepaths();
                    mainActivity.updatespinner();
                       updatePath(current);
                            if (buttons.getVisibility() == View.VISIBLE) bbar(current);

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
            mode.setCustomView(v);
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
            //hideOption(R.id.setringtone,menu);
            mode.setTitle(utils.getString(getActivity(), R.string.select));
            /*if(Build.VERSION.SDK_INT<19)
                getActivity().findViewById(R.id.action_bar).setVisibility(View.GONE);*/
           // rootView.findViewById(R.id.buttonbarframe).setBackgroundColor(res.getColor(R.color.toolbar_cab));
            ObjectAnimator anim = ObjectAnimator.ofInt(rootView.findViewById(R.id.buttonbarframe), "backgroundColor", Color.parseColor(skin), res.getColor(R.color.toolbar_cab));
            anim.setDuration(200);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(res.getColor(android.R.color.black));
            }
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = adapter.getCheckedItemPositions();
            ((TextView) v.findViewById(R.id.item_count)).setText(positions.size() + "");
            //tv.setText(positions.size());
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
                } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk") || x.getName().toLowerCase().endsWith(".rar")|| x.getName().toLowerCase().endsWith(".tar")|| x.getName().toLowerCase().endsWith(".tar.gz")) {

                    showOption(R.id.ex, menu);

                }
            } else {
                try {
                    for (int c : adapter.getCheckedItemPositions()) {
                        File x = new File(list.get(c).getDesc());
                        if (x.isDirectory()) {
                            hideOption(R.id.share, menu);
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

            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            computeScroll();
            ArrayList<Integer> plist = adapter.getCheckedItemPositions();
            switch (item.getItemId()) {
                case R.id.sethome:
                    int pos = plist.get(0);
                    if(results)
                        home = slist.get(pos).getDesc();
                    else
                        home = list.get(pos).getDesc();
                    Toast.makeText(getActivity(),
                            utils.getString(getActivity(), R.string.newhomedirectory) + " " + list.get(pos).getTitle(),
                            Toast.LENGTH_LONG).show();
                    Sp.edit().putString("home", list.get(pos).getDesc()).apply();

                    mode.finish();
                    return true;
                case R.id.about:
                    String x;
                    if(results)
                        x=slist.get((plist.get(0))).getDesc();
                    else
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
                    if(results)
                        utils.deleteFiles(slist,ma,plist);
                    else
                        utils.deleteFiles(list,ma, plist);


                    return true;
                case R.id.share:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    ArrayList<Uri> uris=new ArrayList<Uri>();
                    if(results){
                        for(int i:plist){
                            uris.add(Uri.fromFile(new File(slist.get(i).getDesc())));
                        }
                    }
                    else{
                        for(int i:plist){
                            uris.add(Uri.fromFile(new File(list.get(i).getDesc())));
                        }}
                    sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    sendIntent.setType("*/*");
                    startActivity(sendIntent);
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
                    if(results)
                        f=new File(slist.get(
                                (plist.get(0))).getDesc());
                    else     f= new File(list.get(
                            (plist.get(0))).getDesc());
                    View dialog = getActivity().getLayoutInflater().inflate(
                            R.layout.dialog, null);
                    MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                    final EditText edit = (EditText) dialog
                            .findViewById(R.id.newname);
                    edit.setText(f.getName());
                    a.customView(dialog);
                    if(theme1==1)
                        a.theme(Theme.DARK);
                    a.title(utils.getString(getActivity(), R.string.rename));

                    a.callback(new MaterialDialog.Callback() {
                        @Override
                        public void onPositive(MaterialDialog materialDialog) {

                            boolean b = utils.rename(f, edit.getText()
                                    .toString());
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
                    a.positiveColor(Color.parseColor(skin));
                    a.negativeColor(Color.parseColor(skin));
                    a.build().show();
                    mode.finish();
                    return true;
                case R.id.hide:
                    if(results){for (int i1 = 0; i1 < plist.size(); i1++) {
                        hide(slist.get(plist.get(i1)).getDesc());
                    }}
                    else{
                        for (int i1 = 0; i1 < plist.size(); i1++) {
                            hide(list.get(plist.get(i1)).getDesc());
                        }}
                    updateList();mode.finish();
                    return true;
                case R.id.book:
                    if(results)
                    {for (int i1 = 0; i1 < plist.size(); i1++) {
                        try {
                            sh.addS(new File(slist.get(plist.get(i1)).getDesc()));

                        } catch (Exception e) {
                        }
                    }}else{
                        for (int i1 = 0; i1 < plist.size(); i1++) {
                            try {
                                sh.addS(new File(list.get(plist.get(i1)).getDesc()));

                            } catch (Exception e) {
                            }
                        }}
                    mainActivity.updateDrawer();
                    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                    mode.finish();
                    return true;
                case R.id.ex:
                    Intent intent = new Intent(getActivity(), ExtractService.class);

                    if(results)intent.putExtra("zip", slist.get(
                            (plist.get(0))).getDesc());
                    else intent.putExtra("zip", list.get(
                            (plist.get(0))).getDesc());

                    getActivity().startService(intent);
                    mode.finish();
                    return true;
                case R.id.cpy:
                    mainActivity.MOVE_PATH=null;
                    ArrayList<String> copies = new ArrayList<String>();
                    if(results){
                        for (int i2 = 0; i2 < plist.size(); i2++) {
                            copies.add(slist.get(plist.get(i2)).getDesc());
                        }
                    }
                    else{
                        for (int i2 = 0; i2 < plist.size(); i2++) {
                            copies.add(list.get(plist.get(i2)).getDesc());
                        }}
                    mainActivity.COPY_PATH = copies;
                    mainActivity.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.cut:
                    mainActivity.COPY_PATH=null;
                    ArrayList<String> copie = new ArrayList<String>();
                    if(results){
                        for (int i3 = 0; i3 < plist.size(); i3++) {
                            copie.add(slist.get(plist.get(i3)).getDesc());
                        }
                    }
                    else{
                        for (int i3 = 0; i3 < plist.size(); i3++) {
                            copie.add(list.get(plist.get(i3)).getDesc());
                        }}
                    mainActivity.MOVE_PATH = copie;
                    mainActivity.supportInvalidateOptionsMenu();
                    mode.finish();
                    return true;
                case R.id.compress:
                    ArrayList<String> copies1 = new ArrayList<String>();
                    if(results){
                        for (int i4 = 0; i4 < plist.size(); i4++) {
                            copies1.add(slist.get(plist.get(i4)).getDesc());
                        }
                    }
                    else {
                        for (int i4 = 0; i4 < plist.size(); i4++) {
                            copies1.add(list.get(plist.get(i4)).getDesc());
                        }}
                    utils.showNameDialog((MainActivity) getActivity(), copies1, current);
                    mode.finish();
                    return true;
                case R.id.openwith:
                    if (results)utils.openunknown(new File(slist.get(
                            (plist.get(0))).getDesc()), getActivity());
                    else
                        utils.openunknown(new File(list.get(
                                (plist.get(0))).getDesc()), getActivity());

                    return true;
                case R.id.permissions:
                    if(results)
                        utils.setPermissionsDialog(slist.get(plist.get(0)),ma);
                    else
                        utils.setPermissionsDialog(list.get(plist.get(0)),ma);
                    mode.finish();
                    return true;
                case R.id.addshortcut:
                    if(results)
                        addShortcut(slist.get(plist.get(0)));
                    else addShortcut(list.get(plist.get(0)));
                    mode.finish();return true;
                default:
                    return false;
            }
        }

        // called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            selection = false;
            adapter.toggleChecked(false, current);
            ObjectAnimator anim = ObjectAnimator.ofInt(rootView.findViewById(R.id.buttonbarframe), "backgroundColor", res.getColor(R.color.toolbar_cab), Color.parseColor(skin));
            anim.setDuration(50);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(Color.parseColor(mainActivity.getStatusColor()));
            }
        }
    };
    public void updatePath(String text){
        File f=new File(text);
        String used = utils.readableFileSize(f.getTotalSpace()-f.getFreeSpace());
        String free = utils.readableFileSize(f.getFreeSpace());
        TextView textView = (TextView)pathbar.findViewById(R.id.pathname);
        textView.setText(res.getString(R.string.used)+" " + used +" "+ res.getString(R.string.free)+" " + free);

        TextView bapath=(TextView)pathbar.findViewById(R.id.fullpath);
        bapath.setText(f.getPath());
        bapath.setAllCaps(true);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(View.FOCUS_RIGHT);
                scroll1.fullScroll(View.FOCUS_RIGHT);
            }
        });

    }
    public void bbar(String text) {
        try {
            buttons.removeAllViews();
            Drawable arrow=getResources().getDrawable(R.drawable.abc_ic_ab_back_holo_dark);
            Bundle b = utils.getPaths(text, getActivity());
            ArrayList<String> names = b.getStringArrayList("names");
            ArrayList<String> rnames = new ArrayList<String>();

            for (int i = names.size() - 1; i >= 0; i--) {
                rnames.add(names.get(i));
            }

            ArrayList<String> paths = b.getStringArrayList("paths");
            final ArrayList<String> rpaths = new ArrayList<String>();

            for (int i = paths.size() - 1; i >= 0; i--) {
                rpaths.add(paths.get(i));
            }
            for (int i = 0; i < names.size(); i++) {
                ImageView v=new ImageView(getActivity());
                v.setImageDrawable(arrow);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity= Gravity.CENTER_VERTICAL;
                v.setLayoutParams(params);
                final int index = i;
                if (rpaths.get(i).equals("/")) {
                    ib = new ImageButton(getActivity());
                    ib.setImageDrawable(icons.getRootDrawable());
                    ib.setBackgroundColor(Color.parseColor(skin));
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            ((Main) mainActivity.getFragment().getTab()).loadlist(new File("/"), false);
                            timer.cancel();
                            timer.start();
                        }
                    });

                    buttons.addView(ib);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                } else if (rpaths.get(i).equals(Environment.getExternalStorageDirectory().getPath())) {
                    ib = new ImageButton(getActivity());
                    ib.setImageDrawable(icons.getSdDrawable());
                    ib.setBackgroundColor(Color.parseColor(skin));
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            ((Main)   mainActivity.getFragment().getTab()).loadlist(new File("/"), false);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    buttons.addView(ib);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                } else {
                    Button button = new Button(getActivity());
                    button.setText(rnames.get(index));
                    button.setTextColor(getResources().getColor(android.R.color.white));
                    button.setTextSize(13);
                    button.setBackgroundResource(0);
                    button.setOnClickListener(new Button.OnClickListener() {

                        public void onClick(View p1) {
                            ((Main) mainActivity.getFragment().getTab()).loadlist(new File("/"), false);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    button.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {

                            File file1 = new File(rpaths.get(index));
                            copyToClipboard(getActivity(), file1.getPath());
                            Toast.makeText(getActivity(), res.getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });

                    buttons.addView(button);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                }
            }
            File f=new File(text);

            TextView textView = (TextView)pathbar.findViewById(R.id.pathname);
            String used = utils.readableFileSize(f.getTotalSpace()-f.getFreeSpace());
            String free = utils.readableFileSize(f.getFreeSpace());
            textView.setText(res.getString(R.string.used)+" " + used +" "+ res.getString(R.string.free)+" " + free);

            TextView bapath=(TextView)pathbar.findViewById(R.id.fullpath);
            bapath.setAllCaps(true);
            bapath.setText(f.getPath());
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(View.FOCUS_RIGHT);
                    scroll1.fullScroll(View.FOCUS_RIGHT);
                }
            });

            if(buttons.getVisibility()==View.VISIBLE){timer.cancel();timer.start();}
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("button view not available");
        }

    }

    public boolean copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("Path copied to clipboard", text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public void computeScroll() {
        int index = listView.getFirstVisiblePosition();
        View vi = listView.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        Bundle b = new Bundle();
        b.putInt("index", index);
        b.putInt("top", top);
        scrolls.put(current, b);
    }

    public void goBack() {
        File f = new File(current);
        if (!results) {
            if (utils.canGoBack(f) && adapter.getCheckedItemPositions().size()==0) {
                loadlist(f.getParentFile(), true);
            } else {
                for (int i : adapter.getCheckedItemPositions()) {
                    adapter.toggleChecked(i);
                }
            }
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
        System.out.println(tag+"resumed");
        floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        floatingActionButton.attachToListView(listView);
        floatingActionButton.attachToListView(gridView);
        floatingActionButton.setColorNormal(Color.parseColor(skin));
        floatingActionButton.setColorPressed(Color.parseColor(skin));
        floatingActionButton.setAnimation(animation1);
        //floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.show(true);

        (getActivity()).registerReceiver(receiver2, new IntentFilter("loadlist"));
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println(tag+"paused");
        (getActivity()).unregisterReceiver(receiver2);
    }
    @Override
    public void onStop() {
        super.onStop();
        System.out.println(tag+"stopped");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        System.out.println(tag+"destroyview");
    }
    @Override
    public void onStart() {
        super.onStart();
        System.out.println(tag+"start");
        history = new HistoryManager(getActivity(), "Table1");
    }

    public ArrayList<Layoutelements> addTo(ArrayList<String[]> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        for (int i = 0; i < mFile.size(); i++) {
            File f=new File(mFile.get(i)[0]);
            if(!hiddenfiles.contains(mFile.get(i)[0])){
                if (isDirectory(mFile.get(i))) {
                    a.add(utils.newElement(folder, f.getPath(),mFile.get(i)[2],mFile.get(i)[1],utils.count(f,res,showSize),mFile.get(i)[3],false));

                } else {
                    try {
                        a.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f.getPath(),!aBoolean), f.getPath(),mFile.get(i)[2],mFile.get(i)[1],utils.getSize(mFile.get(i),showSize),mFile.get(i)[3],false));
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
        if(history!=null)
            history.end();    System.out.println(tag+"destroy");

        //hidden.end();

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
    public void crossfade() {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        buttons.setAlpha(0f);
        buttons.setVisibility(View.VISIBLE);



        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        buttons.animate()
                .alpha(1f)
                .setDuration(100)
                .setListener(null);
        pathbar.animate()
                .alpha(0f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        pathbar.setVisibility(View.GONE);
                    }
                });
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)

    }public  void restartPC(final Activity activity) {
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
    }private void crossfadeInverse() {


        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        pathbar.setAlpha(0f);
        pathbar.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        pathbar.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);
        buttons.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        buttons.setVisibility(View.GONE);
                    }
                });
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
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

    public void initiatebbar() {
        LinearLayout pathbar = (LinearLayout) rootView.findViewById(R.id.pathbar);
        TextView textView = (TextView) rootView.findViewById(R.id.fullpath);

        pathbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bbar(current);
                crossfade();
                timer.cancel();
                timer.start();
            }
        });
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bbar(current);
                crossfade();
                timer.cancel();
                timer.start();
            }
        });

    }
}