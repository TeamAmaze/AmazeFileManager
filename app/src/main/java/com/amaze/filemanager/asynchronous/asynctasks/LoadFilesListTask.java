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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileListSorter;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.ui.fragments.CloudSheetFragment;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.OnFileFound;
import com.cloudrail.si.interfaces.CloudStorage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class LoadFilesListTask
    extends AsyncTask<Void, Void, Pair<OpenMode, ArrayList<LayoutElementParcelable>>> {

  private String path;
  private WeakReference<MainFragment> mainFragmentReference;
  private WeakReference<Context> context;
  private OpenMode openmode;
  private boolean showHiddenFiles, showThumbs;
  private DataUtils dataUtils = DataUtils.getInstance();
  private OnAsyncTaskFinished<Pair<OpenMode, ArrayList<LayoutElementParcelable>>> listener;

  public LoadFilesListTask(
      Context context,
      String path,
      MainFragment mainFragment,
      OpenMode openmode,
      boolean showThumbs,
      boolean showHiddenFiles,
      OnAsyncTaskFinished<Pair<OpenMode, ArrayList<LayoutElementParcelable>>> l) {
    this.path = path;
    this.mainFragmentReference = new WeakReference<>(mainFragment);
    this.openmode = openmode;
    this.context = new WeakReference<>(context);
    this.showThumbs = showThumbs;
    this.showHiddenFiles = showHiddenFiles;
    this.listener = l;
  }

  @Override
  protected @Nullable Pair<OpenMode, ArrayList<LayoutElementParcelable>> doInBackground(Void... p) {
    final MainFragment mainFragment = this.mainFragmentReference.get();
    final Context context = this.context.get();

    if (mainFragment == null
        || context == null
        || mainFragment.getMainFragmentViewModel() == null) {
      cancel(true);
      return null;
    }

    HybridFile hFile = null;

    if (OpenMode.UNKNOWN.equals(openmode) || OpenMode.CUSTOM.equals(openmode)) {
      hFile = new HybridFile(openmode, path);
      hFile.generateMode(mainFragment.getActivity());
      openmode = hFile.getMode();

      if (hFile.isSmb()) {
        mainFragment.getMainFragmentViewModel().setSmbPath(path);
      }
    }

    if (isCancelled()) return null;

    mainFragment.getMainFragmentViewModel().setFolderCount(0);
    mainFragment.getMainFragmentViewModel().setFileCount(0);
    final ArrayList<LayoutElementParcelable> list;

    switch (openmode) {
      case SMB:
        if (hFile == null) {
          hFile = new HybridFile(OpenMode.SMB, path);
        }
        if (!hFile.getPath().endsWith("/")) {
          hFile.setPath(hFile.getPath() + "/");
        }
        try {
          SmbFile[] smbFile = hFile.getSmbFile(5000).listFiles();
          list = mainFragment.addToSmb(smbFile, path, showHiddenFiles);
          openmode = OpenMode.SMB;
        } catch (SmbAuthException e) {
          if (!e.getMessage().toLowerCase().contains("denied")) {
            mainFragment.reauthenticateSmb();
          }
          e.printStackTrace();
          return null;
        } catch (SmbException | NullPointerException e) {
          Log.w(getClass().getSimpleName(), "Failed to load smb files for path: " + path, e);
          mainFragment.reauthenticateSmb();
          return null;
        }
        break;
      case SFTP:
        HybridFile sftpHFile = new HybridFile(OpenMode.SFTP, path);

        list = new ArrayList<LayoutElementParcelable>();

        sftpHFile.forEachChildrenFile(
            context,
            false,
            file -> {
              if (!(dataUtils.isFileHidden(file.getPath())
                  || file.isHidden() && !showHiddenFiles)) {
                LayoutElementParcelable elem = createListParcelables(file);
                if (elem != null) {
                  list.add(elem);
                }
              }
            });
        break;
      case CUSTOM:
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

        break;
      case OTG:
        list = new ArrayList<>();
        listOtg(
            path,
            file -> {
              LayoutElementParcelable elem = createListParcelables(file);
              if (elem != null) list.add(elem);
            });
        openmode = OpenMode.OTG;
        break;
      case DOCUMENT_FILE:
        list = new ArrayList<>();
        listDocumentFiles(
            file -> {
              LayoutElementParcelable elem = createListParcelables(file);
              if (elem != null) list.add(elem);
            });
        openmode = OpenMode.DOCUMENT_FILE;
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        CloudStorage cloudStorage = dataUtils.getAccount(openmode);
        list = new ArrayList<>();

        try {
          listCloud(
              path,
              cloudStorage,
              openmode,
              file -> {
                LayoutElementParcelable elem = createListParcelables(file);
                if (elem != null) list.add(elem);
              });
        } catch (CloudPluginException e) {
          e.printStackTrace();
          AppConfig.toast(context, context.getResources().getString(R.string.failed_no_connection));
          return new Pair<>(openmode, list);
        }
        break;
      default:
        // we're neither in OTG not in SMB, load the list based on root/general filesystem
        list = new ArrayList<>();
        final OpenMode[] currentOpenMode = new OpenMode[1];
        ListFilesCommand.INSTANCE.listFiles(
            path,
            mainFragment.getMainActivity().isRootExplorer(),
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
        if (null != currentOpenMode[0]) {
          openmode = currentOpenMode[0];
        }
        break;
    }

    if (list != null
        && !(openmode == OpenMode.CUSTOM && ((path).equals("5") || (path).equals("6")))) {
      int t = SortHandler.getSortType(context, path);
      int sortby;
      int asc;
      if (t <= 3) {
        sortby = t;
        asc = 1;
      } else {
        asc = -1;
        sortby = t - 4;
      }
      Collections.sort(
          list,
          new FileListSorter(mainFragment.getMainFragmentViewModel().getDsort(), sortby, asc));
    }

    return new Pair<>(openmode, list);
  }

  @Override
  protected void onCancelled() {
    listener.onAsyncTaskFinished(null);
  }

  @Override
  protected void onPostExecute(@Nullable Pair<OpenMode, ArrayList<LayoutElementParcelable>> list) {
    listener.onAsyncTaskFinished(list);
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

    if (baseFile.isDirectory()) {
      mainFragment
          .getMainFragmentViewModel()
          .setFolderCount(mainFragment.getMainFragmentViewModel().getFolderCount() + 1);
    } else {
      if (baseFile.getSize() != -1) {
        try {
          longSize = baseFile.getSize();
          size = Formatter.formatFileSize(context, longSize);
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }
      mainFragment
          .getMainFragmentViewModel()
          .setFileCount(mainFragment.getMainFragmentViewModel().getFileCount() + 1);
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

  private ArrayList<LayoutElementParcelable> listImages() {
    final String[] projection = {MediaStore.Images.Media.DATA};
    return listMediaCommon(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null);
  }

  private ArrayList<LayoutElementParcelable> listVideos() {
    final String[] projection = {MediaStore.Video.Media.DATA};
    return listMediaCommon(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null);
  }

  private ArrayList<LayoutElementParcelable> listaudio() {
    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    String[] projection = {MediaStore.Audio.Media.DATA};
    return listMediaCommon(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection);
  }

  private @Nullable ArrayList<LayoutElementParcelable> listMediaCommon(
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

  private @Nullable ArrayList<LayoutElementParcelable> listDocs() {
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

  private @Nullable ArrayList<LayoutElementParcelable> listApks() {
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

  private @Nullable ArrayList<LayoutElementParcelable> listRecent() {
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
          if (!hybridFileParcelable.isSmb()
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

  private @Nullable ArrayList<LayoutElementParcelable> listRecentFiles() {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return null;
    }

    ArrayList<LayoutElementParcelable> recentFiles = new ArrayList<>(20);
    final String[] projection = {MediaStore.Files.FileColumns.DATA};
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
    Date d = c.getTime();
    Cursor cursor =
        context
            .getContentResolver()
            .query(
                MediaStore.Files.getContentUri("external"),
                projection,
                null,
                null,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT 20");
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

  /**
   * Lists files from an OTG device
   *
   * @param path the path to the directory tree, starts with prefix {@link
   *     com.amaze.filemanager.utils.OTGUtil#PREFIX_OTG} Independent of URI (or mount point) for the
   *     OTG
   */
  private void listOtg(String path, OnFileFound fileFound) {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return;
    }

    OTGUtil.getDocumentFiles(path, context, fileFound);
  }

  private void listDocumentFiles(OnFileFound fileFound) {
    final Context context = this.context.get();

    if (context == null) {
      cancel(true);
      return;
    }

    OTGUtil.getDocumentFiles(
        SafRootHolder.getUriRoot(), path, context, OpenMode.DOCUMENT_FILE, fileFound);
  }

  private void listCloud(
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
