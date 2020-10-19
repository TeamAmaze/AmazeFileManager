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

package com.amaze.filemanager.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.AppDataParcelable;
import com.amaze.filemanager.adapters.data.OpenFileParcelable;
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel;
import com.amaze.filemanager.adapters.holders.AppHolder;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.AnimUtils;
import com.amaze.filemanager.utils.Utils;
import com.bumptech.glide.util.ViewPreloadSizeProvider;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class AppsAdapter extends ArrayAdapter<AppDataParcelable> {

  private static final String COM_ANDROID_VENDING = "com.android.vending";

  private UtilitiesProvider utilsProvider;
  private Fragment fragment;
  private AppsAdapterPreloadModel modelProvider;
  private ViewPreloadSizeProvider<String> sizeProvider;
  private SparseBooleanArray myChecked = new SparseBooleanArray();
  private SharedPreferences sharedPrefs;
  private boolean isBottomSheet;

  private ThemedActivity themedActivity;

  public AppsAdapter(
      Fragment fragment,
      ThemedActivity ba,
      UtilitiesProvider utilsProvider,
      AppsAdapterPreloadModel modelProvider,
      ViewPreloadSizeProvider<String> sizeProvider,
      int resourceId,
      SharedPreferences sharedPrefs,
      boolean isBottomSheet) {
    super(fragment.getContext(), resourceId);
    themedActivity = ba;
    this.utilsProvider = utilsProvider;
    this.modelProvider = modelProvider;
    this.sizeProvider = sizeProvider;
    this.fragment = fragment;
    this.sharedPrefs = sharedPrefs;
    this.isBottomSheet = isBottomSheet;

    /*for (int i = 0; i < items.size(); i++) {
        myChecked.put(i, false);
    }*/
  }

  public void setData(List<AppDataParcelable> data) {
    clear();

    if (data != null) {
      addAll(data);
    }
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    final AppDataParcelable rowItem = getItem(position);

    View view;
    if (convertView == null) {
      LayoutInflater mInflater =
          (LayoutInflater) fragment.getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
      view = mInflater.inflate(R.layout.rowlayout, null);
      final AppHolder vholder = new AppHolder(view);
      view.findViewById(R.id.generic_icon).setVisibility(View.GONE);
      view.findViewById(R.id.picture_icon).setVisibility(View.GONE);
      view.setTag(vholder);
      if (sizeProvider != null) {
        sizeProvider.setView(view.findViewById(R.id.apk_icon));
      }
    } else {
      view = convertView;
    }

    final AppHolder holder = (AppHolder) view.getTag();

    if (isBottomSheet) {
      holder.about.setVisibility(View.GONE);
      holder.txtDesc.setText(rowItem.openFileParcelable.getClassName());
      holder.txtDesc.setSingleLine(true);
      holder.txtDesc.setEllipsize(TextUtils.TruncateAt.MIDDLE);
      modelProvider.loadApkImage(rowItem.packageName, holder.apkIcon);
    } else {
      modelProvider.loadApkImage(rowItem.path, holder.apkIcon);
    }

    if (holder.about != null && !isBottomSheet) {
      if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
        holder.about.setColorFilter(Color.parseColor("#ff666666"));
      showPopup(holder.about, rowItem);
    }
    holder.txtTitle.setText(rowItem.label);
    boolean enableMarqueeFilename =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME, true);
    if (enableMarqueeFilename) {
      holder.txtTitle.setEllipsize(
          enableMarqueeFilename ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.MIDDLE);
      AnimUtils.marqueeAfterDelay(2000, holder.txtTitle);
    }

    //	File f = new File(rowItem.getDesc());
    if (!isBottomSheet) {
      holder.txtDesc.setText(rowItem.fileSize);
    }
    holder.rl.setClickable(true);
    holder.rl.setOnClickListener(p1 -> startActivityForRowItem(rowItem));

    if (myChecked.get(position)) {
      holder.rl.setBackgroundColor(
          Utils.getColor(fragment.getContext(), R.color.appsadapter_background));
    } else {
      if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
        holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
      } else {
        holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
      }
    }
    return view;
  }

  private void startActivityForRowItem(AppDataParcelable rowItem) {
    if (isBottomSheet) {
      OpenFileParcelable openFileParcelable = rowItem.openFileParcelable;
      Intent intent =
          OpenFileDialogFragment.Companion.buildIntent(
              Objects.requireNonNull(openFileParcelable.getUri()),
              Objects.requireNonNull(openFileParcelable.getMimeType()),
              Objects.requireNonNull(openFileParcelable.getUseNewStack()),
              openFileParcelable.getClassName(),
              openFileParcelable.getPackageName());
      OpenFileDialogFragment.Companion.setLastOpenedApp(rowItem, themedActivity);
      fragment.getContext().startActivity(intent);
    } else {
      Intent i1 =
          fragment.getContext().getPackageManager().getLaunchIntentForPackage(rowItem.packageName);
      if (i1 != null) {
        fragment.startActivity(i1);
      } else {
        Toast.makeText(
                fragment.getContext(), fragment.getString(R.string.not_allowed), Toast.LENGTH_LONG)
            .show();
        // TODO: Implement this method
      }
    }
  }

  private void showPopup(View v, final AppDataParcelable rowItem) {
    v.setOnClickListener(
        view -> {
          PopupMenu popupMenu = new PopupMenu(fragment.getContext(), view);
          popupMenu.setOnMenuItemClickListener(
              item -> {
                int colorAccent = themedActivity.getAccent();

                switch (item.getItemId()) {
                  case R.id.open:
                    Intent i1 =
                        fragment
                            .getContext()
                            .getPackageManager()
                            .getLaunchIntentForPackage(rowItem.packageName);
                    if (i1 != null) fragment.startActivity(i1);
                    else
                      Toast.makeText(
                              fragment.getContext(),
                              fragment.getString(R.string.not_allowed),
                              Toast.LENGTH_LONG)
                          .show();
                    return true;
                  case R.id.share:
                    ArrayList<File> arrayList2 = new ArrayList<File>();
                    arrayList2.add(new File(rowItem.path));
                    themedActivity.getColorPreference();
                    FileUtils.shareFiles(
                        arrayList2,
                        fragment.getActivity(),
                        utilsProvider.getAppTheme(),
                        colorAccent);
                    return true;
                  case R.id.unins:
                    final HybridFileParcelable f1 = new HybridFileParcelable(rowItem.path);
                    f1.setMode(OpenMode.ROOT);

                    if ((Integer.valueOf(rowItem.data.substring(0, rowItem.data.indexOf("_")))
                            & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {
                      // system package
                      if (sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false)) {
                        MaterialDialog.Builder builder1 =
                            new MaterialDialog.Builder(fragment.requireContext());
                        builder1
                            .theme(utilsProvider.getAppTheme().getMaterialDialogTheme())
                            .content(fragment.getString(R.string.unin_system_apk))
                            .title(fragment.getString(R.string.warning))
                            .negativeColor(colorAccent)
                            .positiveColor(colorAccent)
                            .negativeText(fragment.getString(R.string.no))
                            .positiveText(fragment.getString(R.string.yes))
                            .onNegative(((dialog, which) -> dialog.cancel()))
                            .onPositive(
                                ((dialog, which) -> {
                                  ArrayList<HybridFileParcelable> files = new ArrayList<>();
                                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    String parent = f1.getParent(fragment.getContext());
                                    if (!parent.equals("app") && !parent.equals("priv-app")) {
                                      HybridFileParcelable baseFile =
                                          new HybridFileParcelable(
                                              f1.getParent(fragment.getContext()));
                                      baseFile.setMode(OpenMode.ROOT);
                                      files.add(baseFile);
                                    } else files.add(f1);
                                  } else {
                                    files.add(f1);
                                  }
                                  new DeleteTask(fragment.requireContext()).execute((files));
                                }))
                            .build()
                            .show();
                      } else {
                        Toast.makeText(
                                fragment.getContext(),
                                fragment.getString(R.string.enablerootmde),
                                Toast.LENGTH_SHORT)
                            .show();
                      }
                    } else {
                      FileUtils.uninstallPackage(rowItem.packageName, fragment.getContext());
                    }
                    return true;
                  case R.id.play:
                    Intent intent1 = new Intent(Intent.ACTION_VIEW);
                    try {
                      intent1.setData(
                          Uri.parse(String.format("market://details?id=%s", rowItem.packageName)));
                      fragment.startActivity(intent1);
                    } catch (ActivityNotFoundException ifPlayStoreNotInstalled) {
                      intent1.setData(
                          Uri.parse(
                              String.format(
                                  "https://play.google.com/store/apps/details?id=%s",
                                  rowItem.packageName)));
                      fragment.startActivity(intent1);
                    }
                    return true;
                  case R.id.properties:
                    fragment.startActivity(
                        new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse(String.format("package:%s", rowItem.packageName))));
                    return true;
                  case R.id.backup:
                    Toast.makeText(
                            fragment.getContext(),
                            fragment.getString(R.string.copyingapk)
                                + Environment.getExternalStorageDirectory().getPath()
                                + "/app_backup",
                            Toast.LENGTH_LONG)
                        .show();
                    File f = new File(rowItem.path);
                    ArrayList<HybridFileParcelable> ab = new ArrayList<>();
                    File dst =
                        new File(
                            Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                    if (!dst.exists() || !dst.isDirectory()) dst.mkdirs();
                    Intent intent = new Intent(fragment.getContext(), CopyService.class);
                    HybridFileParcelable baseFile = RootHelper.generateBaseFile(f, true);
                    baseFile.setName(
                        rowItem.label
                            + "_"
                            + rowItem.packageName.substring(rowItem.packageName.indexOf("_") + 1)
                            + ".apk");
                    ab.add(baseFile);

                    intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, ab);
                    intent.putExtra(CopyService.TAG_COPY_TARGET, dst.getPath());
                    intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, 0);

                    ServiceWatcherUtil.runService(fragment.getContext(), intent);
                    return true;
                }
                return false;
              });

          popupMenu.inflate(R.menu.app_options);
          popupMenu.show();
        });
  }
}
