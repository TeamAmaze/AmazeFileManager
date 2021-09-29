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

package com.amaze.filemanager.filesystem;

import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;
import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_URI_PREFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.DeleteFileCommand;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.filesystem.ssh.Statvfs;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.Utils;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.SpaceAllocation;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

/** Hybrid file for handeling all types of files */
public class HybridFile {

  protected static final String TAG = HybridFile.class.getSimpleName();

  public static final String DOCUMENT_FILE_PREFIX =
      "content://com.android.externalstorage.documents";

  protected String path;
  protected OpenMode mode;
  protected String name;

  private final DataUtils dataUtils = DataUtils.getInstance();

  public HybridFile(OpenMode mode, String path) {
    this.path = path;
    this.mode = mode;
  }

  public HybridFile(OpenMode mode, String path, String name, boolean isDirectory) {
    this(mode, path);
    this.name = name;
    if (path.startsWith(SMB_URI_PREFIX) || isSmb() || isDocumentFile() || isOtgFile()) {
      Uri.Builder pathBuilder = Uri.parse(this.path).buildUpon().appendEncodedPath(name);
      if ((path.startsWith(SMB_URI_PREFIX) || isSmb()) && isDirectory) {
        pathBuilder.appendEncodedPath("/");
      }
      this.path = pathBuilder.build().toString();
    } else if (path.startsWith(SSH_URI_PREFIX) || isSftp()) {
      this.path += "/" + name;
    } else if (isRoot() && path.equals("/")) {
      // root of filesystem, don't concat another '/'
      this.path += name;
    } else {
      this.path += "/" + name;
    }
  }

