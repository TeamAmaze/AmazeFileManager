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

package com.amaze.filemanager.asynchronous.loaders;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.adapters.data.AppDataParcelable;
import com.amaze.filemanager.adapters.data.AppDataSorter;
import com.amaze.filemanager.asynchronous.broadcast_receivers.PackageReceiver;
import com.amaze.filemanager.utils.InterestingConfigChange;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.Formatter;

import androidx.loader.content.AsyncTaskLoader;

/**
 * Created by vishal on 23/2/17.
 *
 * <p>Class loads all the packages installed
 */
public class AppListLoader extends AsyncTaskLoader<List<AppDataParcelable>> {

  private final Logger LOG = LoggerFactory.getLogger(AppListLoader.class);

  private PackageManager packageManager;
  private PackageReceiver packageReceiver;
  private List<AppDataParcelable> mApps;
  private final int sortBy;
  private final boolean isAscending;

  public AppListLoader(Context context, int sortBy, boolean isAscending) {
    super(context);

    this.sortBy = sortBy;
    this.isAscending = isAscending;

    /*
     * using global context because of the fact that loaders are supposed to be used
     * across fragments and activities
     */
    packageManager = getContext().getPackageManager();
  }

  @Override
  public List<AppDataParcelable> loadInBackground() {
    List<ApplicationInfo> apps =
        packageManager.getInstalledApplications(
            PackageManager.MATCH_UNINSTALLED_PACKAGES
                | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS);

    if (apps == null) return Collections.emptyList();
    mApps = new ArrayList<>(apps.size());
    PackageInfo androidInfo = null;
    try {
      androidInfo = packageManager.getPackageInfo("android", PackageManager.GET_SIGNATURES);
    } catch (PackageManager.NameNotFoundException e) {
      LOG.warn("faield to find android package name while loading apps list", e);
    }

    for (ApplicationInfo object : apps) {
      if (object.sourceDir == null) {
        continue;
      }
      File sourceDir = new File(object.sourceDir);

      String label = object.loadLabel(packageManager).toString();
      PackageInfo info;

      try {
        info = packageManager.getPackageInfo(object.packageName, PackageManager.GET_SIGNATURES);
      } catch (PackageManager.NameNotFoundException e) {
        LOG.warn("faield to find package name {} while loading apps list", object.packageName, e);
        info = null;
      }
      boolean isSystemApp = isAppInSystemPartition(object) || isSignedBySystem(info, androidInfo);

      List<String> splitPathList = null;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
          && object.splitPublicSourceDirs != null) {
        splitPathList = Arrays.asList(object.splitPublicSourceDirs);
      }

      AppDataParcelable elem =
          new AppDataParcelable(
              label == null ? object.packageName : label,
              object.sourceDir,
              splitPathList,
              object.packageName,
              object.flags + "_" + (info != null ? info.versionName : ""),
              Formatter.formatFileSize(getContext(), sourceDir.length()),
              sourceDir.length(),
              sourceDir.lastModified(),
              isSystemApp,
              null);

      mApps.add(elem);
    }

    Collections.sort(mApps, new AppDataSorter(sortBy, isAscending));
    return mApps;
  }

  @Override
  public void deliverResult(List<AppDataParcelable> data) {
    if (isReset()) {

      if (data != null) onReleaseResources(data); // TODO onReleaseResources() is empty
    }

    // preserving old data for it to be closed
    List<AppDataParcelable> oldData = mApps;
    mApps = data;
    if (isStarted()) {
      // loader has been started, if we have data, return immediately
      super.deliverResult(mApps);
    }

    // releasing older resources as we don't need them now
    if (oldData != null) {
      onReleaseResources(oldData); // TODO onReleaseResources() is empty
    }
  }

  @Override
  protected void onStartLoading() {

    if (mApps != null) {
      // we already have the results, load immediately
      deliverResult(mApps);
    }

    if (packageReceiver != null) {
      packageReceiver = new PackageReceiver(this);
    }

    boolean didConfigChange = InterestingConfigChange.isConfigChanged(getContext().getResources());

    if (takeContentChanged() || mApps == null || didConfigChange) {
      forceLoad();
    }
  }

  @Override
  protected void onStopLoading() {
    cancelLoad();
  }

  @Override
  public void onCanceled(List<AppDataParcelable> data) {
    super.onCanceled(data);

    onReleaseResources(data); // TODO onReleaseResources() is empty
  }

  @Override
  protected void onReset() {
    super.onReset();

    onStopLoading();

    // we're free to clear resources
    if (mApps != null) {
      onReleaseResources(mApps); // TODO onReleaseResources() is empty
      mApps = null;
    }

    if (packageReceiver != null) {
      getContext().unregisterReceiver(packageReceiver);

      packageReceiver = null;
    }

    InterestingConfigChange.recycle();
  }

  /** We would want to release resources here List is nothing we would want to close */
  // TODO do something
  private void onReleaseResources(List<AppDataParcelable> layoutElementList) {}

  /**
   * Check if an App is under /system or has been installed as an update to a built-in system
   * application.
   */
  public static boolean isAppInSystemPartition(ApplicationInfo applicationInfo) {
    return ((applicationInfo.flags
            & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
        != 0);
  }

  /** Check if an App is signed by system or not. */
  public boolean isSignedBySystem(PackageInfo piApp, PackageInfo piSys) {
    return (piApp != null
        && piSys != null
        && piApp.signatures != null
        && piSys.signatures[0].equals(piApp.signatures[0]));
  }
}
