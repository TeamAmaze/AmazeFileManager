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
import static com.amaze.filemanager.utils.SmbUtil.create;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.root.NativeOperations;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.DocumentFileAmazeFilesystem;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.DeleteFileCommand;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

/**
 * Hybrid file for handeling all types of files
 *
 * <p>This is deprecated, please use AmazeFile instead
 */
@Deprecated
public class HybridFile {

  protected static final String TAG = HybridFile.class.getSimpleName();

  protected String path;
  protected OpenMode mode;
  protected String name;

  private final DataUtils dataUtils = DataUtils.getInstance();

  static {
    FilesystemLoader.INSTANCE.toString(); // HACK forces the class to load
  }

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
    } else if (path.startsWith(DocumentFileAmazeFilesystem.DOCUMENT_FILE_PREFIX)) {
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

  public boolean isUnknownFile() {
    return mode == OpenMode.UNKNOWN;
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
      case SMB:
      case FILE:
      case DOCUMENT_FILE:
      case BOX:
      case DROPBOX:
      case GDRIVE:
      case ONEDRIVE:
        return new AmazeFile(path).lastModified();
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
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
      case DOCUMENT_FILE:
        try {
          return new AmazeFile(path).length(() -> context);
        } catch (IOException e) {
          Log.e(TAG, "Error getting length for file", e);
        }
      case ROOT:
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) return baseFile.getSize();
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
      case SFTP:
      case SMB:
      case FILE:
      case ONEDRIVE:
      case GDRIVE:
      case DROPBOX:
      case BOX:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(path).getName();
      default:
        StringBuilder builder = new StringBuilder(path);
        name = builder.substring(builder.lastIndexOf("/") + 1, builder.length());
    }
    return name;
  }

  public String getName(Context context) {
    switch (mode) {
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
      case SFTP:
      case DOCUMENT_FILE:
        return new AmazeFile(path).getName();
      case ROOT:
        return getFile().getName();
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
      case FILE:
      case ROOT:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case SFTP:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(path).getParent();
      default:
        if (path.length() == getName(context).length()) {
          return null;
        }

        int start = 0;
        int end = path.length() - getName(context).length() - 1;

        return path.substring(start, end);
    }
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
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(path).isDirectory(AppConfig::getInstance);
      case ROOT:
        isDirectory = NativeOperations.isDirectory(path);
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
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
        case DOCUMENT_FILE:
        return new AmazeFile(path).isDirectory(() -> context);
      case ROOT:
        isDirectory = NativeOperations.isDirectory(path);
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
      case FILE:
        return FileUtils.folderSize(new AmazeFile(getPath()), () -> null);
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
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
        return FileUtils.folderSize(new AmazeFile(getPath()), () -> context);
      case ROOT:
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) size = baseFile.getSize();
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
      case FILE:
      case ROOT:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case SFTP:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(path).getUsableSpace();
    }
    return size;
  }

  /** Gets total size of the disk */
  public long getTotal(Context context) {
    long size = 0l;
    switch (mode) {
      case SMB:
      case FILE:
      case ROOT:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case SFTP:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(path).getTotalSpace(() -> context);
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
          SmbFile smbFile = create(getPath());
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
        } catch (MalformedURLException | SmbException e) {
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
          CloudUtil.getCloudFiles(path, dataUtils.getAccount(mode).getAccount(), mode, onFileFound);
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
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
        ArrayList<HybridFileParcelable> result = new ArrayList<>();
        for (AmazeFile smbFile1 : new AmazeFile(getPath()).listFiles(() -> context)) {
          try {
            HybridFileParcelable baseFile = new HybridFileParcelable(create(smbFile1.getPath()));
            result.add(baseFile);
          } catch (SmbException | MalformedURLException e) {
            Log.e(TAG, "Error getting an SMB file", e);
            return null;
          }
        }
        return result;
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
    if (uriString.startsWith(SSH_URI_PREFIX)) {
      SshConnectionPool.ConnectionInfo connInfo = new SshConnectionPool.ConnectionInfo(uriString);
      return connInfo.toString();
    } else {
      Uri uri = Uri.parse(uriString);
      return formatUriForDisplayInternal(uri.getScheme(), uri.getHost(), uri.getPath());
    }
  }

  private static String formatUriForDisplayInternal(
      @NonNull String scheme, @NonNull String host, @NonNull String path) {
    return String.format("%s://%s%s", scheme, host, path);
  }

  @Nullable
  public InputStream getInputStream(Context context) {
    InputStream inputStream;

    switch (mode) {
      case SFTP:
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(getPath()).getInputStream(() -> context);
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
      case SMB:
      case FILE:
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
      case OTG:
      case DOCUMENT_FILE:
        return new AmazeFile(path).getOutputStream(() -> context);
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
    return new AmazeFile(path).exists(() -> null);
  }

  /** Helper method to check file existence in otg */
  public boolean exists(Context context) {
    return new AmazeFile(path).exists(() -> context);
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
    return new AmazeFile(path).setLastModified(date);
  }

  public void mkdir(Context context) {
    new AmazeFile(path).mkdirs(() -> context);
  }

  public boolean delete(Context context, boolean rootmode)
      throws ShellNotRunningException, SmbException {
    if (isSftp() || isSmb() || isLocal() || (isRoot() && !rootmode) || isOneDriveFile()
            || isBoxFile() || isGoogleDriveFile() || isDropBoxFile() || isOtgFile()) {
      return new AmazeFile(path).delete(() -> context);
    } else if (isRoot() && rootmode) {
      setMode(OpenMode.ROOT);
      DeleteFileCommand.INSTANCE.deleteFile(getPath());
    } else {
      DeleteOperation.deleteFile(getFile(), context);
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
