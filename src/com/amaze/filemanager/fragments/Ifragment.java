package com.amaze.filemanager.fragments;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.database.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.adapters.*;
import com.amaze.filemanager.utils.*;
import java.io.*;
import java.lang.ref.*;
import java.util.*;

//import android.support.v4.app.ListFragment;

public class Ifragment extends android.support.v4.app.Fragment {
	Futils utils;
	String Ipath;
	TextView prog;
	GridView grid;
	ActionMode mActionMode=null;
	private LruCache<String, Bitmap> mMemoryCache;
	ArrayList<File> images;
	IAdapter adapter;
	boolean selection = false;
	ArrayList<Layoutelements> list;
	DisplayMetrics metrics;
	public int dpi;
	SharedPreferences Sp;
	   public int pix;
	   public int theme=0;
	  Ifragment frag=this;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.listview,
				container, false);
Sp=PreferenceManager.getDefaultSharedPreferences(getActivity());
theme=Integer.parseInt(Sp.getString("theme", "0"));
metrics=getResources().getDisplayMetrics();
	pix=metrics.widthPixels;
		switch(metrics.densityDpi){
			case 120:dpi=70;
			break;
			case 160:dpi=93;
			break;
			case 240:dpi=140;
			break;
			case 320:dpi=186;
			break;
			case 480:dpi=280;
			break;
			case 640:dpi=373;
		}
		utils = new Futils();
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
	   	final int cacheSize = maxMemory / 4;
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		Ipath = sharedPref.getString("Ipath", Environment
					.getExternalStorageDirectory().getPath()
					+ "/"
					+ Environment.DIRECTORY_DCIM);
	grid=(GridView)rootView.findViewById(R.id.grid);
	
	new LoadList().execute();
	
	 	grid.setFastScrollEnabled(true);
		grid.setBackgroundColor(Color.parseColor("#00000000"));
		//getListView().setDividerHeight(20);
		((LinearLayout) getActivity().findViewById(R.id.buttons))
				.setVisibility(View.GONE);
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmp) {
			
				return bitmp.getByteCount() / 1024;
			}
		}; 
	        return rootView;
				}



	

	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public void onLongItemClick(int position) {
		if (selection == false) {
			adapter.toggleChecked(position);
	mActionMode = getActivity().startActionMode(
								mActionModeCallback);
			selection = true;
		}
	}

	public void onListItemClick(int position) {
		// do something with the data
		if (selection == true) {
			adapter.toggleChecked(position);
			mActionMode.invalidate();
			if (adapter.getCheckedItemPositions().size() == 0) {
				selection = false;
				mActionMode.finish();
			}
		} else {
			utils.openunknown(new File(list.get(position).getDesc()), getActivity());
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	class LoadList extends AsyncTask<Void, Void, ArrayList<Layoutelements>> {

	

		public LoadList() {

		}

		protected ArrayList<Layoutelements> doInBackground(Void[] p1) {

//			cc = getActivity().getContentResolver().query(
//				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null,
//				null);
//			mUrls = new Uri[cc.getCount()];
//			for (int i = 0; i < cc.getCount(); i++) {
//				cc.moveToPosition(i);
//				mUrls[i] = Uri.parse(cc.getString(1));}
//		
images=utils.getImages(new File(Ipath));
			list	= new ArrayList<Layoutelements>();
			for (int i = 0; i < images.size(); i++) {

				Layoutelements e = new Layoutelements(getActivity()
						.getResources().getDrawable(R.drawable.ic_doc_image), images.get(i).getName() , images.get(i).getPath() );
				list.add(e);
			}
			// TODO: Implement this method
			return list;
		}

		@Override
		// Once the image is downloaded, associates it to the imageView
		protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}
			adapter = new IAdapter(getActivity(), R.layout.ifragment, bitmap,frag);
			grid.setAdapter(adapter);

		}

	}



	class BitmapWorkerTask extends AsyncTask<String, Bitmap, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;

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
			if (Icons.isPicture((path))) {
				try {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					Bitmap b = BitmapFactory.decodeFile(path, options);
                    publishProgress(b);
					options.inSampleSize = utils.calculateInSampleSize(options,
							dpi, dpi);

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
			}
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
					imageView.setLayoutParams(new FrameLayout.LayoutParams(2*dpi,2*dpi));
				}
			}
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

	public void loadBitmap(String path, ImageView imageView, Bitmap b) {
		if (cancelPotentialWork(path, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(
					getResources(), b, task);
			imageView.setImageDrawable(asyncDrawable);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,path);
		}
	}

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

	public static boolean cancelPotentialWork(String data, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final String bitmapData = bitmapWorkerTask.path;
			// If bitmapData is not yet set or it differs from the new data
			if (bitmapData.equals(null) || !bitmapData.equals(data)) {
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
	}	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		private void hideOption(int id, Menu menu) {
			MenuItem item = menu.findItem(id);
			item.setVisible(false);
		}

		private void showOption(int id, Menu menu) {
			MenuItem item = menu.findItem(id);
			item.setVisible(true);
		}

		// called when the action mode is created; startActionMode() was called
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();

			// assumes that you have "contexual.xml" menu resources
			inflater.inflate(R.menu.contextual, menu);

			mode.setTitle("Select Items");
		
			return true;
		}

		// the following method is called each time
		// the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

			mode.setSubtitle(adapter.getCheckedItemPositions().size()
							 + " Items Selected");
			hideOption(R.id.sethome, menu);
				hideOption(R.id.cpy, menu);
			hideOption(R.id.cut, menu);	
		
			hideOption(R.id.sethome, menu);
			hideOption(R.id.share, menu);
			hideOption(R.id.about, menu);
			hideOption(R.id.rename,menu);
			hideOption(R.id.sethome,menu);
			hideOption(R.id.ex, menu);
			hideOption(R.id.compress, menu);
			hideOption(R.id.openwith, menu);
			hideOption(R.id.book, menu);
			if (adapter.getCheckedItemPositions().size() > 1) {
				hideOption(R.id.share, menu);
			} else if (adapter.getCheckedItemPositions().size() == 1) {
				showOption(R.id.about, menu);
				showOption(R.id.rename,menu);
				showOption(R.id.share, menu);
			}
	
			return false; // Return false if nothing is done
		}

		// called when the user selects a contextual menu item
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			
				case R.id.about:
					utils.showProps(
						new File(list.get(
									 (adapter.getCheckedItemPositions().get(0)))
								 .getDesc()), getActivity());
					mode.finish();
					return true;
				case R.id.delete:
					// ArrayList<Boolean> bools=new ArrayList<Boolean>();
					for (int i = 0; i < adapter.getCheckedItemPositions().size(); i++) {
						boolean b = utils
							.deletefiles(new File(list.get(
													  adapter.getCheckedItemPositions().get(i))
												  .getDesc()));

					}

					mode.finish();
					adapter.notifyDataSetChanged();
					
                    new LoadList().execute();
					
					return true;
				case R.id.share:
					Intent i = new Intent();
					i.setAction(Intent.ACTION_SEND);
					i.setType("*/*");
					i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(list.get(
																			  adapter.getCheckedItemPositions().get(0)).getDesc())));
					startActivity(i);
					return true;
				case R.id.all:
					if (adapter.areAllChecked()) {
						adapter.toggleChecked(false);
						mode.finish();
					} else {
						adapter.toggleChecked(true);
						mode.invalidate();
					}
					

					return true;
				case R.id.rename:
					final ActionMode m = mode;
					final File f = new File(list.get(
												(adapter.getCheckedItemPositions().get(0))).getDesc());
					View dialog = getActivity().getLayoutInflater().inflate(
						R.layout.dialog, null);
					AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
					final EditText edit = (EditText) dialog
						.findViewById(R.id.newname);
					edit.setText(f.getName());
					a.setView(dialog);
					a.setTitle("Rename");
					a.setPositiveButton("Save",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface p1, int p2) {
								boolean b = utils.rename(f, edit.getText()
														 .toString());
								m.finish();
								new LoadList().execute();
								if (b) {
									Toast.makeText(getActivity(),
												   "Rename Successful",
												   Toast.LENGTH_SHORT).show();
								} else {
									Toast.makeText(getActivity(),
												   "Rename Unsuccessful",
												   Toast.LENGTH_SHORT).show();
								}
								// TODO: Implement this method
							}
						});
					a.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface p1, int p2) {
								m.finish();
								// TODO: Implement this method
							}
						});
					a.show();
					new LoadList().execute();
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
}
