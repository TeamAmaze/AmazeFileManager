package com.amaze.filemanager.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.faizmalkani.floatingactionbutton.FloatingActionButton;
import com.fourmob.poppyview.PoppyViewHelper;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class Main extends android.support.v4.app.Fragment {
    public File[] file;
    public ArrayList<Layoutelements> list, slist;
    public MyAdapter adapter;
    public Futils utils;
    public boolean selection;
    public boolean results = false;
    public ActionMode mActionMode;
    public SharedPreferences Sp;
    Drawable folder, apk, unknown, archive, text;
    Resources res;
    public LinearLayout buttons;
    public int sortby, dsort, asc;
    public int uimode;
    public String home, current = Environment.getExternalStorageDirectory().getPath();
    Shortcuts sh = new Shortcuts();
    HashMap<String, Bundle> scrolls = new HashMap<String, Bundle>();
    Main ma = this;
    public HistoryManager history;
    IconUtils icons;
    HorizontalScrollView scroll,scroll1;
    public boolean rootMode, mountSystem,showHidden,showPermissions,showSize,showLastModified;
    View footerView, poppyView;
    ImageButton paste;
    private PoppyViewHelper mPoppyViewHelper;
    public LinearLayout pathbar;
    private ImageButton ib;
    CountDownTimer timer;
    private View rootView;
    public ListView listView;
    public GridView gridView;
    private SharedPreferences sharedPreferences;
    public Boolean aBoolean,showThumbs;
    public IconHolder ic;
    private TabHandler tabHandler;
    private List<Tab> content;
    private ArrayList<String> list1;
    private ArrayAdapter<String> adapter1;
    private MainActivity mainActivity;
    public String skin;
    public int theme;
    private FloatingActionButton fab;
    private TabSpinnerAdapter tabSpinnerAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        aBoolean = Sp.getBoolean("view", true);
        theme=Integer.parseInt(Sp.getString("theme","0"));
        mainActivity=(MainActivity)getActivity();
        tabHandler = new TabHandler(getActivity(), null, null, 1);
        showPermissions=Sp.getBoolean("showPermissions",false);
        showSize=Sp.getBoolean("showFileSize",true);
        showLastModified=Sp.getBoolean("showLastModified",true);
        icons = new IconUtils(Sp, getActivity());
        timer=new CountDownTimer(2000,1000) {
            @Override
            public void onTick(long l) {}
            @Override
            public void onFinish() {
                crossfadeInverse();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.main_frag, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView);
        gridView = (GridView) rootView.findViewById(R.id.gridView);
        showThumbs=Sp.getBoolean("showThumbs",true);
        ic=new IconHolder(getActivity(),showThumbs,!aBoolean);
        res = getResources();
        if(theme==1) {
            rootView.findViewById(R.id.main_frag).setBackgroundColor(getResources().getColor(android.R.color.background_dark));
            ((ImageView)getActivity().findViewById(R.id.shadow)).setImageDrawable(res.getDrawable(R.drawable.shadow_dark));
        }if (aBoolean) {
            listView.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        getActivity().findViewById(R.id.buttonbarframe).setVisibility(View.VISIBLE);

        content = tabHandler.getAllTabs();
        list1 = new ArrayList<String>();

        for (Tab tab : content) {
            //adapter1.add(tab.getLabel());
            list1.add(tab.getLabel());
        }
        tabSpinnerAdapter = new TabSpinnerAdapter(getActivity(), R.layout.spinner_layout, list1, getActivity().getSupportFragmentManager(), mainActivity.tabsSpinner);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        ImageButton overflow=(ImageButton)getActivity().findViewById(R.id.action_overflow);
        overflow.setVisibility(View.VISIBLE);
        (overflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });
        fab=(FloatingActionButton)getActivity().findViewById(R.id.fabbutton);
        (fab).setDrawable(icons.getNewDrawable());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                add();
            }
        });
        getActivity().findViewById(R.id.fabbutton).setVisibility(View.VISIBLE);

        utils = new Futils();

        mPoppyViewHelper = new PoppyViewHelper(getActivity());
        skin = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("skin_color", "#673ab7");

        if (aBoolean) {
            poppyView = mPoppyViewHelper.createPoppyViewOnListView(R.id.listView, R.layout.pooppybar);
            LinearLayout linearLayout = (LinearLayout) poppyView.findViewById(R.id.linearLayout);
            linearLayout.setBackgroundColor(Color.parseColor(skin));
        } else {
            poppyView = mPoppyViewHelper.createPoppyViewOnGridView(R.id.gridView, R.layout.pooppybar,null);
            LinearLayout linearLayout = (LinearLayout) poppyView.findViewById(R.id.linearLayout);
            linearLayout.setBackgroundColor(Color.parseColor(skin));
        }
        initPoppyViewListeners(poppyView);
        history = new HistoryManager(getActivity(), "Table1");
        rootMode = Sp.getBoolean("rootmode", false);
        mountSystem = Sp.getBoolean("mountsystem", false);
        showHidden=Sp.getBoolean("showHidden",true);
        if(aBoolean){
        folder = res.getDrawable(R.drawable.ic_grid_folder_new);

        apk = res.getDrawable(R.drawable.ic_doc_apk);
        unknown = res.getDrawable(R.drawable.ic_doc_generic_am);
        archive = res.getDrawable(R.drawable.archive_blue);
        text = res.getDrawable(R.drawable.ic_doc_text_am);}
        else{folder = res.getDrawable(R.drawable.ic_grid_folder1);

            apk = res.getDrawable(R.drawable.ic_doc_apk_grid);
            unknown = res.getDrawable(R.drawable.ic_doc_generic_am_grid);
            archive = res.getDrawable(R.drawable.archive_blue);
            text = res.getDrawable(R.drawable.ic_doc_text_am_grid);}
        getSortModes();
        home = Sp.getString("home", System.getenv("EXTERNAL_STORAGE"));
        this.setRetainInstance(false);
        File f=new File(Sp.getString("current",home));

        buttons = (LinearLayout) getActivity().findViewById(R.id.buttons);
        pathbar = (LinearLayout) getActivity().findViewById(R.id.pathbar);

        pathbar.setBackgroundColor(Color.parseColor(skin));

        pathbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                crossfade();
                timer.cancel();
                timer.start();
            }
        });
        scroll = (HorizontalScrollView) getActivity().findViewById(R.id.scroll);
        scroll1 = (HorizontalScrollView) getActivity().findViewById(R.id.scroll1);
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        if (uimode == 1) {
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (5 * scale + 0.5f);

            aBoolean = sharedPreferences.getBoolean("view", true);
            if (aBoolean) {

                listView.setPadding(dpAsPixels, 0, dpAsPixels, 0);
                listView.setDivider(null);
                listView.setDividerHeight(dpAsPixels);
            } else {

                gridView.setPadding(dpAsPixels, 0, dpAsPixels, 0);
            }
        }
        footerView=getActivity().getLayoutInflater().inflate(R.layout.divider,null);

        if (aBoolean) {

            listView.addFooterView(footerView);
            listView.setFastScrollEnabled(true);
        } else {

            gridView.setFastScrollEnabled(true);
        }
        if (savedInstanceState == null)
            loadlist(f, false);
        else {
            Bundle b = new Bundle();
            String cur = savedInstanceState.getString("current");
            b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);
            list = savedInstanceState.getParcelableArrayList("list");
            createViews(list, true, new File(cur));
            if (savedInstanceState.getBoolean("selection")) {

                for (int i : savedInstanceState.getIntegerArrayList("position")) {
                    adapter.toggleChecked(i);
                }
            }

            int pos = Sp.getInt("spinner_selected", 0);
            mainActivity.tabsSpinner.setAdapter(tabSpinnerAdapter);
            mainActivity.tabsSpinner.setSelection(pos);

            //listView.setVisibility(View.VISIBLE);
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

    public void add() {

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(fab, "rotation", 0, 180)
        );
        set.setDuration(350).start();

        AlertDialog.Builder ba = new AlertDialog.Builder(getActivity());
        ba.setTitle(utils.getString(getActivity(), R.string.add));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.select_dialog_item);
        adapter.add(utils.getString(getActivity(), R.string.folder));
        adapter.add(utils.getString(getActivity(), R.string.file));
        adapter.add("Tab");
        ba.setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                switch (p2) {

                    case 0:
                        final String path = ma.current;
                        AlertDialog.Builder ba1 = new AlertDialog.Builder(getActivity());
                        ba1.setTitle(utils.getString(getActivity(), R.string.newfolder));
                        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                        final EditText edir = (EditText) v.findViewById(R.id.newname);
                        edir.setHint(utils.getString(getActivity(), R.string.entername));
                        ba1.setView(v);
                        ba1.setNegativeButton(utils.getString(getActivity(), R.string.cancel), null);
                        ba1.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface p1, int p2) {
                                String a = edir.getText().toString();
                                File f = new File(path + "/" + a);
                                if (!f.exists()) {
                                    f.mkdirs();
                                    updateList();
                                    Toast.makeText(getActivity(), "Folder Created", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.fileexist), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        ba1.show();
                        break;
                    case 1:
                        final String path1 = ma.current;
                        AlertDialog.Builder ba2 = new AlertDialog.Builder(getActivity());
                        ba2.setTitle("New File");
                        View v1 = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                        final EditText edir1 = (EditText) v1.findViewById(R.id.newname);
                        edir1.setHint(utils.getString(getActivity(), R.string.entername));
                        ba2.setView(v1);
                        ba2.setNegativeButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface p1, int p2) {
                                // TODO: Implement this method
                            }
                        });
                        ba2.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface p1, int p2) {
                                String a = edir1.getText().toString();
                                File f1 = new File(path1 + "/" + a);
                                if (!f1.exists()) {
                                    try {
                                        boolean b = f1.createNewFile();
                                        updateList();
                                        Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.filecreated), Toast.LENGTH_LONG).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.fileexist), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        ba2.show();
                        break;
                    case 2:
                        int older = tabHandler.getTabsCount();
                        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.animator.tab_anim);

                        tabHandler.addTab(new Tab(older, "legacy", "/storage/emulated/legacy"));
                        //restartPC(getActivity()); // breaks the copy feature
                        Sp.edit().putInt("spinner_selected", older).commit();
                        Sp.edit().putString("current", home).apply();

                        loadlist(new File(home),false);

                        listView.setAnimation(animation);
                        gridView.setAnimation(animation);
                }
            }
        });
        ba.show();


    }

    public void home() {
        ma.loadlist(new File(ma.home), false);
    }

    public void search() {
        final String fpath = ma.current;

        Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.searchpath) + fpath, Toast.LENGTH_LONG).show();
        AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
        a.setTitle(utils.getString(getActivity(), R.string.search));
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText e = (EditText) v.findViewById(R.id.newname);
        e.setHint(utils.getString(getActivity(), R.string.enterfile));
        a.setView(v);
        a.setNeutralButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        a.setPositiveButton(utils.getString(getActivity(), R.string.search), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                String a = e.getText().toString();
                Bundle b = new Bundle();
                b.putString("FILENAME", a);
                b.putString("FILEPATH", fpath);
                new SearchTask((MainActivity) getActivity(), ma).execute(b);

            }
        });
        a.show();
    }


    public void onListItemClicked(int position, View v) {
        if (results) {
            String path = slist.get(position).getDesc();


            final File f = new File(path);
            if (f.isDirectory()) {

                loadlist(f, false);
                results = false;
            } else {
                utils.openFile(f, (MainActivity) getActivity());
            }
        } else if (selection == true) {
            adapter.toggleChecked(position);
            mActionMode.invalidate();
            if (adapter.getCheckedItemPositions().size() == 0) {
                selection = false;
                mActionMode.finish();
                mActionMode = null;
            }

        } else {

            String path, path_name;
            Layoutelements l=list.get(position);
            if(!l.hasSymlink()){
                path= l.getDesc();}
            else{path=l.getSymlink();}
            final File f = new File(path);

            if (f.isDirectory()) {
                computeScroll();
                loadlist(f, false);
            } else {

                utils.openFile(f, (MainActivity) getActivity());
            }

        }
    }

    public void loadlist(final File f, boolean back) {
        if(mActionMode!=null){mActionMode.finish();}
        new LoadList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (f));

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.animator.load_list_anim);
        listView.setAnimation(animation);
        gridView.setAnimation(animation);

        // Spinner

        final int spinner_current = Sp.getInt("spinner_selected", 0);
        tabHandler.updateTab(new Tab(spinner_current, f.getName(), f.getPath()));
        content = tabHandler.getAllTabs();
        list1 = new ArrayList<String>();

        for (Tab tab : content) {
            //adapter1.add(tab.getLabel());
            list1.add(tab.getLabel());
        }

        tabSpinnerAdapter = new TabSpinnerAdapter(getActivity(), R.layout.spinner_layout, list1, getActivity().getSupportFragmentManager(), mainActivity.tabsSpinner);

        mainActivity.tabsSpinner.setAdapter(tabSpinnerAdapter);
        mainActivity.tabsSpinner.setSelection(spinner_current);

        mainActivity.tabsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if (i == spinner_current) {
                    /*Animation animation = AnimationUtils.loadAnimation(getActivity(), R.animator.tab_anim);
                    mainActivity.frameLayout.startAnimation(animation);*/
                }
                else {

                    TabHandler tabHandler1 = new TabHandler(getActivity(), null, null, 1);
                    Tab tab = tabHandler1.findTab(i);
                    String name  = tab.getPath();
                    //Toast.makeText(getActivity(), name, Toast.LENGTH_SHORT).show();
                    Sp.edit().putString("current", name).apply();
                    Sp.edit().putInt("spinner_selected", i).apply();

                    loadlist(new File(tab.getPath()),false);

                    Animation animationLeft = AnimationUtils.loadAnimation(getActivity(), R.animator.tab_selection_left);
                    Animation animationRight = AnimationUtils.loadAnimation(getActivity(), R.animator.tab_selection_right);

                    if (i < spinner_current) {
                        ma.listView.setAnimation(animationLeft);
                        ma.gridView.setAnimation(animationLeft);
                    } else {
                        ma.listView.setAnimation(animationRight);
                        ma.gridView.setAnimation(animationRight);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    @SuppressWarnings("unchecked")
    public void loadsearchlist(ArrayList<String[]> f) {

        new LoadSearchList(ma).execute(f);

    }


    public void createViews(ArrayList<Layoutelements> bitmap, boolean back, File f) {
        try {
            if (bitmap != null) {
                TextView footerText=(TextView) footerView.findViewById(R.id.footerText);
                if(bitmap.size()==0){
                    footerText.setText("No Files");
                }
                else{
                    footerText.setText("Tap and hold on a File or Folder for more options");
                }
                adapter = new MyAdapter(getActivity(), R.layout.rowlayout,
                        bitmap, ma);
                try {

                    aBoolean = sharedPreferences.getBoolean("view", true);
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

                            listView.setSelectionFromTop(b.getInt("index"), b.getInt("top"));
                        }
                    }
                    bbar(current);} catch (Exception e) {
                }
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
            menu.findItem(R.id.cpy).setIcon(icons.getCopyDrawable());
            menu.findItem(R.id.cut).setIcon(icons.getCutDrawable());
            menu.findItem(R.id.delete).setIcon(icons.getDeleteDrawable());
            menu.findItem(R.id.all).setIcon(icons.getAllDrawable());
            menu.findItem(R.id.about).setIcon(icons.getAboutDrawable());


        }

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();

            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.sethome, menu);
            hideOption(R.id.rename, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.about, menu);
            hideOption(R.id.openwith, menu);
            hideOption(R.id.ex, menu);
            mode.setTitle(utils.getString(getActivity(), R.string.select));
            if(Build.VERSION.SDK_INT<19)
                getActivity().findViewById(R.id.action_bar).setVisibility(View.GONE);
            poppyView.setVisibility(View.GONE);
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = adapter.getCheckedItemPositions();
            mode.setSubtitle(positions.size() + " "
                    + utils.getString(getActivity(), R.string.itemsselected));
            if (positions.size() == 1) {
                showOption(R.id.permissions,menu);
                showOption(R.id.about, menu);
                showOption(R.id.rename, menu);
                File x = new File(list.get(adapter.getCheckedItemPositions().get(0))
                        .getDesc());
                if (x.isDirectory()) {
                    showOption(R.id.sethome, menu);
                } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk")) {
                    showOption(R.id.ex, menu);
                    hideOption(R.id.sethome, menu);
                    showOption(R.id.share, menu);
                } else {
                    hideOption(R.id.ex, menu);
                    hideOption(R.id.sethome, menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.share, menu);
                }
            } else {
                hideOption(R.id.ex, menu);
                hideOption(R.id.sethome, menu);
                hideOption(R.id.openwith, menu);
                //hideOption(R.id.share, menu);
                hideOption(R.id.permissions,menu);
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
                    home = list.get(pos).getDesc();
                    Toast.makeText(getActivity(),
                            utils.getString(getActivity(), R.string.newhomedirectory) + list.get(pos).getTitle(),
                            Toast.LENGTH_LONG).show();
                    Sp.edit().putString("home", list.get(pos).getDesc()).apply();

                    mode.finish();
                    return true;
                case R.id.about:
                    utils.showProps(new File(list.get((plist.get(0))).getDesc()), getActivity(),rootMode);
                    mode.finish();
                    return true;
                case R.id.delete:
                    utils.deleteFiles(list,ma, plist);

                    mode.finish();

                    return true;
                case R.id.share:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    ArrayList<Uri> uris=new ArrayList<Uri>();
                    for(int i:plist){
                        uris.add(Uri.fromFile(new File(list.get(i).getDesc())));
                    }
                    sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);
                    sendIntent.setType("*/*");
                    startActivity(sendIntent);
                    mode.finish();
                    return true;
                case R.id.all:
                    if (adapter.areAllChecked()) {
                        adapter.toggleChecked(false);
                    } else {
                        adapter.toggleChecked(true);
                    }
                    mode.invalidate();

                    return true;
                case R.id.rename:
                    final ActionMode m = mode;
                    final File f = new File(list.get(
                            (plist.get(0))).getDesc());
                    View dialog = getActivity().getLayoutInflater().inflate(
                            R.layout.dialog, null);
                    AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
                    final EditText edit = (EditText) dialog
                            .findViewById(R.id.newname);
                    edit.setText(f.getName());
                    a.setView(dialog);
                    a.setTitle(utils.getString(getActivity(), R.string.rename));
                    a.setPositiveButton(utils.getString(getActivity(), R.string.save),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface p1, int p2) {
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
                            }
                    );
                    a.setNegativeButton(utils.getString(getActivity(), R.string.cancel),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface p1, int p2) {
                                    m.finish();
                                }
                            }
                    );
                    a.show();
                    mode.finish();
                    return true;
                case R.id.book:
                    for (int i1 = 0; i1 < plist.size(); i1++) {
                        try {
                            sh.addS(new File(list.get(plist.get(i1)).getDesc()));

                        } catch (Exception e) {
                        }
                    }
                    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                    mode.finish();
                    return true;
                case R.id.ex:
                    Intent intent = new Intent(getActivity(), ExtractService.class);
                    intent.putExtra("zip", list.get(
                            (plist.get(0))).getDesc());
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
                    mainActivity.invalidatePasteButton();
                    mode.finish();
                    return true;
                case R.id.cut:
                    mainActivity.COPY_PATH=null;
                    ArrayList<String> copie = new ArrayList<String>();
                    for (int i3 = 0; i3 < plist.size(); i3++) {
                        copie.add(list.get(plist.get(i3)).getDesc());
                    }
                    mainActivity.MOVE_PATH = copie;
                    mainActivity.invalidatePasteButton();
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
                    utils.openWith(new File(list.get(
                            (plist.get(0))).getDesc()), getActivity());
                    mode.finish();
                    return true;
                case R.id.permissions:
                    utils.setPermissionsDialog(new File(list.get(plist.get(0)).getDesc()),ma);
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
            adapter.toggleChecked(false);
            poppyView.setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.action_bar).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.buttonbarframe).setVisibility(View.VISIBLE);

        }
    };

    public void bbar(String text) {
        try {
            buttons.removeAllViews();
            //Drawable bg=getResources().getDrawable(R.drawable.listitem1);
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
                            loadlist(new File("/"), false);
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
                            loadlist(new File(rpaths.get(index)), true);
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
                            loadlist(new File(rpaths.get(index)), true);
                            timer.cancel();
                            timer.start();
                        }
                    });

                    buttons.addView(button);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                }
            }
            File f=new File(text);
            ((TextView)pathbar.findViewById(R.id.pathname)).setText(f.getName());
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
            if (utils.canGoBack(f)) {
                loadlist(f.getParentFile(), true);
            } else {
                Toast.makeText(getActivity(), "You're at the root", Toast.LENGTH_SHORT).show();
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
        loadlist(new File(current), true);}

    public void getSortModes() {
        int t = Integer.parseInt(Sp.getString("sortby", "0"));
        if (t <= 2) {
            sortby = t;
            asc = 1;
        } else if (t >= 3) {
            asc = -1;
            sortby = t - 3;
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
    public ArrayList<Layoutelements> addTo(ArrayList<String[]> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        for (int i = 0; i < mFile.size(); i++) {
            File f=new File(mFile.get(i)[0]);
            if (f.isDirectory()) {
                a.add(utils.newElement(folder, f.getPath(),mFile.get(i)[2],mFile.get(i)[1],utils.count(f,rootMode)));

            } else {
                try {
                    a.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f.getPath(),!aBoolean), f.getPath(),mFile.get(i)[2],mFile.get(i)[1],utils.getSize(mFile.get(i))));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
if(history!=null)
        history.end();     }
    public void initPoppyViewListeners(View poppy){
        ((ImageView)poppy.findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack();if(mActionMode!=null){mActionMode.finish();}
            }
        });
        final ImageView homebutton=(ImageView)poppy.findViewById(R.id.home);
        (homebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                home();if(mActionMode!=null){mActionMode.finish();}
            }
        });
        ((ImageView)poppy.findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ma.loadlist(new File(ma.current), false);if(mActionMode!=null){mActionMode.finish();}
            }
        });
        ((ImageView)poppy.findViewById(R.id.history)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utils.showHistoryDialog(ma);
            }
        });
        ((ImageView)poppy.findViewById(R.id.books)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utils.showBookmarkDialog(ma,sh);
            }
        });
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.activity_extra, popup.getMenu());

        // Getting option for listView and gridView
        MenuItem s = popup.getMenu().findItem(R.id.view);

        aBoolean = sharedPreferences.getBoolean("view", true);
        if (aBoolean) {
            s.setTitle("Grid View");
        } else {
            s.setTitle("List View");
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.item3:
                        getActivity().finish();
                        break;
                    case R.id.item9:
                        Sp.edit().putString("home", ma.current).apply();
                        Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.newhomedirectory) + ma.home, Toast.LENGTH_LONG).show();
                        ma.home = ma.current;
                        break;
                    case R.id.item10:
                        utils.showSortDialog(ma);
                        break;
                    case R.id.item11:
                        utils.showDirectorySortDialog(ma);
                        break;
                    case R.id.item4:
                        search();
                        break;
                    case R.id.view:
                        if (aBoolean) {
                            Toast.makeText(getActivity(), "Setting GridView", Toast.LENGTH_SHORT).show();
                            sharedPreferences.edit().putBoolean("view", false).commit();

                        } else {
                            Toast.makeText(getActivity(), "Setting ListView", Toast.LENGTH_SHORT).show();
                            sharedPreferences.edit().putBoolean("view", true).commit();

                        }restartPC(getActivity());
                        break;
                }
                return false;
            }
        });
        popup.show();
    }
    private void crossfade() {

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

    }public static void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
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

    @Override
    public void onDetach() {
        super.onDetach();
        boolean check = Sp.getBoolean("random_checkbox", false);
        if (check) {
            random();
        }
    }

    private void random() {
        
        String[] colors = new String[]{
                "#e51c23",
                "#e91e63",
                "#9c27b0",
                "#673ab7",
                "#3f51b5",
                "#5677fc",
                "#03a9f4",
                "#00bcd4",
                "#009688",
                "#259b24",
                "#8bc34a",
                "#cddc39",
                "#ffeb3b",
                "#ffc107",
                "#ff9800",
                "#ff5722",
                "#795548",
                "#9e9e9e",
                "#607d8b"
        };

        Random random = new Random();
        int pos = random.nextInt(colors.length - 1);
        Sp.edit().putString("skin_color", colors[pos]).commit();
    }
}
