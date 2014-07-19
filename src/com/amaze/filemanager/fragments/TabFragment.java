package com.amaze.filemanager.fragments;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v4.app.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.activities.*;
import com.amaze.filemanager.services.*;
import com.amaze.filemanager.utils.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import de.keyboardsurfer.android.widget.crouton.*;
import android.util.*;


public class TabFragment extends android.support.v4.app.Fragment {
	List<android.support.v4.app.Fragment> fragments = new ArrayList<android.support.v4.app.Fragment>();
	public PagerAdapter mSectionsPagerAdapter;
	android.support.v4.view.PagerTitleStrip STRIP;
	Futils utils = new Futils();
	ViewPager mViewPager;
	LinearLayout BUTTONS;
	Shortcuts sh=new Shortcuts();
	boolean TAB=false;SharedPreferences Sp;
	ArrayList<String> COPY_PATH=null,MOVE_PATH=null;
String HOME;IconUtils icons;
ProgressDialog EXTRACT_PROGRESS=null;
ActionBar ACTION_BAR;TabFragment t=this;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment,
				container, false);
		 Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		 icons=new IconUtils(Sp,getActivity());
		mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
				getActivity().getSupportFragmentManager());
		mViewPager = (ViewPager) rootView.findViewById(R.id.pager);

		
	ACTION_BAR= getActivity().getActionBar();

		setHasOptionsMenu(true);
	
		
		 
        boolean firstrun=Sp.getBoolean("firstrun",true);
		if(firstrun){
				Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.guide1),Style.INFO).show();
				Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.guide2),Style.ALERT).show();
				Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.guide3),Style.CONFIRM).show();
			  	Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.guide4),Style.CONFIRM).show();
				Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.guide5),Style.INFO).show();
				Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.guide6),Style.CONFIRM).show();
			    Sp.edit().putBoolean("firstrun",false).apply();
		}
	  HOME=Sp.getString("home",Environment.getExternalStorageDirectory().getPath());
	if(!TAB){
		
		addTab(HOME);TAB=true;}
		BUTTONS= (LinearLayout) getActivity().findViewById(R.id.buttons);
		STRIP = ((android.support.v4.view.PagerTitleStrip) rootView
				.findViewById(R.id.pager_title_strip));
		STRIP.setOnClickListener(new View.OnClickListener() {

			public void onClick(View p1) {
				if (BUTTONS.getVisibility() == View.VISIBLE) {
					BUTTONS.setVisibility(View.GONE);
				} else {
					BUTTONS.setVisibility(View.VISIBLE);
				}
				// TODO: Implement this method
			}
		});	//setRetainInstance(true);
		mViewPager.setPageTransformer(true,new FlipAnim());
		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

					public void onPageScrolled(int p1, float p2, int p3) {
						// TODO: Implement this method
					}

					public void onPageSelected(int p1) { // TODO: Implement this								// method
						Main ma = ((Main) fragments.get(p1));
						if (ma.current != null) {
							ma.bbar(ma.current);
						}
					}

					public void onPageScrollStateChanged(int p1) {
						// TODO: Implement this method
					}
				});
        mViewPager.setOffscreenPageLimit(5);
		
		mViewPager.setAdapter(mSectionsPagerAdapter);
		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.activity_extra, menu);
		initMenu(menu);
		
	}private void hideOption(int id, Menu menu) {
		MenuItem item = menu.findItem(id);
		item.setVisible(false);
	}
	private void showOption(int id, Menu menu) {
		MenuItem item = menu.findItem(id);
		item.setVisible(true);
	}
	
  public void onPrepareOptionsMenu(Menu menu){
	  hideOption(R.id.item8,menu);
	  if(COPY_PATH!=null){ showOption(R.id.item8,menu);}
	  if(MOVE_PATH!=null){ showOption(R.id.item8,menu);}
  }
	public boolean onOptionsItemSelected(MenuItem item) { 
		switch (item.getItemId()) {
	    case R.id.item8:if(COPY_PATH!=null){
				Main ma = ((Main) fragments.get(mViewPager.getCurrentItem()));
				String path1=ma.current;
				Intent intent =new Intent(getActivity(),CopyService.class);
				intent.putExtra("FILE_PATHS",COPY_PATH);
				intent.putExtra("COPY_DIRECTORY",path1);
				getActivity().startService(intent);
				COPY_PATH=null;
				getActivity().invalidateOptionsMenu();
			}if(MOVE_PATH!=null){
				Main ma = ((Main) fragments.get(mViewPager.getCurrentItem()));
				String path1=ma.current;
				Intent intent =new Intent(getActivity(),CopyService.class);
				intent.putExtra("FILE_PATHS",MOVE_PATH);
				intent.putExtra("move",true);
				intent.putExtra("COPY_DIRECTORY",path1);
				getActivity().startService(intent);
				MOVE_PATH=null;
				getActivity().invalidateOptionsMenu();
			}
		break;
		case R.id.item1:
			back();

			break;
			case R.id.item9:
				Main ma1=getCurrentTab();
				Sp.edit().putString("home", ma1.current).commit();
				Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.newhomedirectory)+ma1.home,Style.CONFIRM).show();
				ma1.home=ma1.current;
			break;
		case R.id.item2:
			home();
			break;
            case R.id.item10:
                utils.showSortDialog(getCurrentTab());
                break;
            case R.id.item11:
                utils.showDirectorySortDialog(getCurrentTab());
                break;
		case R.id.item3:
			removePage(mViewPager.getCurrentItem());
			break;
		case R.id.item5:
			add(HOME);
			break;
            case R.id.item12:utils.showHistoryDialog(getCurrentTab());
                break;
			case R.id.item7:
		Main ma = getCurrentTab();
				ma.loadlist(new File(ma.current),false);
			break;
		case R.id.item4:
	search();
			break;
			case R.id.item6:
               utils.showBookmarkDialog(getCurrentTab(),sh);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

		@Override
		public CharSequence getPageTitle(int position) {
			Main ma = ((Main) fragments.get(position));
			if(ma.results){return utils.getString(getActivity(), R.string.searchresults);}
			else{
			if(ma.current.equals("/")){return "Root";}
            else{return new File(ma.current).getName();}}
		}

		public int getCount() {
			// TODO: Implement this method
			return fragments.size();
		}

		public ScreenSlidePagerAdapter(android.support.v4.app.FragmentManager fm) {
			super(fm);
		}

		@Override
		public android.support.v4.app.Fragment getItem(int position) {
			android.support.v4.app.Fragment f;
			f = fragments.get(position);
			return f;
		}

	}

	public void removePage(int position) {
	if(fragments.size()==1){getActivity().finish();}
	else{
		if(position==0){
            mViewPager.setCurrentItem(1);
            fragments.remove(position);
            mSectionsPagerAdapter.notifyDataSetChanged();
			}
		else{
            mViewPager.setCurrentItem(position-1);
            fragments.remove(position);
			mSectionsPagerAdapter.notifyDataSetChanged();
			}
	}
	       	
}
		public void addTab(String text){
				android.support.v4.app.Fragment main = new Main();
										Bundle b = new Bundle();
										int p = fragments.size();
										b.putString("path", text);
										main.setArguments(b);
                                        fragments.add(main);

										mSectionsPagerAdapter.notifyDataSetChanged();
										mViewPager.setCurrentItem(p);
		}
	

	public void add(final String text) {
			AlertDialog.Builder ba=new AlertDialog.Builder(getActivity());
					ba.setTitle(utils.getString(getActivity(),R.string.add));
					
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						getActivity(), android.R.layout.select_dialog_item);
		adapter.add(utils.getString(getActivity(), R.string.tab));adapter.add(utils.getString(getActivity(), R.string.file));adapter.add(utils.getString(getActivity(), R.string.file));
					ba.setAdapter(adapter, new DialogInterface.OnClickListener(){

							public void onClick(DialogInterface p1, int p2)
							{switch(p2){
									case 0:	addTab(text);
										break;
									case 1:	Main ma = ((Main) fragments.get(mViewPager.getCurrentItem()));
									      final String path=ma.current;
										AlertDialog.Builder ba1=new AlertDialog.Builder(getActivity());
										ba1.setTitle(utils.getString(getActivity(), R.string.newfolder));
										View v=getActivity().getLayoutInflater().inflate(R.layout.dialog,null);
										final EditText edir=(EditText)v.findViewById(R.id.newname);
										edir.setHint(utils.getString(getActivity(), R.string.entername));
										ba1.setView(v);
										ba1.setNegativeButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener(){

												public void onClick(DialogInterface p1, int p2)
												{
													// TODO: Implement this method
												}
											});
										ba1.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener(){

												public void onClick(DialogInterface p1, int p2)
												{String a=edir.getText().toString();
												File f=new File(path+"/"+a);
													if(!f.exists()){f.mkdirs();Toast.makeText(getActivity(),"Folder Created",Toast.LENGTH_LONG).show();}
												else{Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.fileexist),Style.ALERT).show();}
													// TODO: Implement this method
												}
											});
											ba1.show();
										break;
									case 2:Main ma1 = ((Main) fragments.get(mViewPager.getCurrentItem()));
										final String path1=ma1.current;
										AlertDialog.Builder ba2=new AlertDialog.Builder(getActivity());
										ba2.setTitle(utils.getString(getActivity(), R.string.newfolder));
										View v1=getActivity().getLayoutInflater().inflate(R.layout.dialog,null);
										final EditText edir1=(EditText)v1.findViewById(R.id.newname);
										edir1.setHint(utils.getString(getActivity(), R.string.entername));
										ba2.setView(v1);
										ba2.setNegativeButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener(){

												public void onClick(DialogInterface p1, int p2)
												{
													// TODO: Implement this method
												}
											});
										ba2.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener(){

												public void onClick(DialogInterface p1, int p2)
												{String a=edir1.getText().toString();
													File f1=new File(path1+"/"+a);
													if (!f1.exists())
													{try
														{
															f1.createNewFile();
															Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.filecreated), Style.CONFIRM).show();
														}
														catch (IOException e)
														{}}
													else
{Crouton.makeText(getActivity(),utils.getString(getActivity(), R.string.fileexist), Style.ALERT).show();}
													// TODO: Implement this method
												}
											});
											ba2.show();
										break;
							}
							}
						});
						ba.show();
	

	}

	public void back() {

		Main ma = ((Main) fragments.get(mViewPager.getCurrentItem()));
		if(ma.results){ma.loadlist(new File(ma.current),true);}else{
		if (utils.canGoBack(new File(ma.current))) {
			ma.loadlist(new File(ma.current).getParentFile(),true);
		}}
	}

	public void home() {

		Main ma = ((Main) fragments.get(mViewPager.getCurrentItem()));

		ma.loadlist(new File(ma.home),false);
	}
	public void search() {

		Main ma = ((Main) fragments.get(mViewPager.getCurrentItem()));
	    final String fpath=ma.current;
		
	    Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.searchpath)+fpath, Toast.LENGTH_LONG).show();
		AlertDialog.Builder a=new AlertDialog.Builder(getActivity());
		a.setTitle(utils.getString(getActivity(), R.string.search));
		View v=getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
		final EditText e=(EditText)v.findViewById(R.id.newname);
		e.setHint(utils.getString(getActivity(), R.string.enterfile));
		a.setView(v);
		a.setNeutralButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {}});
