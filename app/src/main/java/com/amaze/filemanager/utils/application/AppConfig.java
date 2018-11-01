/*
 * AppConfig.java
 *
 * Copyright (C) 2016-2018 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam <emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>
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
package com.amaze.filemanager.utils.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.widget.Toast;

import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.utils.LruBitmapCache;
import com.amaze.filemanager.utils.ScreenUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.lang.ref.WeakReference;

public class AppConfig extends GlideApplication {

    public static final String TAG = AppConfig.class.getSimpleName();

    private UtilitiesProvider utilsProvider;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private UtilsHandler mUtilsHandler;

    private static Handler mApplicationHandler = new Handler();
    private HandlerThread sBackgroundHandlerThread;
    private static Handler sBackgroundHandler;
    private WeakReference<Context> mainActivityContext;
    private static ScreenUtils screenUtils;

    private static AppConfig mInstance;

    public UtilitiesProvider getUtilsProvider() {
        return utilsProvider;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);//selector in srcCompat isn't supported without this
        sBackgroundHandlerThread = new HandlerThread("app_background");
        mInstance = this;

        utilsProvider = new UtilitiesProvider(this);
        mUtilsHandler = new UtilsHandler(this);

        //FIXME: in unit tests when AppConfig is rapidly created/destroyed this call will cause IllegalThreadStateException.
        //Until this gets fixed only one test case can be run in a time. - Raymond, 24/4/2018
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
     */
    public static void runInBackground(Runnable runnable) {
        synchronized (sBackgroundHandler) {
            sBackgroundHandler.post(runnable);
        }
    }

    /**
     * A compact AsyncTask which runs which executes whatever is passed by callbacks.
     * Supports any class that extends an object as param array, and result too.
     */
    public static <Params, Result> void runInParallel(final CustomAsyncCallbacks<Params, Result> customAsyncCallbacks) {

        synchronized (customAsyncCallbacks) {

            new AsyncTask<Params, Void, Result>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    customAsyncCallbacks.onPreExecute();
                }

                @Override
                protected Result doInBackground(Object... params) {
                    return customAsyncCallbacks.doInBackground();
                }

                @Override
                protected void onPostExecute(Result aVoid) {
                    super.onPostExecute(aVoid);
                    customAsyncCallbacks.onPostExecute(aVoid);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, customAsyncCallbacks.parameters);
        }
    }

    /**
     * Interface providing callbacks utilized by {@link #runInBackground(CustomAsyncCallbacks)}
     */
    public static abstract class CustomAsyncCallbacks<Params, Result> {
        public final @Nullable Params[] parameters;

        public CustomAsyncCallbacks(@Nullable Params[] params) {
            parameters = params;
        }

        public abstract Result doInBackground();

        public void onPostExecute(Result result) { }

        public void onPreExecute() { }
    }

    /**
     * Shows a toast message
     *
     * @param context Any context belonging to this application
     * @param message The message to show
     */
    public static void toast(Context context, @StringRes int message) {
        // this is a static method so it is easier to call,
        // as the context checking and casting is done for you

        if (context == null) return;

        if (!(context instanceof Application)) {
            context = context.getApplicationContext();
        }

        if (context instanceof Application) {
            final Context c = context;
            final @StringRes int m = message;

            ((AppConfig) context).runInApplicationThread(() -> {
                Toast.makeText(c, m, Toast.LENGTH_LONG).show();
            });
        }
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

    public ImageLoader getImageLoader() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        if (mImageLoader == null) {
            this.mImageLoader = new ImageLoader(mRequestQueue, new LruBitmapCache());
        }
        return mImageLoader;
    }

    public UtilsHandler getUtilsHandler() {
        return mUtilsHandler;
    }

    public void setMainActivityContext(@NonNull Activity activity) {
        mainActivityContext = new WeakReference<>(activity);
        screenUtils = new ScreenUtils(activity);
    }

    public ScreenUtils getScreenUtils(){
        return screenUtils;
    }

    @Nullable
    public Context getMainActivityContext() {
        return mainActivityContext.get();
    }

}
