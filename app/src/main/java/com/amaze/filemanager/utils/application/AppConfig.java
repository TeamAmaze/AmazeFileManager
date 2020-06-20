/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

import java.lang.ref.WeakReference;

import com.amaze.filemanager.database.ExplorerDatabase;
import com.amaze.filemanager.database.UtilitiesDatabase;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig;
import com.amaze.filemanager.utils.LruBitmapCache;
import com.amaze.filemanager.utils.ScreenUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

public class AppConfig extends GlideApplication {

  public static final String TAG = AppConfig.class.getSimpleName();

  private UtilitiesProvider utilsProvider;
  private RequestQueue requestQueue;
  private ImageLoader imageLoader;
  private UtilsHandler utilsHandler;

  private static Handler applicationhandler = new Handler();
  private HandlerThread backgroundHandlerThread;
  private static Handler backgroundHandler;
  private WeakReference<Context> mainActivityContext;
  private static ScreenUtils screenUtils;

  private static AppConfig instance;

  public UtilitiesProvider getUtilsProvider() {
    return utilsProvider;
  }

  private ExplorerDatabase explorerDatabase;
  private UtilitiesDatabase utilitiesDatabase;

  @Override
  public void onCreate() {
    super.onCreate();
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(
        true); // selector in srcCompat isn't supported without this
    backgroundHandlerThread = new HandlerThread("app_background");
    instance = this;

    CustomSshJConfig.init();
    explorerDatabase = ExplorerDatabase.initialize(this);
    utilitiesDatabase = UtilitiesDatabase.initialize(this);

    utilsProvider = new UtilitiesProvider(this);
    utilsHandler = new UtilsHandler(this, utilitiesDatabase);

    // FIXME: in unit tests when AppConfig is rapidly created/destroyed this call will cause
    // IllegalThreadStateException.
    // Until this gets fixed only one test case can be run in a time. - Raymond, 24/4/2018
    backgroundHandlerThread.start();
    backgroundHandler = new Handler(backgroundHandlerThread.getLooper());

    // disabling file exposure method check for api n+
    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
    StrictMode.setVmPolicy(builder.build());
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    backgroundHandlerThread.quit();
  }

  /**
   * Post a runnable to handler. Use this in case we don't have any restriction to execute after
   * this runnable is executed, and {@link #runInBackground(Runnable)} in case we need to execute
   * something after execution in background
   */
  public static void runInBackground(Runnable runnable) {
    synchronized (backgroundHandler) {
      backgroundHandler.post(runnable);
    }
  }

  /**
   * A compact AsyncTask which runs which executes whatever is passed by callbacks. Supports any
   * class that extends an object as param array, and result too.
   */
  public static <Params, Result> void runInParallel(
      final CustomAsyncCallbacks<Params, Result> customAsyncCallbacks) {

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

  /** Interface providing callbacks utilized by {@link #runInBackground(Runnable)} */
  public abstract static class CustomAsyncCallbacks<Params, Result> {
    public final @Nullable Params[] parameters;

    public CustomAsyncCallbacks(@Nullable Params[] params) {
      parameters = params;
    }

    public abstract Result doInBackground();

    public void onPostExecute(Result result) {}

    public void onPreExecute() {}
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

      ((AppConfig) context)
          .runInApplicationThread(
              () -> {
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

      ((AppConfig) context)
          .runInApplicationThread(
              () -> {
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
    applicationhandler.post(r);
  }

  public static synchronized AppConfig getInstance() {
    return instance;
  }

  public ImageLoader getImageLoader() {
    if (requestQueue == null) {
      requestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    if (imageLoader == null) {
      this.imageLoader = new ImageLoader(requestQueue, new LruBitmapCache());
    }
    return imageLoader;
  }

  public UtilsHandler getUtilsHandler() {
    return utilsHandler;
  }

  public void setMainActivityContext(@NonNull Activity activity) {
    mainActivityContext = new WeakReference<>(activity);
    screenUtils = new ScreenUtils(activity);
  }

  public ScreenUtils getScreenUtils() {
    return screenUtils;
  }

  @Nullable
  public Context getMainActivityContext() {
    return mainActivityContext.get();
  }

  public ExplorerDatabase getExplorerDatabase() {
    return explorerDatabase;
  }
}