  public void generateMode(Context context) {
    if (path.startsWith(SMB_URI_PREFIX)) {
      mode = OpenMode.SMB;
    } else if (path.startsWith(SSH_URI_PREFIX)) {
      mode = OpenMode.SFTP;
    } else if (path.startsWith(OTGUtil.PREFIX_OTG)) {
      mode = OpenMode.OTG;
    } else if (path.startsWith(DOCUMENT_FILE_PREFIX)) {
      mode = OpenMode.DOCUMENT_FILE;
    } else if (isCustomPath()) {
      mode = OpenMode.CUSTOM;
    } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {
      mode = OpenMode.BOX;
    } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {
      mode = OpenMode.ONEDRIVE;
    } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {
      mode = OpenMode.GDRIVE;
    } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
      mode = OpenMode.DROPBOX;
    } else if (context == null) {
      mode = OpenMode.FILE;
    } else {
      boolean rootmode =
          PreferenceManager.getDefaultSharedPreferences(context)
              .getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        mode = OpenMode.FILE;
        if (rootmode && !getFile().canRead()) {
          mode = OpenMode.ROOT;
        }
      } else {
        if (ExternalSdCardOperation.isOnExtSdCard(getFile(), context)) {
          mode = OpenMode.FILE;
        } else if (rootmode && !getFile().canRead()) {
          mode = OpenMode.ROOT;
        }

        // In some cases, non-numeric path is passed into HybridFile while mode is still
        // CUSTOM here. We are forcing OpenMode.FILE in such case too. See #2225
        if (OpenMode.UNKNOWN.equals(mode) || OpenMode.CUSTOM.equals(mode)) {
          mode = OpenMode.FILE;
        }
      }
    }
  }

  public void setMode(OpenMode mode) {
    this.mode = mode;
  }

  public OpenMode getMode() {
    return mode;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean isLocal() {
    return mode == OpenMode.FILE;
  }

  public boolean isRoot() {
    return mode == OpenMode.ROOT;
  }

  public boolean isSmb() {
    return mode == OpenMode.SMB;
  }

  public boolean isSftp() {
    return mode == OpenMode.SFTP;
  }

  public boolean isOtgFile() {
    return mode == OpenMode.OTG;
  }

  public boolean isDocumentFile() {
    return mode == OpenMode.DOCUMENT_FILE;
  }

  public boolean isBoxFile() {
    return mode == OpenMode.BOX;
  }

  public boolean isDropBoxFile() {
    return mode == OpenMode.DROPBOX;
  }

  public boolean isOneDriveFile() {
    return mode == OpenMode.ONEDRIVE;
  }

  public boolean isGoogleDriveFile() {
    return mode == OpenMode.GDRIVE;
  }

  @Nullable
  public File getFile() {
    return new File(path);
  }

  @Nullable
  public DocumentFile getDocumentFile(boolean createRecursive) {
    return OTGUtil.getDocumentFile(
        path,
        SafRootHolder.getUriRoot(),
        AppConfig.getInstance(),
        OpenMode.DOCUMENT_FILE,
        createRecursive);
  }

  HybridFileParcelable generateBaseFileFromParent() {
    ArrayList<HybridFileParcelable> arrayList =
        RootHelper.getFilesList(getFile().getParent(), true, true);
    for (HybridFileParcelable baseFile : arrayList) {
      if (baseFile.getPath().equals(path)) return baseFile;
    }
    return null;
  }

  public long lastModified() {
    switch (mode) {
      case SFTP:
        final Long returnValue =
            SshClientUtils.execute(
                new SFtpClientTemplate<Long>(path) {
                  @Override
                  public Long execute(@NonNull SFTPClient client) throws IOException {
                    return client.mtime(SshClientUtils.extractRemotePathFrom(path));
                  }
                });

        if (returnValue == null) {
          Log.e(TAG, "Error obtaining last modification time over SFTP");
        }

        return returnValue == null ? 0L : returnValue;
      case SMB:
        SmbFile smbFile = getSmbFile();
        if (smbFile != null) {
          try {
            return smbFile.lastModified();
          } catch (SmbException e) {
            Log.e(TAG, "Error getting last modified time for SMB [" + path + "]", e);
            return 0;
          }
        }
        break;
      case FILE:
        return getFile().lastModified();
      case DOCUMENT_FILE:
        return getDocumentFile(false).lastModified();
      case ROOT:
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) return baseFile.getDate();
    }
    return new File("/").lastModified();
  }

  /** Helper method to find length */
  public long length(Context context) {
    long s = 0L;
    switch (mode) {
      case SFTP:
        return ((HybridFileParcelable) this).getSize();
      case SMB:
        SmbFile smbFile = getSmbFile();
        if (smbFile != null)
          try {
            s = smbFile.length();
          } catch (SmbException e) {
            e.printStackTrace();
          }
        return s;
      case FILE:
        s = getFile().length();
        return s;
      case ROOT:
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) return baseFile.getSize();
        break;
      case DOCUMENT_FILE:
        s = getDocumentFile(false).length();
        break;
      case OTG:
        s = OTGUtil.getDocumentFile(path, context, false).length();
        break;
      case DROPBOX:
        s =
            dataUtils
                .getAccount(OpenMode.DROPBOX)
                .getMetadata(CloudUtil.stripPath(OpenMode.DROPBOX, path))
                .getSize();
        break;
      case BOX:
        s =
            dataUtils
                .getAccount(OpenMode.BOX)
                .getMetadata(CloudUtil.stripPath(OpenMode.BOX, path))
                .getSize();
        break;
      case ONEDRIVE:
        s =
            dataUtils
                .getAccount(OpenMode.ONEDRIVE)
                .getMetadata(CloudUtil.stripPath(OpenMode.ONEDRIVE, path))
                .getSize();
        break;
      case GDRIVE:
        s =
            dataUtils
                .getAccount(OpenMode.GDRIVE)
                .getMetadata(CloudUtil.stripPath(OpenMode.GDRIVE, path))
                .getSize();
        break;
      default:
        break;
    }
    return s;
  }

  public String getPath() {
    return path;
  }

  public String getSimpleName() {
    String name = null;
    switch (mode) {
      case SMB:
        SmbFile smbFile = getSmbFile();
        if (smbFile != null) return smbFile.getName();
        break;
      default:
        StringBuilder builder = new StringBuilder(path);
        name = builder.substring(builder.lastIndexOf("/") + 1, builder.length());
    }
    return name;
  }

  public String getName(Context context) {
    switch (mode) {
      case SMB:
        SmbFile smbFile = getSmbFile();
        if (smbFile != null) {
          return smbFile.getName();
        }
        return null;
      case FILE:
      case ROOT:
        return getFile().getName();
      case OTG:
        if (!Utils.isNullOrEmpty(name)) {
          return name;
        }
        return OTGUtil.getDocumentFile(path, context, false).getName();
      case DOCUMENT_FILE:
        if (!Utils.isNullOrEmpty(name)) {
          return name;
        }
        return OTGUtil.getDocumentFile(
                path, SafRootHolder.getUriRoot(), context, OpenMode.DOCUMENT_FILE, false)
            .getName();
      default:
        if (path.isEmpty()) {
          return "";
        }

        String _path = path;
        if (path.endsWith("/")) {
          _path = path.substring(0, path.length() - 1);
        }

        int lastSeparator = _path.lastIndexOf('/');

        return _path.substring(lastSeparator + 1);
    }
  }

  public SmbFile getSmbFile(int timeout) {
    try {
      SmbFile smbFile = SmbUtil.create(path);
      smbFile.setConnectTimeout(timeout);
      return smbFile;
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return null;
    }
  }

  public SmbFile getSmbFile() {
    try {
      return SmbUtil.create(path);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return null;
    }
  }

  public boolean isCustomPath() {
    return path.equals("0")
        || path.equals("1")
        || path.equals("2")
        || path.equals("3")
        || path.equals("4")
        || path.equals("5")
        || path.equals("6");
  }

  /** Helper method to get parent path */
  public String getParent(Context context) {
    switch (mode) {
      case SMB:
        SmbFile smbFile = getSmbFile();
        if (smbFile != null) {
          return smbFile.getParent();
        }
        return "";
      case FILE:
      case ROOT:
        return getFile().getParent();
      case SFTP:
      default:
        if (path.length() == getName(context).length()) {
          return null;
        }

        int start = 0;
        int end = path.length() - getName(context).length() - 1;

        return path.substring(start, end);
    }
  }

  public String getParentName() {
    StringBuilder builder = new StringBuilder(path);
    StringBuilder parentPath =
        new StringBuilder(builder.substring(0, builder.length() - (getSimpleName().length() + 1)));
    String parentName = parentPath.substring(parentPath.lastIndexOf("/") + 1, parentPath.length());
    return parentName;
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  public boolean isDirectory() {
    boolean isDirectory;
    switch (mode) {
      case SFTP:
        return isDirectory(AppConfig.getInstance());
      case SMB:
        SmbFile smbFile = getSmbFile();
        try {
          isDirectory = smbFile != null && smbFile.isDirectory();
        } catch (SmbException e) {
          e.printStackTrace();
          isDirectory = false;
        }
        break;
      case FILE:
        isDirectory = getFile().isDirectory();
        break;
      case ROOT:
        try {
          isDirectory = RootHelper.isDirectory(path, 5);
        } catch (ShellNotRunningException e) {
          e.printStackTrace();
          isDirectory = false;
        }
        break;
      case DOCUMENT_FILE:
        return getDocumentFile(false).isDirectory();
      case OTG:
        // TODO: support for this method in OTG on-the-fly
        // you need to manually call {@link RootHelper#getDocumentFile() method
        isDirectory = false;
        break;
      default:
        isDirectory = getFile().isDirectory();
        break;
    }
    return isDirectory;
  }

  public boolean isDirectory(Context context) {
    boolean isDirectory;
    switch (mode) {
      case SFTP:
        final Boolean returnValue =
            SshClientUtils.<Boolean>execute(
                new SFtpClientTemplate<Boolean>(path) {
                  @Override
                  public Boolean execute(SFTPClient client) {
                    try {
                      return client
                          .stat(SshClientUtils.extractRemotePathFrom(path))
                          .getType()
                          .equals(FileMode.Type.DIRECTORY);
                    } catch (IOException notFound) {
                      Log.e(
                          getClass().getSimpleName(),
                          "Fail to execute isDirectory for SFTP path :" + path,
                          notFound);
                      return false;
                    }
                  }
                });

        if (returnValue == null) {
          Log.e(TAG, "Error obtaining if path is directory over SFTP");
        }

        //noinspection SimplifiableConditionalExpression
        return returnValue == null ? false : returnValue;
      case SMB:
        try {
          isDirectory =
              Single.fromCallable(() -> getSmbFile().isDirectory())
                  .subscribeOn(Schedulers.io())
                  .blockingGet();
        } catch (Exception e) {
          isDirectory = false;
          if (e.getCause() != null) e.getCause().printStackTrace();
          else e.printStackTrace();
        }
        break;
      case FILE:
        isDirectory = getFile().isDirectory();
        break;
      case ROOT:
        try {
          isDirectory = RootHelper.isDirectory(path, 5);
        } catch (ShellNotRunningException e) {
          e.printStackTrace();
          isDirectory = false;
        }
        break;
      case DOCUMENT_FILE:
        isDirectory = getDocumentFile(false).isDirectory();
        break;
      case OTG:
        isDirectory = OTGUtil.getDocumentFile(path, context, false).isDirectory();
        break;
      case DROPBOX:
        isDirectory =
            dataUtils
                .getAccount(OpenMode.DROPBOX)
                .getMetadata(CloudUtil.stripPath(OpenMode.DROPBOX, path))
                .getFolder();
        break;
      case BOX:
        isDirectory =
            dataUtils
                .getAccount(OpenMode.BOX)
                .getMetadata(CloudUtil.stripPath(OpenMode.BOX, path))
                .getFolder();
        break;
      case GDRIVE:
        isDirectory =
            dataUtils
                .getAccount(OpenMode.GDRIVE)
                .getMetadata(CloudUtil.stripPath(OpenMode.GDRIVE, path))
                .getFolder();
        break;
      case ONEDRIVE:
        isDirectory =
            dataUtils
                .getAccount(OpenMode.ONEDRIVE)
                .getMetadata(CloudUtil.stripPath(OpenMode.ONEDRIVE, path))
                .getFolder();
        break;
      default:
        isDirectory = getFile().isDirectory();
        break;
    }
    return isDirectory;
  }

  /** @deprecated use {@link #folderSize(Context)} */
  public long folderSize() {
    long size = 0L;

    switch (mode) {
      case SFTP:
        return folderSize(AppConfig.getInstance());
      case SMB:
        SmbFile smbFile = getSmbFile();
        size = smbFile != null ? FileUtils.folderSize(getSmbFile()) : 0;
        break;
      case FILE:
        size = FileUtils.folderSize(getFile(), null);
        break;
      case ROOT:
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) size = baseFile.getSize();
        break;
      default:
        return 0L;
    }
    return size;
  }

  /** Helper method to get length of folder in an otg */
  public long folderSize(Context context) {

    long size = 0l;

    switch (mode) {
      case SFTP:
        final Long returnValue =
            SshClientUtils.<Long>execute(
                new SFtpClientTemplate<Long>(path) {
                  @Override
                  public Long execute(SFTPClient client) throws IOException {
                    return client.size(SshClientUtils.extractRemotePathFrom(path));
                  }
                });

        if (returnValue == null) {
          Log.e(TAG, "Error obtaining size of folder over SFTP");
        }

        return returnValue == null ? 0L : returnValue;
      case SMB:
        SmbFile smbFile = getSmbFile();
        size = (smbFile != null) ? FileUtils.folderSize(smbFile) : 0L;
        break;
      case FILE:
        size = FileUtils.folderSize(getFile(), null);
        break;
      case ROOT:
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) size = baseFile.getSize();
        break;
      case OTG:
        size = FileUtils.otgFolderSize(path, context);
        break;
      case DOCUMENT_FILE:
        final AtomicLong totalBytes = new AtomicLong(0);
        OTGUtil.getDocumentFiles(
            SafRootHolder.getUriRoot(),
            path,
            context,
            OpenMode.DOCUMENT_FILE,
            file -> totalBytes.addAndGet(FileUtils.getBaseFileSize(file, context)));
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        size =
            FileUtils.folderSizeCloud(
                mode, dataUtils.getAccount(mode).getMetadata(CloudUtil.stripPath(mode, path)));
        break;
      default:
        return 0l;
    }
    return size;
  }

  /** Gets usable i.e. free space of a device */
  public long getUsableSpace() {
    long size = 0L;
    switch (mode) {
      case SMB:
        try {
          SmbFile smbFile = getSmbFile();
          size = smbFile != null ? smbFile.getDiskFreeSpace() : 0L;
        } catch (SmbException e) {
          size = 0L;
          e.printStackTrace();
        }
        break;
      case FILE:
      case ROOT:
        size = getFile().getUsableSpace();
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        SpaceAllocation spaceAllocation = dataUtils.getAccount(mode).getAllocation();
        size = spaceAllocation.getTotal() - spaceAllocation.getUsed();
        break;
      case SFTP:
        final Long returnValue =
            SshClientUtils.<Long>execute(
                new SFtpClientTemplate<Long>(path) {
                  @Override
                  public Long execute(@NonNull SFTPClient client) throws IOException {
                    try {
                      Statvfs.Response response =
                          new Statvfs.Response(
                              path,
                              client
                                  .getSFTPEngine()
                                  .request(
                                      Statvfs.request(
                                          client, SshClientUtils.extractRemotePathFrom(path)))
                                  .retrieve());
                      return response.diskFreeSpace();
                    } catch (SFTPException e) {
                      Log.e(TAG, "Error querying server", e);
                      return 0L;
                    } catch (Buffer.BufferException e) {
                      Log.e(TAG, "Error parsing reply", e);
                      return 0L;
                    }
                  }
                });

        if (returnValue == null) {
          Log.e(TAG, "Error obtaining usable space over SFTP");
        }

        size = returnValue == null ? 0L : returnValue;
        break;
      case DOCUMENT_FILE:
        size =
            FileProperties.getDeviceStorageRemainingSpace(SafRootHolder.INSTANCE.getVolumeLabel());
        break;
      case OTG:
        // TODO: Get free space from OTG when {@link DocumentFile} API adds support
        break;
    }
    return size;
  }

  /** Gets total size of the disk */
  public long getTotal(Context context) {
    long size = 0l;
    switch (mode) {
      case SMB:
        // TODO: Find total storage space of SMB when JCIFS adds support
        try {
          SmbFile smbFile = getSmbFile();
          size = smbFile != null ? smbFile.getDiskFreeSpace() : 0L;
        } catch (SmbException e) {
          e.printStackTrace();
        }
        break;
      case FILE:
      case ROOT:
        size = getFile().getTotalSpace();
        break;
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
        SpaceAllocation spaceAllocation = dataUtils.getAccount(mode).getAllocation();
        size = spaceAllocation.getTotal();
        break;
      case SFTP:
        final Long returnValue =
            SshClientUtils.<Long>execute(
                new SFtpClientTemplate<Long>(path) {
                  @Override
                  public Long execute(@NonNull SFTPClient client) throws IOException {
                    try {
                      Statvfs.Response response =
                          new Statvfs.Response(
                              path,
                              client
                                  .getSFTPEngine()
                                  .request(
                                      Statvfs.request(
                                          client, SshClientUtils.extractRemotePathFrom(path)))
                                  .retrieve());
                      return response.diskSize();
                    } catch (SFTPException e) {
                      Log.e(TAG, "Error querying server", e);
                      return 0L;
                    } catch (Buffer.BufferException e) {
                      Log.e(TAG, "Error parsing reply", e);
                      return 0L;
                    }
                  }
                });

        if (returnValue == null) {
          Log.e(TAG, "Error obtaining total space over SFTP");
        }

        size = returnValue == null ? 0L : returnValue;
        break;
      case OTG:
        // TODO: Find total storage space of OTG when {@link DocumentFile} API adds support
        DocumentFile documentFile = OTGUtil.getDocumentFile(path, context, false);
        size = documentFile.length();
        break;
      case DOCUMENT_FILE:
        size = getDocumentFile(false).length();
        break;
    }
    return size;
  }

  /** Helper method to list children of this file */
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    switch (mode) {
      case SFTP:
        SshClientUtils.<Boolean>execute(
            new SFtpClientTemplate<Boolean>(path) {
              @Override
              public Boolean execute(SFTPClient client) {
                try {
                  for (RemoteResourceInfo info :
                      client.ls(SshClientUtils.extractRemotePathFrom(path))) {
                    boolean isDirectory = false;
                    try {
                      isDirectory = SshClientUtils.isDirectory(client, info);
                    } catch (IOException ifBrokenSymlink) {
                      Log.w(TAG, "IOException checking isDirectory(): " + info.getPath());
                      continue;
                    }
                    HybridFileParcelable f = new HybridFileParcelable(path, isDirectory, info);
                    onFileFound.onFileFound(f);
                  }
                } catch (IOException e) {
                  Log.w("DEBUG.listFiles", "IOException", e);
                  AppConfig.toast(
                      context,
                      context.getString(
                          R.string.cannot_read_directory,
                          parseAndFormatUriForDisplay(path),
                          e.getMessage()));
                }
                return true;
              }
            });
        break;
      case SMB:
        try {
          SmbFile smbFile = getSmbFile();
          if (smbFile != null) {
            for (SmbFile smbFile1 : smbFile.listFiles()) {
              HybridFileParcelable baseFile;
              try {
                SmbFile sf = new SmbFile(smbFile1.getURL(), smbFile.getContext());
                baseFile = new HybridFileParcelable(sf);
              } catch (MalformedURLException shouldNeverHappen) {
                shouldNeverHappen.printStackTrace();
                baseFile = new HybridFileParcelable(smbFile1);
              }
              onFileFound.onFileFound(baseFile);
            }
          }
        } catch (SmbException e) {
          e.printStackTrace();
        }
        break;
      case OTG:
        OTGUtil.getDocumentFiles(path, context, onFileFound);
        break;
      case DOCUMENT_FILE:
        OTGUtil.getDocumentFiles(
            SafRootHolder.getUriRoot(), path, context, OpenMode.DOCUMENT_FILE, onFileFound);
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        try {
          CloudUtil.getCloudFiles(path, dataUtils.getAccount(mode), mode, onFileFound);
        } catch (CloudPluginException e) {
          e.printStackTrace();
        }
        break;
      default:
        ListFilesCommand.INSTANCE.listFiles(
            path,
            isRoot,
            true,
            openMode -> null,
            hybridFileParcelable -> {
              onFileFound.onFileFound(hybridFileParcelable);
              return null;
            });
    }
  }

  /**
   * Helper method to list children of this file
   *
   * @deprecated use forEachChildrenFile()
   */
  public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
    ArrayList<HybridFileParcelable> arrayList = new ArrayList<>();
    switch (mode) {
      case SFTP:
        arrayList =
            SshClientUtils.execute(
                new SFtpClientTemplate<ArrayList<HybridFileParcelable>>(path) {
                  @Override
                  public ArrayList<HybridFileParcelable> execute(SFTPClient client) {
                    ArrayList<HybridFileParcelable> retval = new ArrayList<>();
                    try {
                      for (RemoteResourceInfo info :
                          client.ls(SshClientUtils.extractRemotePathFrom(path))) {
                        boolean isDirectory = false;
                        try {
                          isDirectory = SshClientUtils.isDirectory(client, info);
                        } catch (IOException ifBrokenSymlink) {
                          Log.w(TAG, "IOException checking isDirectory(): " + info.getPath());
                          continue;
                        }
                        HybridFileParcelable f = new HybridFileParcelable(path, isDirectory, info);
                        retval.add(f);
                      }
                    } catch (IOException e) {
                      Log.w("DEBUG.listFiles", "IOException", e);
                    }
                    return retval;
                  }
                });
        break;
      case SMB:
        try {
          SmbFile smbFile = getSmbFile();
          if (smbFile != null) {
            for (SmbFile smbFile1 : smbFile.listFiles()) {
              HybridFileParcelable baseFile = new HybridFileParcelable(smbFile1);
              arrayList.add(baseFile);
            }
          }
        } catch (SmbException e) {
          arrayList.clear();
          e.printStackTrace();
        }
        break;
      case OTG:
        arrayList = OTGUtil.getDocumentFilesList(path, context);
        break;
      case DOCUMENT_FILE:
        final ArrayList<HybridFileParcelable> hybridFileParcelables = new ArrayList<>();
        OTGUtil.getDocumentFiles(
            SafRootHolder.getUriRoot(),
            path,
            context,
            OpenMode.DOCUMENT_FILE,
            file -> hybridFileParcelables.add(file));
        arrayList = hybridFileParcelables;
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        try {
          arrayList = CloudUtil.listFiles(path, dataUtils.getAccount(mode), mode);
        } catch (CloudPluginException e) {
          e.printStackTrace();
          arrayList = new ArrayList<>();
        }
        break;
      default:
        arrayList = RootHelper.getFilesList(path, isRoot, true);
    }

    return arrayList;
  }

  public String getReadablePath(String path) {
    if (isSftp() || isSmb()) return parseAndFormatUriForDisplay(path);
    else return path;
  }

  public static String parseAndFormatUriForDisplay(@NonNull String uriString) {
    Uri uri = Uri.parse(uriString);
    return String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPath());
  }

  /**
   * Handles getting input stream for various {@link OpenMode}
   *
   * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
   */
  @Nullable
  public InputStream getInputStream() {
    InputStream inputStream;
    if (isSftp()) {
      return SshClientUtils.execute(
          new SFtpClientTemplate<InputStream>(path) {
            @Override
            public InputStream execute(SFTPClient client) throws IOException {
              final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path));
              return rf.new RemoteFileInputStream() {
                @Override
                public void close() throws IOException {
                  try {
                    super.close();
                  } finally {
                    rf.close();
                  }
                }
              };
            }
          });
    } else if (isSmb()) {
      try {
        inputStream = getSmbFile().getInputStream();
      } catch (IOException e) {
        inputStream = null;
        e.printStackTrace();
      }
    } else {
      try {
        inputStream = new FileInputStream(path);
      } catch (FileNotFoundException e) {
        inputStream = null;
        e.printStackTrace();
      }
    }
    return inputStream;
  }

  @Nullable
  public InputStream getInputStream(Context context) {
    InputStream inputStream;

    switch (mode) {
      case SFTP:
        inputStream =
            SshClientUtils.execute(
                new SFtpClientTemplate<InputStream>(path, false) {
                  @Override
                  public InputStream execute(final SFTPClient client) throws IOException {
                    final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path));
                    return rf.new RemoteFileInputStream() {
                      @Override
                      public void close() throws IOException {
                        try {
                          super.close();
                        } finally {
                          rf.close();
                          client.close();
                        }
                      }
                    };
                  }
                });
        break;
      case SMB:
        try {
          inputStream = getSmbFile().getInputStream();
        } catch (IOException e) {
          inputStream = null;
          e.printStackTrace();
        }
        break;
      case DOCUMENT_FILE:
        ContentResolver contentResolver = context.getContentResolver();
        DocumentFile documentSourceFile = getDocumentFile(false);
        try {
          inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          inputStream = null;
        }
        break;
      case OTG:
        contentResolver = context.getContentResolver();
        documentSourceFile = OTGUtil.getDocumentFile(path, context, false);
        try {
          inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          inputStream = null;
        }
        break;
      case DROPBOX:
        CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
        Log.d(getClass().getSimpleName(), CloudUtil.stripPath(OpenMode.DROPBOX, path));
        inputStream = cloudStorageDropbox.download(CloudUtil.stripPath(OpenMode.DROPBOX, path));
        break;
      case BOX:
        CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
        inputStream = cloudStorageBox.download(CloudUtil.stripPath(OpenMode.BOX, path));
        break;
      case GDRIVE:
        CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
        inputStream = cloudStorageGDrive.download(CloudUtil.stripPath(OpenMode.GDRIVE, path));
        break;
      case ONEDRIVE:
        CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
        inputStream = cloudStorageOneDrive.download(CloudUtil.stripPath(OpenMode.ONEDRIVE, path));
        break;
      default:
        try {
          inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
          inputStream = null;
          e.printStackTrace();
        }
        break;
    }
    return inputStream;
  }

  @Nullable
  public OutputStream getOutputStream(Context context) {
    OutputStream outputStream;
    switch (mode) {
      case SFTP:
        return SshClientUtils.execute(
            new SshClientTemplate<OutputStream>(path, false) {
              @Override
              public OutputStream execute(final SSHClient ssh) throws IOException {
                final SFTPClient client = ssh.newSFTPClient();
                final RemoteFile rf =
                    client.open(
                        SshClientUtils.extractRemotePathFrom(path),
                        EnumSet.of(
                            net.schmizz.sshj.sftp.OpenMode.WRITE,
                            net.schmizz.sshj.sftp.OpenMode.CREAT));
                return rf.new RemoteFileOutputStream() {
                  @Override
                  public void close() throws IOException {
                    try {
                      super.close();
                    } finally {
                      try {
                        rf.close();
                        client.close();
                      } catch (Exception e) {
                        Log.w(TAG, "Error closing stream", e);
                      }
                    }
                  }
                };
              }
            });
      case SMB:
        try {
          outputStream = getSmbFile().getOutputStream();
        } catch (IOException e) {
          outputStream = null;
          e.printStackTrace();
        }
        break;
      case DOCUMENT_FILE:
        ContentResolver contentResolver = context.getContentResolver();
        DocumentFile documentSourceFile = getDocumentFile(true);
        try {
          outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          outputStream = null;
        }
        break;
      case OTG:
        contentResolver = context.getContentResolver();
        documentSourceFile = OTGUtil.getDocumentFile(path, context, true);
        try {
          outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          outputStream = null;
        }
        break;
      default:
        try {
          outputStream = FileUtil.getOutputStream(getFile(), context);
        } catch (Exception e) {
          outputStream = null;
          e.printStackTrace();
        }
    }
    return outputStream;
  }

  public boolean exists() {
    boolean exists = false;
    if (isSftp()) {
      final Boolean executionReturn =
          SshClientUtils.<Boolean>execute(
              new SFtpClientTemplate<Boolean>(path) {
                @Override
                public Boolean execute(SFTPClient client) throws IOException {
                  try {
                    return client.stat(SshClientUtils.extractRemotePathFrom(path)) != null;
                  } catch (SFTPException notFound) {
                    return false;
                  }
                }
              });

      if (executionReturn == null) {
        Log.e(TAG, "Error obtaining existance of file over SFTP");
      }

      //noinspection SimplifiableConditionalExpression
      exists = executionReturn == null ? false : executionReturn;
    } else if (isSmb()) {
      try {
        SmbFile smbFile = getSmbFile(2000);
        exists = smbFile != null && smbFile.exists();
      } catch (SmbException e) {
        e.printStackTrace();
        exists = false;
      }
    } else if (isDropBoxFile()) {
      CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
      exists = cloudStorageDropbox.exists(CloudUtil.stripPath(OpenMode.DROPBOX, path));
    } else if (isBoxFile()) {
      CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
      exists = cloudStorageBox.exists(CloudUtil.stripPath(OpenMode.BOX, path));
    } else if (isGoogleDriveFile()) {
      CloudStorage cloudStorageGoogleDrive = dataUtils.getAccount(OpenMode.GDRIVE);
      exists = cloudStorageGoogleDrive.exists(CloudUtil.stripPath(OpenMode.GDRIVE, path));
    } else if (isOneDriveFile()) {
      CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
      exists = cloudStorageOneDrive.exists(CloudUtil.stripPath(OpenMode.ONEDRIVE, path));
    } else if (isLocal()) {
      exists = getFile().exists();
    } else if (isRoot()) {
      return RootHelper.fileExists(path);
    }

    return exists;
  }

  /** Helper method to check file existence in otg */
  public boolean exists(Context context) {
    boolean exists = false;
    try {
      if (isOtgFile()) {
        exists = OTGUtil.getDocumentFile(path, context, false) != null;
      } else if (isDocumentFile()) {
        exists =
            OTGUtil.getDocumentFile(
                    path, SafRootHolder.getUriRoot(), context, OpenMode.DOCUMENT_FILE, false)
                != null;
      } else return (exists());
    } catch (Exception e) {
      Log.i(getClass().getSimpleName(), "Failed to find file", e);
    }
    return exists;
  }

  /**
   * Whether file is a simple file (i.e. not a directory/smb/otg/other)
   *
   * @return true if file; other wise false
   */
  public boolean isSimpleFile() {
    return !isSmb()
        && !isOtgFile()
        && !isDocumentFile()
        && !isCustomPath()
        && !android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches()
        && (getFile() != null && !getFile().isDirectory())
        && !isOneDriveFile()
        && !isGoogleDriveFile()
        && !isDropBoxFile()
        && !isBoxFile()
        && !isSftp();
  }

  public boolean setLastModified(final long date) {
    if (isSmb()) {
      try {
        SmbFile smbFile = getSmbFile();
        if (smbFile != null) {
          smbFile.setLastModified(date);
          return true;
        } else {
          return false;
        }
      } catch (SmbException e) {
        e.printStackTrace();
        return false;
      }
    }
    File f = getFile();
    return f.setLastModified(date);
  }

  public void mkdir(Context context) {
    if (isSftp()) {
      SshClientUtils.execute(
          new SFtpClientTemplate<Void>(path) {
            @Override
            public Void execute(@NonNull SFTPClient client) {
              try {
                client.mkdir(SshClientUtils.extractRemotePathFrom(path));
              } catch (IOException e) {
                Log.e(TAG, "Error making directory over SFTP", e);
              }
              // FIXME: anything better than throwing a null to make Rx happy?
              return null;
            }
          });
    } else if (isSmb()) {
      try {
        getSmbFile().mkdirs();
      } catch (SmbException e) {
        e.printStackTrace();
      }
    } else if (isOtgFile()) {
      if (!exists(context)) {
        DocumentFile parentDirectory = OTGUtil.getDocumentFile(getParent(context), context, true);
        if (parentDirectory.isDirectory()) {
          parentDirectory.createDirectory(getName(context));
        }
      }
    } else if (isDocumentFile()) {
      if (!exists(context)) {
        DocumentFile parentDirectory =
            OTGUtil.getDocumentFile(
                getParent(context),
                SafRootHolder.getUriRoot(),
                context,
                OpenMode.DOCUMENT_FILE,
                true);
        if (parentDirectory.isDirectory()) {
          parentDirectory.createDirectory(getName(context));
        }
      }
    } else if (isDropBoxFile()) {
      CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
      try {
        cloudStorageDropbox.createFolder(CloudUtil.stripPath(OpenMode.DROPBOX, path));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (isBoxFile()) {
      CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
      try {
        cloudStorageBox.createFolder(CloudUtil.stripPath(OpenMode.BOX, path));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (isOneDriveFile()) {
      CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
      try {
        cloudStorageOneDrive.createFolder(CloudUtil.stripPath(OpenMode.ONEDRIVE, path));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (isGoogleDriveFile()) {
      CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
      try {
        cloudStorageGdrive.createFolder(CloudUtil.stripPath(OpenMode.GDRIVE, path));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else MakeDirectoryOperation.mkdir(getFile(), context);
  }

  public boolean delete(Context context, boolean rootmode)
      throws ShellNotRunningException, SmbException {
    if (isSftp()) {
      Boolean retval =
          SshClientUtils.<Boolean>execute(
              new SFtpClientTemplate(path) {
                @Override
                public Boolean execute(@NonNull SFTPClient client) throws IOException {
                  String _path = SshClientUtils.extractRemotePathFrom(path);
                  if (isDirectory(AppConfig.getInstance())) client.rmdir(_path);
                  else client.rm(_path);
                  return client.statExistence(_path) == null;
                }
              });
      return retval != null && retval;
    } else if (isSmb()) {
      try {
        getSmbFile().delete();
      } catch (SmbException e) {
        Log.e(TAG, "Error delete SMB file", e);
        throw e;
      }
    } else {
      if (isRoot() && rootmode) {
        setMode(OpenMode.ROOT);
        DeleteFileCommand.INSTANCE.deleteFile(getPath());
      } else {
        DeleteOperation.deleteFile(getFile(), context);
      }
    }
    return !exists();
  }

  /**
   * Returns the name of file excluding it's extension If no extension is found then whole file name
   * is returned
   */
  public String getNameString(Context context) {
    String fileName = getName(context);

    int extensionStartIndex = fileName.lastIndexOf(".");
    return fileName.substring(
        0, extensionStartIndex == -1 ? fileName.length() : extensionStartIndex);
  }

  /**
   * Generates a {@link LayoutElementParcelable} adapted compatible element. Currently supports only
   * local filesystem
   */
  public LayoutElementParcelable generateLayoutElement(@NonNull Context c, boolean showThumbs) {
    switch (mode) {
      case FILE:
      case ROOT:
        File file = getFile();
        LayoutElementParcelable layoutElement;
        if (isDirectory()) {

          layoutElement =
              new LayoutElementParcelable(
                  c,
                  path,
                  RootHelper.parseFilePermission(file),
                  "",
                  folderSize() + "",
                  0,
                  true,
                  file.lastModified() + "",
                  false,
                  showThumbs,
                  mode);
        } else {
          layoutElement =
              new LayoutElementParcelable(
                  c,
                  file.getPath(),
                  RootHelper.parseFilePermission(file),
                  file.getPath(),
                  file.length() + "",
                  file.length(),
                  false,
                  file.lastModified() + "",
                  false,
                  showThumbs,
                  mode);
        }
        return layoutElement;
      default:
        return null;
    }
  }
}
