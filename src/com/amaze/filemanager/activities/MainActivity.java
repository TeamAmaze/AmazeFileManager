package com.amaze.filemanager.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.BookmarksManager;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.ProcessViewer;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Shortcuts;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends android.support.v4.app.FragmentActivity {
    int select;
    TextView title;

    Futils utils;
    private boolean backPressedToExitOnce = false;
    private Toast toast = null;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    SharedPreferences Sp;
    private ActionBarDrawerToggle mDrawerToggle;
    public Spinner tabsSpinner;
    private TabHandler tabHandler;
    private List<Tab> content;
    private ArrayList<String> list1;
    private ArrayAdapter<String> adapter1;

    String[] val;
    ProgressBar progress;
    DrawerAdapter adapter;
    IconUtils util;
    RelativeLayout mDrawerLinear;
    Shortcuts s = new Shortcuts();
    int tab=0; public String skin;
    public int theme;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Futils();
        val = new String[]{utils.getString(this, R.string.storage), utils.getString(this, R.string.apps), utils.getString(this, R.string.bookmanag)};
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        theme=Integer.parseInt(Sp.getString("theme","0"));
        util = new IconUtils(Sp, this);
        int th = Integer.parseInt(Sp.getString("theme", "0"));
        if (th == 1) {
            setTheme(R.style.DarkTheme);
        }
        setContentView(R.layout.main);
        getActionBar().hide();
        title=(TextView)findViewById(R.id.title);
        tabsSpinner = (Spinner) findViewById(R.id.tab_spinner);

        // BBar
       skin = PreferenceManager.getDefaultSharedPreferences(this).getString("skin_color", "#009688");
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.action_bar);
        linearLayout.setBackgroundColor(Color.parseColor(skin));
        LinearLayout linearLayout1 = (LinearLayout) findViewById(R.id.pathbar);
        linearLayout1.setBackgroundColor(Color.parseColor(skin));
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if(tab==0){
                    replaceFragment(1);
                }else{replaceFragment(0);}*/
            }
        });
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.scroll1);
        horizontalScrollView.setBackgroundColor(Color.parseColor(skin));
        HorizontalScrollView horizontalScrollView1 = (HorizontalScrollView) findViewById(R.id.scroll);
        horizontalScrollView1.setBackgroundColor(Color.parseColor(skin));
        LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.drawerheader);
        linearLayout2.setBackgroundColor(Color.parseColor(skin));
        LinearLayout linearLayout3 = (LinearLayout) findViewById(R.id.settings_bg);
        linearLayout3.setBackgroundColor(Color.parseColor(skin));

        tabsSpinner.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        if(Sp.getBoolean("firstrun",true)){
        try {
            s.makeS();
        } catch (Exception e) {
        }Sp.edit().putBoolean("firstrun",false);}
        mDrawerLinear = (RelativeLayout) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.menu_drawer);
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < val.length; i++) {
            list.add(val[i]);
        }
        adapter= new DrawerAdapter(this, val, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);

        if (savedInstanceState == null) {
            selectItem(0);
        }if(select<4){title.setText(val[select]);}
        if(Build.VERSION.SDK_INT>=19){
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintColor(Color.parseColor(skin));
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        DrawerLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mDrawerLayout.getLayoutParams();
        p.setMargins(0,config.getPixelInsetTop(false),0,0);
             }
        ((ImageButton)findViewById(R.id.settingsbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(getApplicationContext(),Preferences.class);
                finish();
                startActivity(i);
            }
        });
        if (th == 1) {
            mDrawerList.setBackgroundResource(android.R.drawable.screen_background_dark);
        }
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setDivider(null);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(select!=102){
                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.content_frame, new ProcessViewer());
                    //   transaction.addToBackStack(null);
                    select = 102;
