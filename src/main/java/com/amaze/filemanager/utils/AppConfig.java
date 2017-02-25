package com.amaze.filemanager.utils;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.StrictMode;
import android.text.TextUtils;
import android.widget.Toast;

import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by vishal on 7/12/16.
 */

public class AppConfig extends Application {

    public static final String TAG = AppConfig.class.getSimpleName();

    private UtilitiesProviderInterface utilsProvider;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Handler mApplicationHandler = new Handler();

    private static AppConfig mInstance;

    public UtilitiesProviderInterface getUtilsProvider() {
        return utilsProvider;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        utilsProvider = new UtilitiesProvider(this);

        // disabling file exposure method check for api n+
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    /**
     * Shows a toast message
     *
     * @param context Any context belonging to this application
     * @param message The message to show
     */
    public static void toast(Context context, String message) {
        // this is a static method so it is easier to call,
        // as the context checking and casting is done for you

        if (context == null) return;

        if (!(context instanceof Application)) {
            context = context.getApplicationContext();
        }

        if (context instanceof Application) {
            final Context c = context;
            final String m = message;

            ((AppConfig) context).runInApplicationThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(c, m, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Run a runnable in the main application thread
     *
     * @param r Runnable to run
     */
    public void runInApplicationThread(Runnable r) {
        mApplicationHandler.post(r);
    }

    public static synchronized AppConfig getInstance() {
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public ImageLoader getImageLoader() {
        getRequestQueue();
        if (mImageLoader == null) {
            this.mImageLoader = new ImageLoader(mRequestQueue, new LruBitmapCache());
        }
        return mImageLoader;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
