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
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileListSorter;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.ui.activities.superclasses.BaseAsyncTask;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class LoadFilesListTask
    extends AsyncTask<Void, Void, Pair<OpenMode, ArrayList<LayoutElementParcelable>>>
    implements BaseAsyncTask {

  private String path;
  private MainFragment mainFragment;
  private Context context;
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
    this.mainFragment = mainFragment;
    this.openmode = openmode;
    this.context = context;
    this.showThumbs = showThumbs;
    this.showHiddenFiles = showHiddenFiles;
    this.listener = l;
  }

  @Override
  protected Pair<OpenMode, ArrayList<LayoutElementParcelable>> doInBackground(Void... p) {
    HybridFile hFile = null;

    if (OpenMode.UNKNOWN.equals(openmode) || OpenMode.CUSTOM.equals(openmode)) {
      hFile = new HybridFile(openmode, path);
      hFile.generateMode(nullCheckOrInterrupt(mainFragment, this).getActivity());
      openmode = hFile.getMode();

      if (hFile.isSmb()) {
        nullCheckOrInterrupt(mainFragment, this).smbPath = path;
      }
    }

    if (isCancelled()) return null;

    nullCheckOrInterrupt(mainFragment, this).folder_count = 0;
    nullCheckOrInterrupt(mainFragment, this).file_count = 0;
    final ArrayList<LayoutElementParcelable> list;

    switch (openmode) {
      case SMB:
        if (hFile == null) {
          hFile = new HybridFile(OpenMode.SMB, path);
        }

        try {
          SmbFile[] smbFile = hFile.getSmbFile(5000).listFiles();
          list = nullCheckOrInterrupt(mainFragment, this).addToSmb(smbFile, path, showHiddenFiles);
          openmode = OpenMode.SMB;
        } catch (SmbAuthException e) {
          if (!e.getMessage().toLowerCase().contains("denied")) {
            nullCheckOrInterrupt(mainFragment, this).reauthenticateSmb();
          }
          return null;
        } catch (SmbException | NullPointerException e) {
          e.printStackTrace();
          nullCheckOrInterrupt(mainFragment, this).reauthenticateSmb();
          return null;
        }
        break;
      case SFTP:
        HybridFile sftpHFile = new HybridFile(OpenMode.SFTP, path);

        list = new ArrayList<LayoutElementParcelable>();

        sftpHFile.forEachChildrenFile(
            nullCheckOrInterrupt(context, this),
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
          AppConfig.toast(
              nullCheckOrInterrupt(context, this),
              nullCheckOrInterrupt(context, this)
                  .getResources()
                  .getString(R.string.failed_no_connection));
          return new Pair<>(openmode, list);
        }
        break;
      default:
        // we're neither in OTG not in SMB, load the list based on root/general filesystem
        list = new ArrayList<>();
        final OpenMode[] currentOpenMode = new OpenMode[1];
        ListFilesCommand.INSTANCE.listFiles(
            path,
            nullCheckOrInterrupt(mainFragment, this).getMainActivity().isRootExplorer(),
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
      int t = SortHandler.getSortType(nullCheckOrInterrupt(context, this), path);
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
          list, new FileListSorter(nullCheckOrInterrupt(mainFragment, this).dsort, sortby, asc));
    }

    return new Pair<>(openmode, list);
  }

  @Override
  protected void onCancelled() {
    super.onCancelled();
    listener.onAsyncTaskFinished(new Pair<>(openmode, null));
  }

  @Override
  protected void onPostExecute(Pair<OpenMode, ArrayList<LayoutElementParcelable>> list) {
    super.onPostExecute(list);
    listener.onAsyncTaskFinished(list);
  }

  private LayoutElementParcelable createListParcelables(HybridFileParcelable baseFile) {
    if (!dataUtils.isFileHidden(baseFile.getPath())) {
      String size = "";
      long longSize = 0;

      if (baseFile.isDirectory()) {
        nullCheckOrInterrupt(mainFragment, this).folder_count++;
      } else {
        if (baseFile.getSize() != -1) {
          try {
            longSize = baseFile.getSize();
            size = Formatter.formatFileSize(nullCheckOrInterrupt(context, this), longSize);
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        }

        nullCheckOrInterrupt(mainFragment, this).file_count++;
      }

      LayoutElementParcelable layoutElement =
          new LayoutElementParcelable(
              nullCheckOrInterrupt(context, this),
              baseFile.getName(nullCheckOrInterrupt(context, this)),
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

    return null;
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

  private @NonNull ArrayList<LayoutElementParcelable> listMediaCommon(
      Uri contentUri, @NonNull String[] projection, @Nullable String selection) {
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

  private ArrayList<LayoutElementParcelable> listDocs() {
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
                || path.endsWith(".rc")
                || path.endsWith(".msg")
                || path.endsWith(".odt")
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

  private ArrayList<LayoutElementParcelable> listApks() {
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

  private ArrayList<LayoutElementParcelable> listRecent() {
    UtilsHandler utilsHandler = AppConfig.getInstance().getUtilsHandler();
    final LinkedList<String> paths = utilsHandler.getHistoryLinkedList();
    ArrayList<LayoutElementParcelable> songs = new ArrayList<>();
    for (String f : paths) {
      if (!f.equals("/")) {
        HybridFileParcelable hybridFileParcelable =
            RootHelper.generateBaseFile(new File(f), showHiddenFiles);
        if (hybridFileParcelable != null) {
          hybridFileParcelable.generateMode(nullCheckOrInterrupt(mainFragment, this).getActivity());
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

  private ArrayList<LayoutElementParcelable> listRecentFiles() {
    ArrayList<LayoutElementParcelable> recentFiles = new ArrayList<>(20);
    final String[] projection = {MediaStore.Files.FileColumns.DATA};
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
    Date d = c.getTime();
    Cursor cursor =
        this.context
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
    OTGUtil.getDocumentFiles(path, context, fileFound);
  }

  private void listCloud(
      String path, CloudStorage cloudStorage, OpenMode openMode, OnFileFound fileFoundCallback)
      throws CloudPluginException {
    if (!CloudSheetFragment.isCloudProviderAvailable(context)) {
      throw new CloudPluginException();
    }

    CloudUtil.getCloudFiles(path, cloudStorage, openMode, fileFoundCallback);
  }
}
