/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>
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

package com.amaze.filemanager.utils;


import android.content.*;
import android.content.pm.*;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.ThumbnailUtils;
import android.os.*;
import android.provider.MediaStore;
import android.widget.*;

import com.amaze.filemanager.R;

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
    boolean grid;
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
    public IconHolder(Context context, boolean useThumbs,boolean grid) {
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
        this.grid=grid;
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
            return;
        }

        // Is cached?
        final String filePath = fso.getPath();
        if (this.mAppIcons.containsKey(filePath)) {
            iconView.setImageBitmap(this.mAppIcons.get(filePath));
            return;
        }

        mRequests.put(iconView, fso);
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

            try {
                if (Icons.isApk(filePath)) {
                    return getAppDrawable(fso);
                }else if(Icons.isPicture(filePath)){
                return	loadImage(fso.getPath());
                }else if(Icons.isVideo(filePath))
                    return getVideoDrawable(fso);
            } catch (OutOfMemoryError outOfMemoryError) {
               cleanup();
                shutdownWorker();
            }

            return null;
        }
    private Bitmap getVideoDrawable(File fso) throws OutOfMemoryError{
        String path = fso.getPath();
        try {
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Images.Thumbnails.MINI_KIND);
            return thumb;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }/**
         * Method that returns the main icon of the app
         *
         * @param fso The FileSystemObject
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Bitmap getAppDrawable(File fso) throws OutOfMemoryError{
            String path=fso.getPath();
            Bitmap bitsat;
            try {
                PackageManager pm = mContext.getPackageManager();
                PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
                // // the secret are these two lines....
                pi.applicationInfo.sourceDir = path;
                pi.applicationInfo.publicSourceDir = path;
                // //
                Drawable d = pi.applicationInfo.loadIcon(pm);

                Bitmap d1 = null;
                d1 = ((BitmapDrawable) d).getBitmap();
                bitsat = d1;
            } catch (Exception e) {
                Drawable apk =mContext. getResources().getDrawable(R.drawable.ic_doc_apk);
                Bitmap apk1 = ((BitmapDrawable) apk).getBitmap();
                bitsat = apk1;
            }
        return bitsat;
        }


		public Bitmap loadImage(String path) throws OutOfMemoryError{
			Bitmap bitsat;
            Resources res=mContext.getResources();
            int dp=50;
            if(grid){dp=150;}
            int px = (int)(dp * (res.getDisplayMetrics().densityDpi / 160));
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				Bitmap b = BitmapFactory.decodeFile(path, options);
				
				options.inSampleSize = new Futils().calculateInSampleSize(options, px, px);

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

