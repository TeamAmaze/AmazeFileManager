package com.amaze.filemanager.utils.application;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.widget.Toast;

import com.amaze.filemanager.utils.LruBitmapCache;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by vishal on 7/12/16 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */

public class AppConfig extends LeakCanaryApplication {

    public static final String TAG = AppConfig.class.getSimpleName();

    private UtilitiesProviderInterface utilsProvider;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Handler mApplicationHandler = new Handler();
    private static HandlerThread sBackgroundHandlerThread = new HandlerThread("app_background");
    private static Handler sBackgroundHandler;

    private static AppConfig mInstance;

    public UtilitiesProviderInterface getUtilsProvider() {
        return utilsProvider;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);//selector in srcCompat isn't supported without this
        mInstance = this;

        utilsProvider = new UtilitiesProvider(this);

        sBackgroundHandlerThread.start();
        sBackgroundHandler = new Handler(sBackgroundHandlerThread.getLooper());

        // disabling file exposure method check for api n+
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        sBackgroundHandlerThread.quit();
    }

    /**
     * Post a runnable to handler. Use this in case we don't have any restriction to execute after
     * this runnable is executed, and {@link #runInBackground(CustomAsyncCallbacks)} in case we need
     * to execute something after execution in background
     * @param runnable
     */
    public static void runInBackground(Runnable runnable) {
        synchronized (sBackgroundHandler) {
            sBackgroundHandler.post(runnable);
        }
    }

    /**
     * A compact AsyncTask which runs which executes whatever is passed by callbacks.
     * Supports any class that extends an object as param array, and result too.
     * @param customAsyncCallbacks
     */
    public static void runInBackground(final CustomAsyncCallbacks customAsyncCallbacks) {

        synchronized (customAsyncCallbacks) {

            new AsyncTask<Object, Object, Object>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    customAsyncCallbacks.onPreExecute();
                }

                @Override
                protected void onProgressUpdate(Object... values) {
                    super.onProgressUpdate(values);
                    customAsyncCallbacks.publishResult(values);
                }

                @Override
                protected Void doInBackground(Object... params) {
                    return customAsyncCallbacks.doInBackground();
                }

                @Override
                protected void onPostExecute(Object aVoid) {
                    super.onPostExecute(aVoid);
                    customAsyncCallbacks.onPostExecute(aVoid);
                }
            }.execute(customAsyncCallbacks.params());
        }
    }

    /**
     * Interface providing callbacks utilized by {@link #runInBackground(CustomAsyncCallbacks)}
     */
    public interface CustomAsyncCallbacks {

        <E extends Object> E doInBackground();

        Void onPostExecute(Object result);

        Void onPreExecute();

        Void publishResult(Object... result);

        <T extends Object> T[] params();
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

            ((AppConfig) context).runInApplicationThread(() -> {
                Toast.makeText(c, m, Toast.LENGTH_LONG).show();
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
