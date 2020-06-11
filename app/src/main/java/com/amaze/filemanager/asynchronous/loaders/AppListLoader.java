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
import java.util.Collections;
import java.util.List;

import com.amaze.filemanager.adapters.data.AppDataParcelable;
import com.amaze.filemanager.utils.InterestingConfigChange;
import com.amaze.filemanager.utils.broadcast_receiver.PackageReceiver;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.format.Formatter;

import androidx.core.util.Pair;
import androidx.loader.content.AsyncTaskLoader;

/**
 * Created by vishal on 23/2/17.
 *
 * <p>Class loads all the packages installed
 */
public class AppListLoader extends AsyncTaskLoader<AppListLoader.AppsDataPair> {

  private PackageManager packageManager;
  private PackageReceiver packageReceiver;
  private AppsDataPair mApps;
  private int sortBy, asc;

  public AppListLoader(Context context, int sortBy, int asc) {
    super(context);

    this.sortBy = sortBy;
    this.asc = asc;

    /*
     * using global context because of the fact that loaders are supposed to be used
     * across fragments and activities
     */
    packageManager = getContext().getPackageManager();
  }

  @Override
  public AppsDataPair loadInBackground() {
    List<ApplicationInfo> apps =
        packageManager.getInstalledApplications(
            PackageManager.MATCH_UNINSTALLED_PACKAGES
                | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS);

    if (apps == null) return new AppsDataPair(Collections.emptyList(), Collections.emptyList());

    mApps = new AppsDataPair(new ArrayList<>(apps.size()), new ArrayList<>(apps.size()));

    for (ApplicationInfo object : apps) {
      File sourceDir = new File(object.sourceDir);

      String label = object.loadLabel(packageManager).toString();
      PackageInfo info;

      try {
        info = packageManager.getPackageInfo(object.packageName, 0);
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
        info = null;
      }

      AppDataParcelable elem =
          new AppDataParcelable(
              label == null ? object.packageName : label,
              object.sourceDir,
              object.packageName,
              object.flags + "_" + (info != null ? info.versionName : ""),
              Formatter.formatFileSize(getContext(), sourceDir.length()),
              sourceDir.length(),
              sourceDir.lastModified());

      mApps.first.add(elem);

      Collections.sort(mApps.first, new AppDataParcelable.AppDataSorter(sortBy, asc));

      for (AppDataParcelable p : mApps.first) {
        mApps.second.add(p.path);
      }
    }

    return mApps;
  }

  @Override
  public void deliverResult(AppsDataPair data) {
    if (isReset()) {

      if (data != null) onReleaseResources(data); // TODO onReleaseResources() is empty
    }

    // preserving old data for it to be closed
    AppsDataPair oldData = mApps;
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
  public void onCanceled(AppsDataPair data) {
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
  private void onReleaseResources(AppsDataPair layoutElementList) {}

  /** typedef Pair<List<AppDataParcelable>, List<String>> AppsDataPair */
  public static class AppsDataPair extends Pair<List<AppDataParcelable>, List<String>> {

    /**
     * Constructor for a Pair.
     *
     * @param first the first object in the Pair
     * @param second the second object in the pair
     */
    public AppsDataPair(List<AppDataParcelable> first, List<String> second) {
      super(first, second);
    }
  }
}
