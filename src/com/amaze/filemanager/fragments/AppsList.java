package com.amaze.filemanager.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.util.LruCache;
import android.view.ActionMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.AppsAdapter;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.AppsSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppsList extends ListFragment {
    ArrayList<File> mFile = new ArrayList<File>();
    Futils utils = new Futils();
    AppsList app = this;
    AppsAdapter adapter;
    public int uimode;
    SharedPreferences Sp;
    public boolean selection = false;
    public ActionMode mActionMode;
    public ArrayList<ApplicationInfo> c = new ArrayList<ApplicationInfo>();
    private LruCache<String, Bitmap> mMemoryCache;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().findViewById(R.id.fabbutton).setVisibility(View.GONE);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        getActivity().findViewById(R.id.buttonbarframe).setVisibility(View.GONE);
        final int cacheSize = maxMemory / 4;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

        };ImageButton overflow=(ImageButton)getActivity().findViewById(R.id.action_overflow);
        overflow.setVisibility(View.GONE);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        ListView vl = getListView();
        if (uimode == 1) {
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (5 * scale + 0.5f);
            vl.setPadding(dpAsPixels, 0, dpAsPixels, 0);
            vl.setDivider(null);
        }
        vl.setFastScrollEnabled(true);
        new LoadListTask().execute();
    }

    public void onLongItemClick(final int position) {
        AlertDialog.Builder d = new AlertDialog.Builder(getActivity());
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(
                getActivity(), android.R.layout.select_dialog_item);
        adapter1.add(utils.getString(getActivity(), R.string.backup));
        adapter1.add(utils.getString(getActivity(), R.string.uninstall));
        adapter1.add(utils.getString(getActivity(), R.string.properties));
        adapter1.add(utils.getString(getActivity(), R.string.play));
        d.setAdapter(adapter1, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                switch (p2) {
                    case 0:
                        Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.copyingapk) + Environment.getExternalStorageDirectory().getPath() + "/app_backup", Toast.LENGTH_LONG).show();
                        ApplicationInfo info = c.get(position);
                        File f = new File(info.publicSourceDir);
                        ArrayList<String> a = new ArrayList<String>();
                        a.add(info.publicSourceDir);
                        File dst = new File(Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                        Intent intent = new Intent(getActivity(), CopyService.class);
                        intent.putExtra("FILE_PATHS", a);
                        intent.putExtra("COPY_DIRECTORY", dst.getPath());
                        getActivity().startService(intent);
                        break;
                    case 1:
                        unin(c.get(position).packageName);
                        break;
                    case 2:
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + c.get(position).packageName)));
                        break;
                    case 3:
                        Intent intent1 = new Intent(Intent.ACTION_VIEW);
                        intent1.setData(Uri.parse("market://details?id=" + c.get(position).packageName));
                        startActivity(intent1);
                        break;
                }
                // TODO: Implement this method
            }
        });
        d.show();
    }

    class LoadListTask extends AsyncTask<Void, Void, ArrayList<Layoutelements>> {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();

        protected ArrayList<Layoutelements> doInBackground(Void[] p1) {
            try {
                List<ApplicationInfo> all_apps = getActivity().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);


                for (ApplicationInfo object : all_apps) {


                    c.add(object);


                }
                Collections.sort(c, new AppsSorter(getActivity().getPackageManager()));
                for (int i = 0; i < c.size(); i++) {


                    a.add(new Layoutelements(getActivity().getResources().getDrawable(R.drawable.ic_doc_apk), c.get(i).loadLabel(getActivity().getPackageManager()).toString(), c.get(i).publicSourceDir,"","",""));

                    File file = new File(c.get(i).publicSourceDir);
                    mFile.add(file);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "" + e, Toast.LENGTH_LONG).show();
            }//ArrayAdapter<String> b=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,a);
            // TODO: Implement this method

            return a;
        }


        public LoadListTask() {

        }

        @Override
        protected void onPreExecute() {

        }


        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
            if (isCancelled()) {
                bitmap = null;

            }
            try {
                if (bitmap != null) {


                    adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, bitmap, app);
                    setListAdapter(adapter);

                }
            } catch (Exception e) {
            }

        }
    }  // copy the .apk file to wherever

    class BitmapWorkerTask extends AsyncTask<ApplicationInfo, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        ApplicationInfo path;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage
            // collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(ApplicationInfo... params) {
            path = params[0];
            Bitmap b = ((BitmapDrawable) path.loadIcon(getActivity().getPackageManager())).getBitmap();
            addBitmapToMemoryCache(path.publicSourceDir, b);
            // TODO: Implement this method
            return b;
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

    public void loadBitmap(ApplicationInfo path, ImageView imageView, Bitmap b) {
        if (cancelPotentialWork(path, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(
                    getResources(), b, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(path);
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

    public static boolean cancelPotentialWork(ApplicationInfo data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final ApplicationInfo bitmapData = bitmapWorkerTask.path;
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
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public void unin(String pkg) {

        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "" + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }
}
