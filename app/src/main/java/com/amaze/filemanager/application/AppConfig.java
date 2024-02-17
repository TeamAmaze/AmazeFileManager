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

package com.amaze.filemanager.application;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.CoreConfiguration;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.data.StringFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.crashreport.AcraReportSenderFactory;
import com.amaze.filemanager.crashreport.ErrorActivity;
import com.amaze.filemanager.database.ExplorerDatabase;
import com.amaze.filemanager.database.UtilitiesDatabase;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.ScreenUtils;
import com.amaze.trashbin.TrashBin;
import com.amaze.trashbin.TrashBinConfig;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import jcifs.Config;
import jcifs.smb.SmbException;

@AcraCore(
    buildConfigClass = BuildConfig.class,
    reportSenderFactoryClasses = AcraReportSenderFactory.class)
public class AppConfig extends GlideApplication {

  private Logger log = null;

  private UtilitiesProvider utilsProvider;
  private UtilsHandler utilsHandler;

  private WeakReference<Context> mainActivityContext;
  private static ScreenUtils screenUtils;

  private static AppConfig instance;

  private UtilitiesDatabase utilitiesDatabase;

  private ExplorerDatabase explorerDatabase;

  private TrashBinConfig trashBinConfig;
  private TrashBin trashBin;
  private static final String TRASH_BIN_BASE_PATH =
      Environment.getExternalStorageDirectory().getPath() + File.separator + ".AmazeData";

  public UtilitiesProvider getUtilsProvider() {
    return utilsProvider;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(
        true); // selector in srcCompat isn't supported without this
    instance = this;

    CustomSshJConfig.init();
    explorerDatabase = ExplorerDatabase.initialize(this);
    utilitiesDatabase = UtilitiesDatabase.initialize(this);

    utilsProvider = new UtilitiesProvider(this);
    utilsHandler = new UtilsHandler(this, utilitiesDatabase);

    runInBackground(Config::registerSmbURLHandler);

    // disabling file exposure method check for api n+
    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
    StrictMode.setVmPolicy(builder.build());
    log = LoggerFactory.getLogger(AppConfig.class);
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    initACRA();
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
  }

  /**
   * Post a runnable to handler. Use this in case we don't have any restriction to execute after
   * this runnable is executed, and {@link #runInBackground(Runnable)} in case we need to execute
   * something after execution in background
   */
  public void runInBackground(Runnable runnable) {
    Completable.fromRunnable(runnable).subscribeOn(Schedulers.io()).subscribe();
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

      getInstance().runInApplicationThread(() -> Toast.makeText(c, m, Toast.LENGTH_LONG).show());
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

      getInstance().runInApplicationThread(() -> Toast.makeText(c, m, Toast.LENGTH_LONG).show());
    }
  }

  /**
   * Run a {@link Runnable} in the main application thread
   *
   * @param r {@link Runnable} to run
   */
  public void runInApplicationThread(@NonNull Runnable r) {
    Completable.fromRunnable(r).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
  }

  /**
   * Convenience method to run a {@link Callable} in the main application thread. Use when the
   * callable's return value is not processed.
   *
   * @param c {@link Callable} to run
   */
  public void runInApplicationThread(@NonNull Callable<Void> c) {
    Completable.fromCallable(c).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
  }

  public static synchronized AppConfig getInstance() {
    return instance;
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

  public UtilitiesDatabase getUtilitiesDatabase() {
    return utilitiesDatabase;
  }

  /**
   * Called in {@link #attachBaseContext(Context)} after calling the {@code super} method. Should be
   * overridden if MultiDex is enabled, since it has to be initialized before ACRA.
   */
  protected void initACRA() {
    if (ACRA.isACRASenderServiceProcess()) {
      return;
    }

    try {
      final CoreConfiguration acraConfig =
          new CoreConfigurationBuilder(this)
              .setBuildConfigClass(BuildConfig.class)
              .setReportFormat(StringFormat.JSON)
              .setSendReportsInDevMode(true)
              .setEnabled(true)
              .build();
      ACRA.init(this, acraConfig);
    } catch (final ACRAConfigurationException ace) {
      if (log != null) {
        log.warn("failed to initialize ACRA", ace);
      }
      ErrorActivity.reportError(
          this,
          ace,
          null,
          ErrorActivity.ErrorInfo.make(
              ErrorActivity.ERROR_UNKNOWN,
              "Could not initialize ACRA crash report",
              R.string.app_ui_crash));
    }
  }

  public TrashBin getTrashBinInstance() {
    if (trashBin == null) {
      trashBin =
          new TrashBin(
              getApplicationContext(),
              true,
              getTrashBinConfig(),
              s -> {
                runInBackground(
                    () -> {
                      HybridFile file = new HybridFile(OpenMode.TRASH_BIN, s);
                      try {
                        file.delete(getMainActivityContext(), false);
                      } catch (ShellNotRunningException | SmbException e) {
                        log.warn("failed to delete file in trash bin cleanup", e);
                      }
                    });
                return true;
              },
              null);
    }
    return trashBin;
  }

  private TrashBinConfig getTrashBinConfig() {
    if (trashBinConfig == null) {
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

      int days =
          sharedPrefs.getInt(
              PreferencesConstants.KEY_TRASH_BIN_RETENTION_DAYS,
              TrashBinConfig.RETENTION_DAYS_INFINITE);
      long bytes =
          sharedPrefs.getLong(
              PreferencesConstants.KEY_TRASH_BIN_RETENTION_BYTES,
              TrashBinConfig.RETENTION_BYTES_INFINITE);
      int numOfFiles =
          sharedPrefs.getInt(
              PreferencesConstants.KEY_TRASH_BIN_RETENTION_NUM_OF_FILES,
              TrashBinConfig.RETENTION_NUM_OF_FILES);
      int intervalHours =
          sharedPrefs.getInt(
              PreferencesConstants.KEY_TRASH_BIN_CLEANUP_INTERVAL_HOURS,
              TrashBinConfig.INTERVAL_CLEANUP_HOURS);
      trashBinConfig =
          new TrashBinConfig(
              TRASH_BIN_BASE_PATH, days, bytes, numOfFiles, intervalHours, false, true);
    }
    return trashBinConfig;
  }
}