a.setPositiveButton(utils.getString(getActivity(), R.string.search), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				String a=e.getText().toString();
	          Bundle b=new Bundle();
			  b.putString("FILENAME",a);
			  b.putString("FILEPATH",fpath);
			  new SearchTask((MainActivity)getActivity(),t).execute(b);

			}
		});
a.show();
	}


	  @Override
	public void onResume() {
	    super.onResume();
	
		  LocalBroadcastManager.getInstance(getActivity()).registerReceiver(PASTE_RECIEVER, new IntentFilter("copy_path"));
		  LocalBroadcastManager.getInstance(getActivity()).registerReceiver(PASTE_RECIEVER1, new IntentFilter("move_path"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(UPDATE_PAGER, new IntentFilter("updatepager"));

	}
	  @Override
	public void onPause() {
	    super.onPause();
		if(EXTRACT_PROGRESS!=null){
		EXTRACT_PROGRESS.dismiss();}
	    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(UPDATE_PAGER);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(PASTE_RECIEVER);
		  LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(PASTE_RECIEVER1);
	  }
	private BroadcastReceiver PASTE_RECIEVER = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b=intent.getExtras();
			if(b!=null){
				
		   COPY_PATH=b.getStringArrayList("copy_path");
		   MOVE_PATH=null;
		getActivity().invalidateOptionsMenu();
		}}
	};
	private BroadcastReceiver PASTE_RECIEVER1 = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b=intent.getExtras();
			if(b!=null){

				MOVE_PATH=b.getStringArrayList("move_path");
			    COPY_PATH=null;
			    getActivity().invalidateOptionsMenu();
			}}
	};
	  private BroadcastReceiver UPDATE_PAGER = new BroadcastReceiver() {

		    @Override
		    public void onReceive(Context context, Intent intent) {
		    mSectionsPagerAdapter.notifyDataSetChanged();
		    }
		  };
	
	private void verifyFiles(String a,String b){
		File f=new File(a);
		File f1=new File(b,f.getName());
        HashMap<String,Integer> hash=new HashMap<String,Integer>();

		if(f.isDirectory()){
			if(f1.exists() && f1.isDirectory()){
				File[] files=( f1.listFiles());
				File[] ofiles= (f.listFiles());
				for(File fff :ofiles){

					for(File fa:files){
						if(fff==fa){
							if(fa.isDirectory() && fff.isDirectory()){
								//prompt merge dialog
								verifyFiles(fff.getPath(),fa.getPath());
							}
							else{replaceDialog(fa,hash);
								//promp replace dialog
							}
						}
					}
				}

			}

		}else{ if(f1.exists()){
            replaceDialog(f1,hash);
				//promp replace dialog
			}
		}}
        public void replaceDialog(final File f,final HashMap<String,Integer> hash){
            AlertDialog.Builder a=new AlertDialog.Builder(getActivity());
            a.setTitle("Paste File");
            a.setMessage("File already exists \n"+f.getName());
            a.setPositiveButton("Replace",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    hash.put(f.getPath(),0);
                }
            });
            a.setNegativeButton("Skip",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    hash.put(f.getPath(),1);
                }
            });

	}public Main getCurrentTab(){
		Main man = ((Main) fragments.get(mViewPager.getCurrentItem()));
		return man;
	}
  public void initMenu(Menu menu){
	  menu.findItem(R.id.item1).setIcon(icons.getBackDrawable());
	  menu.findItem(R.id.item2).setIcon(icons.getHomeDrawable());
	  menu.findItem(R.id.item3).setIcon(icons.getCancelDrawable());
	  menu.findItem(R.id.item4).setIcon(icons.getSearchDrawable());
	  menu.findItem(R.id.item5).setIcon(icons.getNewDrawable());
	  menu.findItem(R.id.item6).setIcon(icons.getBookDrawable());
	  menu.findItem(R.id.item7).setIcon(icons.getRefreshDrawable());
	  menu.findItem(R.id.item8).setIcon(icons.getPasteDrawable());
      menu.findItem(R.id.item12).setIcon(icons.getBookDrawable());
  }
  }
	
