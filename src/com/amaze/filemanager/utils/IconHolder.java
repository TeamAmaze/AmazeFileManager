package com.amaze.filemanager.utils;


import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.widget.*;
import com.amaze.filemanager.*;
import java.io.*;
import java.util.*;

/**
 * A class that holds icons for a more efficient access.
 */
public class IconHolder {

    private static final int MAX_CACHE = 500;

    private static final int MSG_LOAD = 1;
    private static final int MSG_LOADED = 2;
    private static final int MSG_DESTROY = 3;

    private final Map<String, Bitmap> mIcons;     // Themes based
    private final Map<String, Bitmap> mAppIcons;  // App based

    private Map<String, Long> mAlbums;      // Media albums

    private Map<ImageView, File> mRequests;

    private final Context mContext;
    private final boolean mUseThumbs; 
	private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private static class LoadResult {
        File fso;
        Bitmap result;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOADED:
                    processResult((LoadResult) msg.obj);
                    sendEmptyMessageDelayed(MSG_DESTROY, 3000);
                    break;
                case MSG_DESTROY:
                    shutdownWorker();
                    break;
            }
        }

        private void processResult(LoadResult result) {
            // Cache the new drawable
            final String filePath =(result.fso.getPath());
            mAppIcons.put(filePath, result.result);

            // find the request for it
            for (Map.Entry<ImageView, File> entry : mRequests.entrySet()) {
                final ImageView imageView = entry.getKey();
                final File fso = entry.getValue();
                if (fso == result.fso) {
                    imageView.setImageBitmap(result.result);
                    mRequests.remove(imageView);
                    break;
                }
            }
        }
    };

    /**
     * Constructor of <code>IconHolder</code>.
     *
     * @param useThumbs If thumbs of images, videos, apps, ... should be returned
     * instead of the default icon.
     */
    public IconHolder(Context context, boolean useThumbs) {
        super();
        this.mContext = context;
        this.mUseThumbs = useThumbs;
        this.mRequests = new HashMap<ImageView, File>();
        this.mIcons = new HashMap<String,Bitmap>();
        this.mAppIcons = new LinkedHashMap<String, Bitmap>(MAX_CACHE, .75F, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
                return size() > MAX_CACHE;
            }
        };
        this.mAlbums = new HashMap<String, Long>();
      
    }

    /**
     * Method that returns a drawable reference of a icon.
     *
     * @param resid The resource identifier
     * @return Drawable The drawable icon reference
     */

    /**
     * Method that returns a drawable reference of a FileSystemObject.
     *
     * @param iconView View to load the drawable into
     * @param fso The FileSystemObject reference
     * @param defaultIcon Drawable to be used in case no specific one could be found
     * @return Drawable The drawable reference
     */
    public void loadDrawable(ImageView iconView, File fso, Drawable defaultIcon) {
        if (!mUseThumbs) {
            iconView.setImageDrawable(defaultIcon);
            return;
        }

        // Is cached?
        final String filePath = fso.getPath();
        if (this.mAppIcons.containsKey(filePath)) {
            iconView.setImageBitmap(this.mAppIcons.get(filePath));
            return;
        }

        mRequests.put(iconView, fso);
        iconView.setImageDrawable(defaultIcon);

        mHandler.removeMessages(MSG_DESTROY);
        if (mWorkerThread == null) {
            mWorkerThread = new HandlerThread("IconHolderLoader");
            mWorkerThread.start();
            mWorkerHandler = new WorkerHandler(mWorkerThread.getLooper());
        }
        Message msg = mWorkerHandler.obtainMessage(MSG_LOAD, fso);
        msg.sendToTarget();
    }

    /**
     * Cancel loading of a drawable for a certain ImageView.
     */
    public void cancelLoad(ImageView view) {
        File fso = mRequests.get(view);
        if (fso != null && mWorkerHandler != null) {
            mWorkerHandler.removeMessages(MSG_LOAD, fso);
        }
        mRequests.remove(view);
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD:
                    File fso = (File) msg.obj;
                    Bitmap d = loadDrawable(fso);
                    if (d != null) {
                        LoadResult result = new LoadResult();
                        result.fso = fso;
                        result.result = d;
                        mHandler.obtainMessage(MSG_LOADED, result).sendToTarget();
                    }
                    break;
            }
        }
}
        private Bitmap loadDrawable(File fso) {
            final String filePath = (fso.getPath());

            if (Icons.isApk(filePath)) {
                return getAppDrawable(fso);
            }else if(Icons.isPicture(filePath)){
			return	loadImage(fso.getPath());
			}

            return null;
        }

        /**
         * Method that returns the main icon of the app
         *
         * @param fso The FileSystemObject
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Bitmap getAppDrawable(File fso) {
            final String filepath = fso.getPath();
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath,
															   PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                // Read http://code.google.com/p/android/issues/detail?id=9151, CM fixed this
                // issue. We retain it for compatibility with older versions and roms without
                // this fix. Required to access apk which are not installed.
                final ApplicationInfo appInfo = packageInfo.applicationInfo;
                appInfo.sourceDir = filepath;
                appInfo.publicSourceDir = filepath;
                return ((BitmapDrawable)pm.getDrawable(appInfo.packageName, appInfo.icon, appInfo)).getBitmap();
            }
            return null;
        }


		public Bitmap loadImage(String path){
			Bitmap bitsat;
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				Bitmap b = BitmapFactory.decodeFile(path, options);
				
				options.inSampleSize = new Futils().calculateInSampleSize(options, 50, 50);

				// Decode bitmap with inSampleSize set
				options.inJustDecodeBounds = false;

				Bitmap bit = BitmapFactory.decodeFile(path, options);
				
				bitsat = bit;// decodeFile(path);//.createScaledBitmap(bits,imageViewReference.get().getHeight(),imageViewReference.get().getWidth(),true);
			} catch (Exception e) {
				Drawable img = mContext.getResources().getDrawable(R.drawable.ic_doc_image);
				Bitmap img1 = ((BitmapDrawable) img).getBitmap();
				bitsat = img1;
			}return bitsat;
		}

        /**
         * Method that returns a thumbnail of the album folder
         *
         * @param albumId The album identifier
         * @return Drawable The drawable or null if cannot be extracted
         */
      

    /**
     * Shut down worker thread
     */
    private void shutdownWorker() {
        if (mWorkerThread != null) {
            mWorkerThread.getLooper().quit();
            mWorkerHandler = null;
            mWorkerThread = null;
        }
    }

    /**
     * Free any resources used by this instance
     */
    public void cleanup() {
        this.mRequests.clear();
        this.mIcons.clear();
        this.mAppIcons.clear();
       
        shutdownWorker();
    }
}

