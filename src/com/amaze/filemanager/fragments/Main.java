package com.amaze.filemanager.fragments;



import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.support.v4.app.*;
import android.support.v4.content.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.activities.*;
import com.amaze.filemanager.adapters.*;
import com.amaze.filemanager.services.ZipTask;
import com.amaze.filemanager.utils.*;
import de.keyboardsurfer.android.widget.crouton.*;
import java.io.*;
import java.lang.ref.*;
import java.util.*;

import android.support.v4.app.ListFragment;
import com.amaze.filemanager.services.*;


public class Main extends ListFragment {
	File[] file;
	public ArrayList<Layoutelements> list,slist;
	TextView prog;
	MyAdapter adapter;
	Futils utils;
	boolean orient=false;
	private android.util.LruCache<String, Bitmap> mMemoryCache;
	public String lastpath;
	public ArrayList<File> sFile,array,mFile=new ArrayList<File>();
	public boolean selection;
	LoadListTask listtask;
	public boolean results = false;
	LinearLayout utilbar;
	public ActionMode mActionMode;
	ProgressBar pbar;
	NotificationCompat.Builder mBuilder;
	public SharedPreferences Sp;
	Drawable folder,apk,unknown,archive,text;
	Resources res;
	LinearLayout buttons;
	int sortby,dsort,asc;
	public int uimode;
	public String home,current=Environment.getExternalStorageDirectory().getPath(),sdetails;
	android.support.v4.view.PagerTitleStrip strip;
    Shortcuts sh=new Shortcuts();
	HashMap<String,Bundle> scrolls=new HashMap<String,Bundle>();
	Main ma=this;
	IconUtils icons;
	int p;
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(false);
		utils = new Futils();
		res=getResources();

	    Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
	    icons=new IconUtils(Sp,getActivity());
		int foldericon=Integer.parseInt(Sp.getString("folder","1"));
		switch(foldericon){
			case 0:	folder=res.getDrawable(R.drawable.ic_grid_folder);
			break;
			case 1:	folder=res.getDrawable(R.drawable.ic_grid_folder1);
			break;
			case 2:	folder=res.getDrawable(R.drawable.ic_grid_folder2);
			break;
			default:	folder=res.getDrawable(R.drawable.ic_grid_folder);
		}
	  //  Crouton.makeText(getActivity(),""+IconUtils.getMimeType("/sdcard/AndroidClock.ttf"),Style.ALERT).show();
		apk=res.getDrawable(R.drawable.ic_doc_apk);
		unknown=res.getDrawable(R.drawable.ic_doc_generic_am);
		archive=res.getDrawable(R.drawable.archive_blue);
		text=res.getDrawable(R.drawable.ic_doc_text_am);
		getSortModes();
		home=Sp.getString("home",Environment.getExternalStorageDirectory().getPath());
		sdetails=Sp.getString("viewmode","0");
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		this.setRetainInstance(true);
      	final int cacheSize = maxMemory / 4;
		mMemoryCache = new android.util.LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmp) {
				return bitmp.getByteCount() / 1024;
			}
		};
	   
		File f=new File(getArguments().getString("path"));
		buttons=(LinearLayout) getActivity().findViewById(R.id.buttons);
		p=getArguments().getInt("pos");
		uimode=Integer.parseInt(Sp.getString("uimode","0"));
		ListView vl=getListView();
		if(uimode==1){
		float scale = getResources().getDisplayMetrics().density;
		int dpAsPixels = (int) (10*scale + 0.5f);
	    vl.setPadding(dpAsPixels,0, dpAsPixels, 0);
	    vl.setDivider(null);
		vl.setDividerHeight(dpAsPixels);

		View divider=getActivity().getLayoutInflater().inflate(R.layout.divider,null);
		vl.addFooterView(divider);
		vl.addHeaderView(divider);
		vl.setHeaderDividersEnabled(true);
		vl.setFooterDividersEnabled(true);}
	    vl.setFastScrollEnabled(true);
		if(!orient){
		loadlist(f,false);orient=true;}
		
	
     	}
	

	    public void onListItemClicked(int position,View v) {
	    	if(results==true){
	    		String path =slist.get(position).getDesc();

	    		
	    		final File f = new File(path);
	    		if (f.isDirectory()) {
	    	
	    		loadlist(f,false);
	    		results=false;
	    		} else {utils.openFile(f,(MainActivity)getActivity());}
	    	}
	    	else if (selection == true)
			{
			adapter.toggleChecked(position);
			mActionMode.invalidate();
			if(adapter.getCheckedItemPositions().size()==0){selection = false;
			mActionMode.finish();
			mActionMode=null;}
		
		} else {
			
		String path =list.get(position).getDesc();

		
		final File f = new File(path);
		if (f.isDirectory()) {
		


		int index = getListView().getFirstVisiblePosition();
		View vi= getListView().getChildAt(0);
		int top = (vi == null) ? 0 : vi.getTop();
		Bundle b=new Bundle();
		b.putInt("index",index);
		b.putInt("top",top);
		scrolls.put(current,b);
		loadlist(f,false);
		} else {utils.openFile(f,(MainActivity)getActivity());}

		}
	}


	


	public void loadlist(File f,boolean back) {
		mMemoryCache.evictAll();
		new LoadListTask(back).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,(f));

	}
	@SuppressWarnings("unchecked")
	public void loadsearchlist(ArrayList<String> f) {

    new LoadSearchTask().execute(f);

	}
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	
	



	class LoadListTask extends AsyncTask<File, String, ArrayList<Layoutelements>> {

		private File f;
        boolean back;
		public LoadListTask(boolean back) {
this.back=back;
		}

		@Override
		protected void onPreExecute() {

		}
		@Override
		public void onProgressUpdate(String... message){
			Crouton.makeText(getActivity(), message[0], Style.ALERT).show();
		}

		@Override
		// Actual download method, run in the task thread
		protected ArrayList<Layoutelements> doInBackground(File... params) {
			// params comes from the execute() call: params[0] is the url.
            
			f = params[0];
            mFile.clear();
			try {	if(utils.canListFiles(f)){file = f.listFiles();	mFile.clear();
				for (int i = 0; i < file.length; i++) {
					mFile.add(file[i]);}}else{
						publishProgress(utils.getString(getActivity(),R.string.cantlistfiles));
						mFile=utils.toFileArray(RootCommands.listFiles(f.getPath(), true));
					}
				
			
			
				// String path=prog.getText().toString();
				Collections.sort(mFile,
						new FileListSorter(dsort,sortby,asc));

				list = addTo(mFile);

				
				return list;

			} catch (Exception e) {
				return null;
			}

		}

		@Override
		// Once the image is downloaded, associates it to the imageView
		protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
			if (isCancelled()) {
				bitmap = null;

			}
			try {
				if (bitmap != null) {
					adapter = new MyAdapter(getActivity(), R.layout.rowlayout,
							bitmap,ma);
				try{
					setListAdapter(adapter);
					
					}catch(Exception e){}
					results=false;
					current = f.getPath();
					if(back){
					if(scrolls.containsKey(current))
						{Bundle b=scrolls.get(current);
						
						getListView().setSelectionFromTop(b.getInt("index"),b.getInt("top"));
						}}
					try {
						Intent i=new Intent("updatepager");
						LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bbar(current);
				}
			} catch (Exception e) {
			}

		}
	}
	class LoadSearchTask extends AsyncTask<ArrayList<String>, Void, ArrayList<Layoutelements>> {

		private ArrayList<String> f;

		public LoadSearchTask() {

		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		// Actual download method, run in the task thread
		protected ArrayList<Layoutelements> doInBackground(ArrayList<String>... params) {
			// params comes from the execute() call: params[0] is the url.
           sFile=new ArrayList<File>();
			f = params[0];
			for(int i=0;i<f.size();i++){
				sFile.add(new File(f.get(i)));
			}
            
			try {
			
			
				// String path=prog.getText().toString();
				Collections.sort(sFile,
						new FileListSorter(dsort,sortby,asc));

				slist = addTo(sFile);

				
				return slist;

			} catch (Exception e) {
				return null;
			}

		}

		@Override
		// Once the image is downloaded, associates it to the imageView
		protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
			if (isCancelled()) {
				bitmap = null;

			}
			try {
				if (bitmap != null) {
					adapter = new MyAdapter(getActivity(), R.layout.rowlayout,
							bitmap,ma);
							try{
					setListAdapter(adapter);results=true;
								try {
									Intent i=new Intent("updatepager");
									LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}catch(Exception e){}		prog.setText(utils.getString(getActivity(),R.string.searchresults));
					buttons.setVisibility(View.GONE);
				
				}
			} catch (Exception e) {
			}

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
		public void initMenu(Menu menu){
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
			hideOption(R.id.openwith,menu);
			hideOption(R.id.ex,menu);
			mode.setTitle(utils.getString(getActivity(),R.string.select));
			
			return true;
		}

		// the following method is called each time
		// the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
           List<Integer> positions=adapter.getCheckedItemPositions();
			mode.setSubtitle(positions.size()+" "
							 + utils.getString(getActivity(),R.string.itemsselected));
			if (positions.size() == 1) {
				showOption(R.id.about, menu);
				showOption(R.id.rename, menu);
				File x=new File(list.get(adapter.getCheckedItemPositions().get(0))
								.getDesc());
				if (x.isDirectory()) {
					showOption(R.id.sethome, menu);
				} else if(x.getName().toLowerCase().endsWith(".zip") ||x.getName().toLowerCase().endsWith(".jar")|| x.getName().toLowerCase().endsWith(".apk")){
					showOption(R.id.ex,menu);
					hideOption(R.id.sethome,menu);
					showOption(R.id.share, menu);
				}else{hideOption(R.id.ex,menu);	hideOption(R.id.sethome, menu);	showOption(R.id.openwith,menu);showOption(R.id.share, menu);}
			}else{hideOption(R.id.about,menu);}
			return false; // Return false if nothing is done
		}

		// called when the user selects a contextual menu item
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		computeScroll();
			List<Integer> plist=adapter.getCheckedItemPositions();
			switch (item.getItemId()) {
			case R.id.sethome:
				int pos = plist.get(0);
				home = list.get(pos).getDesc();
				Crouton.makeText(getActivity(),
									 utils.getString(getActivity(),R.string.newhomedirectory)+ mFile.get(pos).getName(),
						Style.INFO).show();
				Sp.edit().putString("home", list.get(pos).getDesc()).apply();
				// the Action was executed, close the CAB
				mode.finish();
				return true;
			case R.id.about:
				utils.showProps(new File(list.get((plist.get(0))).getDesc()), getActivity());
				mode.finish();
				return true;
			case R.id.delete:
			    utils.deleteFiles(list,(MainActivity)getActivity(),plist);

				mode.finish();
				
				return true;
			case R.id.share:
				Intent i = new Intent();
				i.setAction(Intent.ACTION_SEND);
				i.setType("*/*");
				i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(list.get(plist.get(0)).getDesc())));
				startActivity(i);
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
					a.setTitle(utils.getString(getActivity(),R.string.rename));
				a.setPositiveButton(utils.getString(getActivity(),R.string.save),
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface p1, int p2) {
								boolean b = utils.rename(f, edit.getText()
										.toString());
								m.finish();
								mMemoryCache.evictAll();
								loadlist(new File(current),true);
								if (b) {
									Crouton.makeText(getActivity(),
											utils.getString(getActivity(),R.string.renamed),
											Style.CONFIRM).show();
								} else {
									Crouton.makeText(getActivity(),
													 utils.getString(getActivity(),R.string.renameerror)	,
											Style.ALERT).show();
								}
								// TODO: Implement this method
							}
						});
					a.setNegativeButton(utils.getString(getActivity(),R.string.cancel),
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface p1, int p2) {
								m.finish();
								// TODO: Implement this method
							}
						});
				a.show();
				mode.finish();
				return true;
				case R.id.book:
				for(int i1=0;i1<plist.size();i1++){
					try
					{
						sh.addS(new File(list.get(plist.get(i1)).getDesc()));
					
					}
					catch (Exception e)
					{}
				}	Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.bookmarksadded),Style.CONFIRM).show();
				mode.finish();
				return true;
				case R.id.ex: 
					Intent intent=new Intent(getActivity(),ExtractService.class);
					intent.putExtra("zip",list.get(
							(plist.get(0))).getDesc());
					getActivity().startService(intent);
					mode.finish();
				return true;
				case R.id.cpy:
				Intent intent1=new Intent("copy_path");
				ArrayList<String> copies=new ArrayList<String>();
				
					for(int i2=0;i2<plist.size();i2++){
						copies.add(list.get(plist.get(i2)).getDesc());
					}
					intent1.putExtra("copy_path",copies);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent1);
				mode.finish();
				return true;
		case R.id.cut:
				Intent intent2=new Intent("move_path");
				ArrayList<String> copie=new ArrayList<String>();
					for(int i3=0;i3<plist.size();i3++){
						copie.add(list.get(plist.get(i3)).getDesc());
					}
					intent2.putExtra("move_path",copie);
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent2);
				mode.finish();
				return true;
		case R.id.compress:
				ArrayList<String> copies1=new ArrayList<String>();
					for(int i4=0;i4<plist.size();i4++){
						copies1.add(list.get(plist.get(i4)).getDesc());
					}
				utils.showNameDialog((MainActivity)getActivity(),copies1,current);
					mode.finish();
			return 	true;
		case R.id.openwith:
			utils.openWith(new File(list.get(
					(plist.get(0))).getDesc()),getActivity());
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
	
		
		}
	};

	public void bbar(String text){
		try{
			buttons.removeAllViews();
			Bundle b=utils.getPaths(text,getActivity());
			ArrayList<String> names=	b.getStringArrayList("names");
			ArrayList<String> rnames=new ArrayList<String>();

			for(int i=names.size()-1;i>=0;i--){
				rnames.add(names.get(i));}

			ArrayList<String> paths=	b.getStringArrayList("paths");
			final	ArrayList<String> rpaths=new ArrayList<String>();

			for(int i=paths.size()-1;i>=0;i--){
				rpaths.add(paths.get(i));}
			for(int i=0;i<names.size();i++){
				final int index =i;
				if(rpaths.get(i).equals("/")){
					ImageButton ib=new ImageButton(getActivity());
					ib.setImageDrawable(icons.getRootDrawable());
					ib.setOnClickListener(new View.OnClickListener(){

							public void onClick(View p1)
							{loadlist(new File("/"),false);
								// TODO: Implement this method
							}
						});
			
					buttons.addView(ib);	
				}else if(rpaths.get(i).equals(Environment.getExternalStorageDirectory().getPath())){
					ImageButton ib=new ImageButton(getActivity());
					ib.setImageDrawable(icons.getSdDrawable());
					ib.setOnClickListener(new View.OnClickListener(){

							public void onClick(View p1)
							{loadlist(new File(rpaths.get(index)),true);
								// TODO: Implement this method
							}
						});
					
					buttons.addView(ib);	
				}
				else{
				Button button=new Button(getActivity());
				button.setText(rnames.get(index));
			
				button.setTextSize(13);

				//	button.setBackgroundDrawable(getResources().getDrawable(R.drawable.listitem));
				button.setOnClickListener(new Button.OnClickListener(){

						public void onClick(View p1)
						{loadlist(new File(rpaths.get(index)),true);
							//	Toast.makeText(getActivity(),rpaths.get(index),Toast.LENGTH_LONG).show();
							// TODO: Implement this method
						}
					});

			buttons.addView(button);
			}}
		}catch(NullPointerException e){System.out.println("button view not available");}
	}

	class BitmapWorkerTask extends AsyncTask<String, Bitmap, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private int data = 0;
		String path;

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage
			// collected
			imageViewReference = new WeakReference<ImageView>(imageView);
		}
		
		
		// Decode image in background.
		@Override
		protected Bitmap doInBackground(String... params) {
			path = params[0];
			
			Bitmap bitsat = null;
			if(!isCancelled()){	if (path.endsWith(".apk")) {
				try {
					PackageManager pm = getActivity().getPackageManager();
					PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
					// // the secret are these two lines....
					pi.applicationInfo.sourceDir = path;
					pi.applicationInfo.publicSourceDir = path;
					// //
					Drawable d = pi.applicationInfo.loadIcon(pm);

					Bitmap d1 = null;
					d1 = ((BitmapDrawable) d).getBitmap();

					addBitmapToMemoryCache(path, d1);
					bitsat = d1;
				} catch (Exception e) {
					Drawable apk = getResources().getDrawable(R.drawable.ic_doc_apk);
					Bitmap apk1 = ((BitmapDrawable) apk).getBitmap();
					bitsat = apk1;
				}
			} else if (Icons.isPicture(path)) {
				try {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
				    Bitmap b=	BitmapFactory.decodeFile(path, options);
					publishProgress(b);
					options.inSampleSize = utils.calculateInSampleSize(options,50, 50);

					// Decode bitmap with inSampleSize set
					options.inJustDecodeBounds = false;

					Bitmap bit = BitmapFactory.decodeFile(path, options);
					addBitmapToMemoryCache(path, bit);
					bitsat = bit;// decodeFile(path);//.createScaledBitmap(bits,imageViewReference.get().getHeight(),imageViewReference.get().getWidth(),true);
				} catch (Exception e) {
					Drawable img = getResources().getDrawable(R.drawable.ic_doc_image);
					Bitmap img1 = ((BitmapDrawable) img).getBitmap();
					bitsat = img1;
				}
			}}
			// TODO: Implement this method
			return bitsat;
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}

			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
				if (this == bitmapWorkerTask && imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}

	}

	public void loadBitmap(String path, ImageView imageView, Bitmap b) {
		if (cancelPotentialWork(path, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(
					getResources(), b, task);
			imageView.setImageDrawable(asyncDrawable);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,path);
		}
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	public static boolean cancelPotentialWork(String data, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final String bitmapData = bitmapWorkerTask.path;
			if (!bitmapData.equals(data)) {
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the ImageView, or an existing task was
		// cancelled
		return true;
	}
    public void computeScroll(){
        int index = getListView().getFirstVisiblePosition();
        View vi= getListView().getChildAt(0);
        int top = (vi== null) ? 0 : vi.getTop();
        Bundle b=new Bundle();
        b.putInt("index",index);
        b.putInt("top",top);
        scrolls.put(current,b);
    }
public void goBack(){
    File f=new File(current);
    loadlist(f.getParentFile(),true);
}
    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            computeScroll();
            loadlist(new File(current),true);
        }
    };
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}
public void getSortModes(){
	int t=Integer.parseInt(Sp.getString("sortby","0"));
	if(t<=2){sortby=t;asc=1;}
	else if(t>=3){asc=-1;sortby=t-3;}
	dsort=Integer.parseInt(Sp.getString("dirontop","0"));
	
	}
	@Override
	public void onResume(){
		super.onResume();
	
		(getActivity()).registerReceiver(receiver2, new IntentFilter("loadlist"));
	}
	@Override
	public void onPause(){
		super.onPause();
	
		(getActivity()).unregisterReceiver(receiver2);
	}
public ArrayList<Layoutelements> addTo(ArrayList<File> mFile){
	ArrayList<Layoutelements> a=new ArrayList<Layoutelements>();
	for (int i = 0; i < mFile.size(); i++) {
		if (mFile.get(i).isDirectory()) {
		a.add(utils.newElement(folder,mFile.get(i).getPath()));
			
		} else {
		   try{a.add(utils.newElement(Icons.loadMimeIcon(getActivity(),mFile.get(i).getPath()),mFile.get(i).getPath()));
		   }catch(Exception e){e.printStackTrace();Log.e("Amaze", mFile.get(i).getPath());}}
	}
	return a;
}




}
