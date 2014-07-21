package com.amaze.filemanager.activities;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.widget.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.fragments.*;
import com.amaze.filemanager.services.*;
import com.amaze.filemanager.utils.*;
import com.amaze.filemanager.adapters.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import android.preference.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import android.support.v4.content.*;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class MainActivity extends android.support.v4.app.FragmentActivity
{int select;
	View footer;Futils utils;private boolean backPressedToExitOnce = false;
	private Toast toast = null;
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    SharedPreferences Sp;
    private ActionBarDrawerToggle mDrawerToggle;

String[] val;
ImageButton progress;
	
	IconUtils util;
	Shortcuts s=new Shortcuts();
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
		Sp = PreferenceManager.getDefaultSharedPreferences(this);
		util=new IconUtils(Sp,this);
		int th=Integer.parseInt(Sp.getString("theme","0"));
		if(th==1){
	setTheme(R.style.DarkTheme);}
       setContentView(R.layout.main);
   
		try
		{
			s.makeS();
		}
		catch (Exception e)
		{}
		utils = new Futils();
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mDrawerList = (ListView) findViewById(R.id.left_drawer);
		val=new String[]{utils.getString(this,R.string.storage),utils.getString(this,R.string.apps),utils.getString(this,R.string.bookmanag),utils.getString(this,R.string.setting)};
	ArrayList<String> list=new ArrayList<String>();
	for(int i=0;i<val.length;i++){
		list.add(val[i]);
	}
	DrawerAdapter adapter =new DrawerAdapter(this,val,MainActivity.this,Sp);
	mDrawerList.setAdapter(adapter);
	if(th==1){mDrawerList.setBackgroundResource(android.R.drawable.screen_background_dark);}
	mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

	
	ActionBar bar=getActionBar();
	View v=getLayoutInflater().inflate(R.layout.button,null);
	progress=(ImageButton)v.findViewById(R.id.progress);
	progress.setImageDrawable(getResources().getDrawable(R.drawable.ic_prog));
    bar.setCustomView(v);
	progress.setVisibility(View.GONE);
progress.setOnClickListener(new View.OnClickListener() {
	
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
		android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, new ProcessViewer());
        transaction.addToBackStack(null);
select=102;
//Commit the transaction
        transaction.commit();
		
	}
});
	bar.setDisplayShowCustomEnabled(true);bar.setDisplayShowTitleEnabled(true);

  getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

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
				if(select<=5){
                getActionBar().setSubtitle(val[select]);
                 }// creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
               getActionBar().setSubtitle(utils.getString(MainActivity.this,R.string.select));
             // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }

		
	/*try {
  ViewConfiguration config = ViewConfiguration.get(this);
  Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

  if (menuKeyField != null) {
    menuKeyField.setAccessible(true);
    menuKeyField.setBoolean(config, false);
  }
}
catch (Exception e) {
  // presumably, not relevant
}*/

	} 
	@Override
	public void onBackPressed(){
		
		if(select==0){
           Main main=(Main) getSupportFragmentManager().findFragmentById(R.id.content_frame);
		   
		if(main.results==true){main.results=false;main.loadlist(new File(main.current),true);}
	else{
          if(!main.current.equals(main.home)){
                if(utils.canGoBack(new File(main.current))){
             main.goBack();
            }else{exit();}
        }else exit();

}	}
		else{selectItem(0);}}
    public void exit(){
        if (backPressedToExitOnce) {
            finish();
        } else {
            this.backPressedToExitOnce = true;
            showToast(utils.getString(this,R.string.pressagain));
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    backPressedToExitOnce = false;
                }
            }, 2000);
        }
    }
		public void selectItem(int i){
			switch(i){case 0:
			
			android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.content_frame, new Main());
            transaction.addToBackStack(null);
select=0;
// Commit the transaction
            transaction.commit();
			getActionBar().setSubtitle(val[0]);
					mDrawerList.setItemChecked(0, true);
			break;
				
				case 1:
					android.support.v4.app.FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                 	transaction2.replace(R.id.content_frame, new AppsList());
					transaction2.addToBackStack(null);
					select=2;
// Commit the transaction
					transaction2.commit();
					getActionBar().setSubtitle(val[2]);
				break;
			case 2:	android.support.v4.app.FragmentTransaction transaction3= getSupportFragmentManager().beginTransaction();
                    transaction3.replace(R.id.content_frame, new BookmarksManager());
					transaction3.addToBackStack(null);
					select=3;
// Commit the transaction
					transaction3.commit();
					getActionBar().setSubtitle(val[3]);
			break;
				case 3: Intent in=new Intent(MainActivity.this,Preferences.class);finish();startActivity(in);
				break;
			
			} 
		mDrawerList.setItemChecked(i, true);
        
        mDrawerLayout.closeDrawer(mDrawerList);
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
		public void back(){super.onBackPressed();}

	
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
	public void onResume(){
		super.onResume();
		registerReceiver(RECIEVER,new IntentFilter("run"));

}	private BroadcastReceiver RECIEVER = new BroadcastReceiver() {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle b=intent.getExtras();
		if(b!=null){
			progress.setVisibility(b.getBoolean("run", false) ? View.VISIBLE :View.GONE);
	}}
};
}
