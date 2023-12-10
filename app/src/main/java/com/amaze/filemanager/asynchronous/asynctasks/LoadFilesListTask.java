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

package com.amaze.filemanager.asynchronous.asynctasks;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.Q;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileListSorter;
import com.amaze.filemanager.filesystem.files.sort.SortType;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.ui.activities.MainActivityViewModel;
import com.amaze.filemanager.ui.fragments.CloudSheetFragment;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.data.MainFragmentViewModel;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.GenericExtKt;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.OnFileFound;
import com.cloudrail.si.interfaces.CloudStorage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import kotlin.collections.CollectionsKt;

public class LoadFilesListTask
    extends AsyncTask<Void, Void, Pair<OpenMode, List<LayoutElementParcelable>>> {

  private static final Logger LOG = LoggerFactory.getLogger(LoadFilesListTask.class);

  private String path;
  private WeakReference<MainFragment> mainFragmentReference;
  private WeakReference<Context> context;
  private OpenMode openmode;
  private boolean showHiddenFiles, showThumbs;
  private DataUtils dataUtils = DataUtils.getInstance();
  private OnAsyncTaskFinished<Pair<OpenMode, List<LayoutElementParcelable>>> listener;
  private boolean forceReload;

  public LoadFilesListTask(
      Context context,
      String path,
      MainFragment mainFragment,
      OpenMode openmode,
      boolean showThumbs,
      boolean showHiddenFiles,
      boolean forceReload,
      OnAsyncTaskFinished<Pair<OpenMode, List<LayoutElementParcelable>>> l) {
    this.path = path;
    this.mainFragmentReference = new WeakReference<>(mainFragment);
    this.openmode = openmode;
    this.context = new WeakReference<>(context);
    this.showThumbs = showThumbs;
    this.showHiddenFiles = showHiddenFiles;
    this.listener = l;
    this.forceReload = forceReload;
  }

  @Override
  @SuppressWarnings({"PMD.NPathComplexity", "ComplexMethod", "LongMethod"})
  protected @Nullable Pair<OpenMode, List<LayoutElementParcelable>> doInBackground(Void... p) {
    final MainFragment mainFragment = this.mainFragmentReference.get();
    final Context context = this.context.get();

    if (mainFragment == null
        || context == null
        || mainFragment.getMainFragmentViewModel() == null
        || mainFragment.getMainActivityViewModel() == null) {
      cancel(true);
      return null;
    }

    HybridFile hFile = null;
    MainFragmentViewModel mainFragmentViewModel = mainFragment.getMainFragmentViewModel();
    MainActivityViewModel mainActivityViewModel = mainFragment.getMainActivityViewModel();

    if (OpenMode.UNKNOWN.equals(openmode) || OpenMode.CUSTOM.equals(openmode)) {
      hFile = new HybridFile(openmode, path);
      hFile.generateMode(mainFragment.getActivity());
      openmode = hFile.getMode();

      if (hFile.isSmb()) {
        mainFragmentViewModel.setSmbPath(path);
      }
    }

    if (isCancelled()) return null;

    mainFragmentViewModel.setFolderCount(0);
    mainFragmentViewModel.setFileCount(0);
    final List<LayoutElementParcelable> list;

    switch (openmode) {
      case SMB:
        list = listSmb(hFile, mainActivityViewModel, mainFragment);
        break;
      case SFTP:
        list = listSftp(mainActivityViewModel);
        break;
      case CUSTOM:
        list = getCachedMediaList(mainActivityViewModel);
        break;
      case OTG:
        list = listOtg();
        openmode = OpenMode.OTG;
        break;
      case DOCUMENT_FILE:
        list = listDocumentFiles(mainActivityViewModel);
        openmode = OpenMode.DOCUMENT_FILE;
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        try {
          list = listCloud(mainActivityViewModel);
        } catch (CloudPluginException e) {
          LOG.warn("failed to load cloud files", e);
          AppConfig.toast(context, context.getResources().getString(R.string.failed_no_connection));
          return new Pair<>(openmode, Collections.emptyList());
        }
        break;
      case ANDROID_DATA:
        list = listAppDataDirectories(path);
        break;
      default:
        // we're neither in OTG not in SMB, load the list based on root/general filesystem
        list = listDefault(mainActivityViewModel, mainFragment);
        break;
    }

    if (list != null
        && !(openmode == OpenMode.CUSTOM && ((path).equals("5") || (path).equals("6")))) {
      postListCustomPathProcess(list, mainFragment);
    }

    return new Pair<>(openmode, list);
  }

  @Override
  protected void onCancelled() {
    listener.onAsyncTaskFinished(null);
  }

  @Override
  protected void onPostExecute(@Nullable Pair<OpenMode, List<LayoutElementParcelable>> list) {
    listener.onAsyncTaskFinished(list);
  }

  private List<LayoutElementParcelable> getCachedMediaList(
      MainActivityViewModel mainActivityViewModel) throws IllegalStateException {
    List<LayoutElementParcelable> list;
    int mediaType = Integer.parseInt(path);
    if (5 == mediaType
        || 6 == mediaType
        || mainActivityViewModel.getMediaCacheHash().get(mediaType) == null
        || forceReload) {
      switch (Integer.parseInt(path)) {
        case 0:
          list = listImages();
          break;
        case 1:
          list = listVideos();
          break;
        case 2:
          list = listaudio();
          break;
        case 3:
          list = listDocs();
          break;
        case 4:
          list = listApks();
          break;
        case 5:
          list = listRecent();
          break;
        case 6:
          list = listRecentFiles();
          break;
        default:
          throw new IllegalStateException();
      }
      if (5 != mediaType && 6 != mediaType) {
        // not saving recent files in cache
        mainActivityViewModel.getMediaCacheHash().set(mediaType, list);
      }
    } else {
      list = mainActivityViewModel.getFromMediaFilesCache(mediaType);
    }
    return list;
  }

  private void postListCustomPathProcess(
      @NonNull List<LayoutElementParcelable> list, @NonNull MainFragment mainFragment) {

    SortType sortType = SortHandler.getSortType(context.get(), path);

    MainFragmentViewModel viewModel = mainFragment.getMainFragmentViewModel();

    if (viewModel == null) {
      LOG.error("MainFragmentViewModel is null, this is a bug");
      return;
    }

    for (int i = 0; i < list.size(); i++) {
      LayoutElementParcelable layoutElementParcelable = list.get(i);

      if (layoutElementParcelable == null) {
        //noinspection SuspiciousListRemoveInLoop
        list.remove(i);
        continue;
      }

      if (layoutElementParcelable.isDirectory) {
        viewModel.incrementFolderCount();
      } else {
        viewModel.incrementFileCount();
      }
    }

    Collections.sort(list, new FileListSorter(viewModel.getDsort(), sortType));
  }

  private @Nullable LayoutElementParcelable createListParcelables(HybridFileParcelable baseFile) {
    if (dataUtils.isFileHidden(baseFile.getPath())) {
      return null;
    }

    final MainFragment mainFragment = this.mainFragmentReference.get();
    final Context context = this.context.get();

    if (mainFragment == null || context == null) {
      cancel(true);
      return null;
    }

    String size = "";
    long longSize = 0;

    if (!baseFile.isDirectory()) {
      if (baseFile.getSize() != -1) {
        try {
          longSize = baseFile.getSize();
          size = Formatter.formatFileSize(context, longSize);
        } catch (NumberFormatException e) {
          LOG.warn("failed to create list parcelables", e);
        }
      }
    }

    LayoutElementParcelable layoutElement =
        new LayoutElementParcelable(
            context,
            baseFile.getName(context),
            baseFile.getPath(),
            baseFile.getPermission(),
            baseFile.getLink(),
            size,
            longSize,
            false,
            baseFile.getDate() + "",
            baseFile.isDirectory(),
            showThumbs,
            baseFile.getMode());
    return layoutElement;
  }

  private List<LayoutElementParcelable> listImages() {
    final String[] projection = {MediaStore.Images.Media.DATA};
    return listMediaCommon(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null);
  }

  private List<LayoutElementParcelable> listVideos() {
    final String[] projection = {MediaStore.Video.Media.DATA};
    return listMediaCommon(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null);
  }

  private List<LayoutElementParcelable> listaudio() {
    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    String[] projection = {MediaStore.Audio.Media.DATA};
    return listMediaCommon(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection);
  }

  private @Nullable List<LayoutElementParcelable> listMediaCommon(
      Uri contentUri, @NonNull String[] projection, @Nullable String selection) {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return null;
    }

    Cursor cursor =
        context.getContentResolver().query(contentUri, projection, selection, null, null);

    ArrayList<LayoutElementParcelable> retval = new ArrayList<>();
    if (cursor == null) return retval;
    else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
      do {
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
        if (strings != null) {
          LayoutElementParcelable parcelable = createListParcelables(strings);
          if (parcelable != null) retval.add(parcelable);
        }
      } while (cursor.moveToNext());
    }
    cursor.close();
    return retval;
  }

  private @Nullable List<LayoutElementParcelable> listDocs() {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return null;
    }

    ArrayList<LayoutElementParcelable> docs = new ArrayList<>();
    final String[] projection = {MediaStore.Files.FileColumns.DATA};
    Cursor cursor =
        context
            .getContentResolver()
            .query(MediaStore.Files.getContentUri("external"), projection, null, null, null);

    if (cursor == null) return docs;
    else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
      do {
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));

        if (path != null
            && (path.endsWith(".pdf")
                || path.endsWith(".doc")
                || path.endsWith(".docx")
                || path.endsWith("txt")
                || path.endsWith(".rtf")
                || path.endsWith(".odt")
                || path.endsWith(".html")
                || path.endsWith(".xml")
                || path.endsWith(".text/x-asm")
                || path.endsWith(".def")
                || path.endsWith(".in")
                || path.endsWith(".rc")
                || path.endsWith(".list")
                || path.endsWith(".log")
                || path.endsWith(".pl")
                || path.endsWith(".prop")
                || path.endsWith(".properties")
                || path.endsWith(".msg")
                || path.endsWith(".pages")
                || path.endsWith(".wpd")
                || path.endsWith(".wps"))) {
          HybridFileParcelable strings =
              RootHelper.generateBaseFile(new File(path), showHiddenFiles);
          if (strings != null) {
            LayoutElementParcelable parcelable = createListParcelables(strings);
            if (parcelable != null) docs.add(parcelable);
          }
        }
      } while (cursor.moveToNext());
    }
    cursor.close();
    Collections.sort(docs, (lhs, rhs) -> -1 * Long.valueOf(lhs.date).compareTo(rhs.date));
    return docs;
  }

  private @Nullable List<LayoutElementParcelable> listApks() {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return null;
    }

    ArrayList<LayoutElementParcelable> apks = new ArrayList<>();
    final String[] projection = {MediaStore.Files.FileColumns.DATA};

    Cursor cursor =
        context
            .getContentResolver()
            .query(MediaStore.Files.getContentUri("external"), projection, null, null, null);
    if (cursor == null) return apks;
    else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
      do {
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        if (path != null && path.endsWith(".apk")) {
          HybridFileParcelable strings =
              RootHelper.generateBaseFile(new File(path), showHiddenFiles);
          if (strings != null) {
            LayoutElementParcelable parcelable = createListParcelables(strings);
            if (parcelable != null) apks.add(parcelable);
          }
        }
      } while (cursor.moveToNext());
    }
    cursor.close();
    return apks;
  }

  private @Nullable List<LayoutElementParcelable> listRecent() {
    final MainFragment mainFragment = this.mainFragmentReference.get();
    if (mainFragment == null) {
      cancel(true);
      return null;
    }

    UtilsHandler utilsHandler = AppConfig.getInstance().getUtilsHandler();
    final LinkedList<String> paths = utilsHandler.getHistoryLinkedList();
    ArrayList<LayoutElementParcelable> songs = new ArrayList<>();
    for (String f : paths) {
      if (!f.equals("/")) {
        HybridFileParcelable hybridFileParcelable =
            RootHelper.generateBaseFile(new File(f), showHiddenFiles);
        if (hybridFileParcelable != null) {
          hybridFileParcelable.generateMode(mainFragment.getActivity());
          if (hybridFileParcelable.isSimpleFile()
              && !hybridFileParcelable.isDirectory()
              && hybridFileParcelable.exists()) {
            LayoutElementParcelable parcelable = createListParcelables(hybridFileParcelable);
            if (parcelable != null) songs.add(parcelable);
          }
        }
      }
    }
    return songs;
  }

  private @Nullable List<LayoutElementParcelable> listRecentFiles() {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return null;
    }

    List<LayoutElementParcelable> recentFiles = new ArrayList<>(20);
    final String[] projection = {
      MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED
    };
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
    Date d = c.getTime();
    Cursor cursor;
    if (SDK_INT >= Q) {
      Bundle queryArgs = new Bundle();
      queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 20);
      queryArgs.putStringArray(
          ContentResolver.QUERY_ARG_SORT_COLUMNS,
          new String[] {MediaStore.Files.FileColumns.DATE_MODIFIED});
      queryArgs.putInt(
          ContentResolver.QUERY_ARG_SORT_DIRECTION,
          ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
      cursor =
          context
              .getContentResolver()
              .query(MediaStore.Files.getContentUri("external"), projection, queryArgs, null);
    } else {
      cursor =
          context
              .getContentResolver()
              .query(
                  MediaStore.Files.getContentUri("external"),
                  projection,
                  null,
                  null,
                  MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT 20");
    }
    if (cursor == null) return recentFiles;
    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
      do {
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        File f = new File(path);
        if (d.compareTo(new Date(f.lastModified())) != 1 && !f.isDirectory()) {
          HybridFileParcelable strings =
              RootHelper.generateBaseFile(new File(path), showHiddenFiles);
          if (strings != null) {
            LayoutElementParcelable parcelable = createListParcelables(strings);
            if (parcelable != null) recentFiles.add(parcelable);
          }
        }
      } while (cursor.moveToNext());
    }
    cursor.close();
    return recentFiles;
  }

  private @NonNull List<LayoutElementParcelable> listAppDataDirectories(@NonNull String basePath) {
    if (!GenericExtKt.containsPath(FileProperties.ANDROID_DEVICE_DATA_DIRS, basePath)) {
      throw new IllegalArgumentException("Invalid base path: [" + basePath + "]");
    }
    Context ctx = context.get();
    @Nullable PackageManager pm = ctx != null ? ctx.getPackageManager() : null;
    List<LayoutElementParcelable> retval = new ArrayList<>();
    if (pm != null) {
      Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
      for (ResolveInfo app :
          CollectionsKt.distinctBy(
              pm.queryIntentActivities(intent, 0),
              resolveInfo -> resolveInfo.activityInfo.packageName)) {
        File dir = new File(new File(basePath), app.activityInfo.packageName);
        if (dir.exists()) {
          LayoutElementParcelable element =
              new LayoutElementParcelable(
                  ctx,
                  dir.getAbsolutePath(),
                  "",
                  "",
                  Long.toString(dir.length()),
                  dir.length(),
                  false,
                  Long.toString(dir.lastModified()),
                  true,
                  false,
                  OpenMode.ANDROID_DATA);
          retval.add(element);
        }
      }
    }
    return retval;
  }

  private List<LayoutElementParcelable> listSmb(
      @Nullable final HybridFile hFile,
      @NonNull MainActivityViewModel mainActivityViewModel,
      @NonNull MainFragment mainFragment) {
    HybridFile _file = hFile;
    if (_file == null) {
      _file = new HybridFile(OpenMode.SMB, path);
    }
    if (!_file.getPath().endsWith("/")) {
      _file.setPath(_file.getPath() + "/");
    }
    @NonNull List<LayoutElementParcelable> list;
    List<LayoutElementParcelable> smbCache = mainActivityViewModel.getFromListCache(path);
    openmode = OpenMode.SMB;
    if (smbCache != null && !forceReload) {
      list = smbCache;
    } else {
      try {
        SmbFile[] smbFile = _file.getSmbFile(5000).listFiles();
        list = mainFragment.addToSmb(smbFile, path, showHiddenFiles);
      } catch (SmbAuthException e) {
        if (!e.getMessage().toLowerCase().contains("denied")) {
          mainFragment.reauthenticateSmb();
        }
        LOG.warn("failed to load smb list, authentication issue", e);
        return null;
      } catch (SmbException | NullPointerException e) {
        LOG.warn("Failed to load smb files for path: " + path, e);
        mainFragment.reauthenticateSmb();
        return null;
      }
      mainActivityViewModel.putInCache(path, list);
    }
    return list;
  }

  private List<LayoutElementParcelable> listSftp(
      @NonNull MainActivityViewModel mainActivityViewModel) {
    HybridFile ftpHFile = new HybridFile(openmode, path);
    List<LayoutElementParcelable> list;
    List<LayoutElementParcelable> sftpCache = mainActivityViewModel.getFromListCache(path);
    if (sftpCache != null && !forceReload) {
      list = sftpCache;
    } else {
      list = new ArrayList<>();
      ftpHFile.forEachChildrenFile(
          context.get(),
          false,
          file -> {
            if (!(dataUtils.isFileHidden(file.getPath()) || file.isHidden() && !showHiddenFiles)) {
              LayoutElementParcelable elem = createListParcelables(file);
              if (elem != null) {
                list.add(elem);
              }
            }
          });
      mainActivityViewModel.putInCache(path, list);
    }
    return list;
  }

  private List<LayoutElementParcelable> listOtg() {
    List<LayoutElementParcelable> list = new ArrayList<>();
    listOtgInternal(
        path,
        file -> {
          LayoutElementParcelable elem = createListParcelables(file);
          if (elem != null) list.add(elem);
        });
    return list;
  }

  private List<LayoutElementParcelable> listDocumentFiles(
      @NonNull MainActivityViewModel mainActivityViewModel) {
    List<LayoutElementParcelable> list;
    List<LayoutElementParcelable> cache = mainActivityViewModel.getFromListCache(path);
    if (cache != null && !forceReload) {
      list = cache;
    } else {
      list = new ArrayList<>();
      listDocumentFilesInternal(
          file -> {
            LayoutElementParcelable elem = createListParcelables(file);
            if (elem != null) list.add(elem);
          });
      mainActivityViewModel.putInCache(path, list);
    }
    return list;
  }

  private List<LayoutElementParcelable> listCloud(
      @NonNull MainActivityViewModel mainActivityViewModel) throws CloudPluginException {
    List<LayoutElementParcelable> list;
    List<LayoutElementParcelable> cloudCache = mainActivityViewModel.getFromListCache(path);
    if (cloudCache != null && !forceReload) {
      list = cloudCache;
    } else {
      CloudStorage cloudStorage = dataUtils.getAccount(openmode);
      list = new ArrayList<>();
      listCloudInternal(
          path,
          cloudStorage,
          openmode,
          file -> {
            LayoutElementParcelable elem = createListParcelables(file);
            if (elem != null) list.add(elem);
          });
      mainActivityViewModel.putInCache(path, list);
    }
    return list;
  }

  private List<LayoutElementParcelable> listDefault(
      @NonNull MainActivityViewModel mainActivityViewModel, @NonNull MainFragment mainFragment) {
    List<LayoutElementParcelable> list;
    List<LayoutElementParcelable> localCache = mainActivityViewModel.getFromListCache(path);
    openmode =
        ListFilesCommand.INSTANCE.getOpenMode(
            path, mainFragment.requireMainActivity().isRootExplorer());
    if (localCache != null && !forceReload) {
      list = localCache;
    } else {
      list = new ArrayList<>();
      final OpenMode[] currentOpenMode = new OpenMode[1];
      ListFilesCommand.INSTANCE.listFiles(
          path,
          mainFragment.requireMainActivity().isRootExplorer(),
          showHiddenFiles,
          mode -> {
            currentOpenMode[0] = mode;
            return null;
          },
          hybridFileParcelable -> {
            LayoutElementParcelable elem = createListParcelables(hybridFileParcelable);
            if (elem != null) list.add(elem);
            return null;
          });
      if (list.size() > MainActivityViewModel.Companion.getCACHE_LOCAL_LIST_THRESHOLD()) {
        mainActivityViewModel.putInCache(path, list);
      }
      if (null != currentOpenMode[0]) {
        openmode = currentOpenMode[0];
      }
    }
    return list;
  }

  /**
   * Lists files from an OTG device
   *
   * @param path the path to the directory tree, starts with prefix {@link
   *     com.amaze.filemanager.utils.OTGUtil#PREFIX_OTG} Independent of URI (or mount point) for the
   *     OTG
   */
  private void listOtgInternal(String path, OnFileFound fileFound) {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return;
    }

    OTGUtil.getDocumentFiles(path, context, fileFound);
  }

  private void listDocumentFilesInternal(OnFileFound fileFound) {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return;
    }

    OTGUtil.getDocumentFiles(
        SafRootHolder.getUriRoot(), path, context, OpenMode.DOCUMENT_FILE, fileFound);
  }

  private void listCloudInternal(
      String path, CloudStorage cloudStorage, OpenMode openMode, OnFileFound fileFoundCallback)
      throws CloudPluginException {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return;
    }

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) {
      throw new CloudPluginException();
    }

    CloudUtil.getCloudFiles(path, cloudStorage, openMode, fileFoundCallback);
  }
}