//Commit the transaction
                    transaction.commit();
                }else{selectItem(0);}
            }
        });
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                util.getDrawerDrawable(),  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                if (select <= 5) {
                    title.setText(val[select]);
                    getActionBar().setSubtitle(val[select]);
                }// creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //title.setText("Amaze File Manager");
                getActionBar().setSubtitle(utils.getString(MainActivity.this, R.string.select));
                // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        ((ImageButton)findViewById(R.id.drawer_buttton)).setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mDrawerLayout.isDrawerOpen(mDrawerLinear)){mDrawerLayout.closeDrawer(mDrawerLinear);}
                else mDrawerLayout.openDrawer(mDrawerLinear);
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (select == 0) {
            Main main = ((Main) getSupportFragmentManager().findFragmentById(R.id.content_frame));



            if (main.results == true) {
                main.results = false;
                main.loadlist(new File(main.current), true);
            } else {
                if (!main.current.equals(main.home)) {
                    if (utils.canGoBack(new File(main.current))) {
                        main.goBack();

                    } else {
                        exit();
                    }
                } else exit();

            }
        } else {
            selectItem(0);
        }
    }

    public void exit() {
        if (backPressedToExitOnce) {
            finish();
        } else {
            this.backPressedToExitOnce = true;
            showToast(utils.getString(this, R.string.pressagain));
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    backPressedToExitOnce = false;
                }
            }, 2000);
        }
    }
    private void replaceFragment (int position){
        FragmentManager manager =getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate ("tab"+position, 0);
        if (!fragmentPopped){ //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.content_frame, new Main());
            ft.addToBackStack("tab"+position);
            ft.commit();
        }tab=position;
    }
    public void selectItem(int i) {
        switch (i) {
            case 0:

                android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_frame, new Main());
               // transaction.addToBackStack(null);
                select = 0;
// Commit the transaction
                transaction.addToBackStack("tab"+1);
                transaction.commit();
                title.setText(val[0]);
                title.setVisibility(View.GONE);
                tabsSpinner.setVisibility(View.VISIBLE);

                break;

            case 1:
                android.support.v4.app.FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                transaction2.replace(R.id.content_frame, new AppsList());
               // transaction2.addToBackStack(null);
                select = 1;
// Commit the transaction
                transaction2.commit();
                title.setText(val[1]);
                title.setVisibility(View.VISIBLE);
                tabsSpinner.setVisibility(View.GONE);
                break;
            case 2:
                android.support.v4.app.FragmentTransaction transaction3 = getSupportFragmentManager().beginTransaction();
                transaction3.replace(R.id.content_frame, new BookmarksManager());
               // transaction3.addToBackStack(null);
                select = 2;
// Commit the transaction
                transaction3.commit();
                title.setText(val[2]);
                title.setVisibility(View.VISIBLE);
                tabsSpinner.setVisibility(View.GONE);
                break;


        }
        adapter.toggleChecked(i);
        mDrawerLayout.closeDrawer(mDrawerLinear);
    }

    private void showToast(String message) {
        if (this.toast == null) {
            // Create toast if found null, it would he the case of first call only
            this.toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);

        } else if (this.toast.getView() == null) {
            // Toast not showing, so create new one
            this.toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);

        } else {
            // Updating toast message is showing
            this.toast.setText(message);
        }

        // Showing toast finally
        this.toast.show();
    }

    private void killToast() {
        if (this.toast != null) {
            this.toast.cancel();
        }
    }

    public void back() {
        super.onBackPressed();
    }


    //// called when the user exits the action mode
//	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
//

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        killToast();
        unregisterReceiver(RECIEVER);
        progress.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(RECIEVER, new IntentFilter("run"));

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
       if ( keyCode == KeyEvent.KEYCODE_MENU ) {
ImageButton ib=(ImageButton)findViewById(R.id.action_overflow);
if(ib.getVisibility()==View.VISIBLE){
	ib.performClick();
}
           // perform your desired action here

           // return 'true' to prevent further propagation of the key event
           return true;
       }

       // let the system handle all other key events
       return super.onKeyDown(keyCode, event);
    }
    private BroadcastReceiver RECIEVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                progress.setVisibility(b.getBoolean("run", false) ? View.VISIBLE : View.GONE);
            }
        }
    };
}
