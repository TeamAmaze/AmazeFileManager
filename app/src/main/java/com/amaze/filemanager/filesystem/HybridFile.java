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

import static com.amaze.filemanager.filesystem.ftp.FtpConnectionPool.FTPS_URI_PREFIX;
import static com.amaze.filemanager.filesystem.ftp.FtpConnectionPool.FTP_URI_PREFIX;
import static com.amaze.filemanager.filesystem.ftp.FtpConnectionPool.SSH_URI_PREFIX;
import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException;
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.fileoperations.filesystem.root.NativeOperations;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;
import com.amaze.filemanager.filesystem.ftp.ExtensionsKt;
import com.amaze.filemanager.filesystem.ftp.FtpClientTemplate;
import com.amaze.filemanager.filesystem.ftp.FtpConnectionPool;
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils;
import com.amaze.filemanager.filesystem.root.DeleteFileCommand;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientSessionTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.filesystem.ssh.Statvfs;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.Function;
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
import android.text.format.Formatter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

/** Hybrid file for handeling all types of files */
public class HybridFile {

  private final Logger LOG = LoggerFactory.getLogger(HybridFile.class);

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
    } else if (path.startsWith(FTP_URI_PREFIX) || path.startsWith(FTPS_URI_PREFIX)) {
      mode = OpenMode.FTP;
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

  public boolean isFtp() {
    return mode == OpenMode.FTP;
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

  public boolean isCloudDriveFile() {
    return isBoxFile() || isDropBoxFile() || isOneDriveFile() || isGoogleDriveFile();
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
            NetCopyClientUtils.INSTANCE.execute(
                new SFtpClientTemplate<Long>(path, false) {
                  @Override
                  public Long execute(@NonNull SFTPClient client) throws IOException {
                    return client.mtime(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
                  }
                });

        if (returnValue == null) {
          LOG.error("Error obtaining last modification time over SFTP");
        }

        return returnValue == null ? 0L : returnValue;
      case SMB:
        SmbFile smbFile = getSmbFile();
        if (smbFile != null) {
          try {
            return smbFile.lastModified();
          } catch (SmbException e) {
            LOG.error("Error getting last modified time for SMB [" + path + "]", e);
            return 0;
          }
        }
        break;
      case FTP:
        FTPFile ftpFile = getFtpFile();
        return ftpFile != null ? ftpFile.getTimestamp().getTimeInMillis() : 0L;
      case NFS:
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
        if (this instanceof HybridFileParcelable) {
          return ((HybridFileParcelable) this).getSize();
        } else {
          return NetCopyClientUtils.INSTANCE.execute(
              new SFtpClientTemplate<Long>(path, false) {
                @Override
                public Long execute(@NonNull SFTPClient client) throws IOException {
                  return client.size(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
                }
              });
        }
      case SMB:
        s =
            Single.fromCallable(
                    () -> {
                      SmbFile smbFile = getSmbFile();
                      if (smbFile != null) {
                        try {
                          return smbFile.length();
                        } catch (SmbException e) {
                          LOG.warn("failed to get length for smb file", e);
                          return 0L;
                        }
                      } else {
                        return 0L;
                      }
                    })
                .subscribeOn(Schedulers.io())
                .blockingGet();
        return s;
      case FTP:
        FTPFile ftpFile = getFtpFile();
        s = ftpFile != null ? ftpFile.getSize() : 0L;
        return s;
      case NFS:
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
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
        s =
            Single.fromCallable(
                    () ->
                        dataUtils
                            .getAccount(mode)
                            .getMetadata(CloudUtil.stripPath(mode, path))
                            .getSize())
                .subscribeOn(Schedulers.io())
                .blockingGet();
        return s;
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
      LOG.warn("failed to get smb file with timeout", e);
      return null;
    }
  }

  public SmbFile getSmbFile() {
    try {
      return SmbUtil.create(path);
    } catch (MalformedURLException e) {
      LOG.warn("failed to get smb file", e);
      return null;
    }
  }

  @Nullable
  public FTPFile getFtpFile() {
    return NetCopyClientUtils.INSTANCE.execute(
        new FtpClientTemplate<FTPFile>(path, false) {
          public FTPFile executeWithFtpClient(@NonNull FTPClient ftpClient) throws IOException {
            return ftpClient.mlistFile(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
          }
        });
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
      case FTP:
        return isDirectory(AppConfig.getInstance());
      case SMB:
        SmbFile smbFile = getSmbFile();
        try {
          isDirectory = smbFile != null && smbFile.isDirectory();
        } catch (SmbException e) {
          LOG.warn("failed to get isDirectory for smb file", e);
          isDirectory = false;
        }
        break;
      case FILE:
        isDirectory = getFile().isDirectory();
        break;
      case ROOT:
        isDirectory = NativeOperations.isDirectory(path);
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
            NetCopyClientUtils.INSTANCE.<SSHClient, Boolean>execute(
                new SFtpClientTemplate<Boolean>(path, false) {
                  @Override
                  public Boolean execute(@NonNull SFTPClient client) {
                    try {
                      return client
                          .stat(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path))
                          .getType()
                          .equals(FileMode.Type.DIRECTORY);
                    } catch (IOException notFound) {
                      LOG.error("Fail to execute isDirectory for SFTP path :" + path, notFound);
                      return false;
                    }
                  }
                });

        if (returnValue == null) {
          LOG.error("Error obtaining if path is directory over SFTP");
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
          LOG.warn("failed to get isDirectory with context for smb file", e);
        }
        break;
      case FTP:
        FTPFile ftpFile = getFtpFile();
        isDirectory = ftpFile != null && ftpFile.isDirectory();
        break;
      case FILE:
        isDirectory = getFile().isDirectory();
        break;
      case ROOT:
        isDirectory = NativeOperations.isDirectory(path);
        break;
      case DOCUMENT_FILE:
        isDirectory = getDocumentFile(false).isDirectory();
        break;
      case OTG:
        isDirectory = OTGUtil.getDocumentFile(path, context, false).isDirectory();
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        isDirectory =
            Single.fromCallable(
                    () ->
                        dataUtils
                            .getAccount(mode)
                            .getMetadata(CloudUtil.stripPath(mode, path))
                            .getFolder())
                .subscribeOn(Schedulers.io())
                .blockingGet();
        break;
      default:
        isDirectory = getFile().isDirectory();
        break;
    }
    return isDirectory;
  }

  /**
   * @deprecated use {@link #folderSize(Context)}
   */
  public long folderSize() {
    long size = 0L;

    switch (mode) {
      case SFTP:
      case FTP:
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

    long size = 0L;

    switch (mode) {
      case SFTP:
        final Long returnValue =
            NetCopyClientUtils.INSTANCE.<SSHClient, Long>execute(
                new SFtpClientTemplate<Long>(path, false) {
                  @Override
                  public Long execute(@NonNull SFTPClient client) throws IOException {
                    return client.size(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
                  }
                });

        if (returnValue == null) {
          LOG.error("Error obtaining size of folder over SFTP");
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
      case FTP:

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
          LOG.warn("failed to get usage space for smb file", e);
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
            NetCopyClientUtils.INSTANCE.<SSHClient, Long>execute(
                new SFtpClientTemplate<Long>(path, false) {
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
                                          client,
                                          NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path)))
                                  .retrieve());
                      return response.diskFreeSpace();
                    } catch (SFTPException e) {
                      LOG.error("Error querying server", e);
                      return 0L;
                    } catch (Buffer.BufferException e) {
                      LOG.error("Error parsing reply", e);
                      return 0L;
                    }
                  }
                });

        if (returnValue == null) {
          LOG.error("Error obtaining usable space over SFTP");
        }

        size = returnValue == null ? 0L : returnValue;
        break;
      case DOCUMENT_FILE:
        size =
            FileProperties.getDeviceStorageRemainingSpace(SafRootHolder.INSTANCE.getVolumeLabel());
        break;
      case FTP:
        /*
         * Quirk, or dirty trick.
         *
         * I think 99.9% FTP servers in this world will not report their disk's remaining space,
         * simply because they are not Serv-U (using AVBL command) or IIS (extended LIST command on
         * it own). But it doesn't make sense to simply block write to FTP servers either, hence
         * this value Integer.MAX_VALUE = 2048MB, which should be suitable for 99% of the cases.
         *
         * File sizes bigger than this, either Android device (unless TV boxes) would have
         * difficulty to handle, either client and server side. In that case I shall recommend you
         * to send it in splits, or just move to better transmission mechanism, like WiFi Direct
         * as provided by Amaze File Utilities ;)
         *
         * - TranceLove
         */
        size = Integer.MAX_VALUE;
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
          LOG.warn("failed to get total space for smb file", e);
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
            NetCopyClientUtils.INSTANCE.<SSHClient, Long>execute(
                new SFtpClientTemplate<Long>(path, false) {
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
                                          client,
                                          NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path)))
                                  .retrieve());
                      return response.diskSize();
                    } catch (SFTPException e) {
                      LOG.error("Error querying server", e);
                      return 0L;
                    } catch (Buffer.BufferException e) {
                      LOG.error("Error parsing reply", e);
                      return 0L;
                    }
                  }
                });

        if (returnValue == null) {
          LOG.error("Error obtaining total space over SFTP");
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
      case FTP:
        size = 0L;
    }
    return size;
  }

  /** Helper method to list children of this file */
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    switch (mode) {
      case SFTP:
        NetCopyClientUtils.INSTANCE.<SSHClient, Boolean>execute(
            new SFtpClientTemplate<Boolean>(path, false) {
              @Override
              public Boolean execute(@NonNull SFTPClient client) {
                try {
                  for (RemoteResourceInfo info :
                      client.ls(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path))) {
                    boolean isDirectory = false;
                    try {
                      isDirectory = SshClientUtils.isDirectory(client, info);
                    } catch (IOException ifBrokenSymlink) {
                      LOG.warn("IOException checking isDirectory(): " + info.getPath());
                      continue;
                    }
                    HybridFileParcelable f = new HybridFileParcelable(path, isDirectory, info);
                    onFileFound.onFileFound(f);
                  }
                } catch (IOException e) {
                  LOG.warn("IOException", e);
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
                LOG.warn("failed to get children file for smb", shouldNeverHappen);
                baseFile = new HybridFileParcelable(smbFile1);
              }
              onFileFound.onFileFound(baseFile);
            }
          }
        } catch (SmbException e) {
          LOG.warn("failed to get children file for smb file", e);
        }
        break;
      case FTP:
        String thisPath = NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path);
        NetCopyClientUtils.INSTANCE.execute(
            new FtpClientTemplate<Boolean>(path, false) {
              public Boolean executeWithFtpClient(@NonNull FTPClient ftpClient) throws IOException {
                for (FTPFile ftpFile : ftpClient.listFiles(thisPath)) {
                  onFileFound.onFileFound(new HybridFileParcelable(path, ftpFile));
                }
                return true;
              }
            });
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
          LOG.warn("failed to get children file for cloud file", e);
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
    forEachChildrenFile(context, isRoot, arrayList::add);
    return arrayList;
  }

  public String getReadablePath(String path) {
    if (isSftp() || isSmb() || isFtp()) return parseAndFormatUriForDisplay(path);
    else return path;
  }

  public static String parseAndFormatUriForDisplay(@NonNull String uriString) {
    if (uriString.startsWith(SSH_URI_PREFIX)
        || uriString.startsWith(FTP_URI_PREFIX)
        || uriString.startsWith(FTPS_URI_PREFIX)) {
      FtpConnectionPool.ConnectionInfo connInfo = new FtpConnectionPool.ConnectionInfo(uriString);
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

  /**
   * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
   */
  @Nullable
  public InputStream getInputStream() {
    return getInputStream(AppConfig.getInstance());
  }

  /**
   * Handles getting input stream for various {@link OpenMode}
   *
   * @param context
   * @return
   */
  @Nullable
  public InputStream getInputStream(Context context) {
    InputStream inputStream;

    switch (mode) {
      case SFTP:
        inputStream =
            SshClientUtils.execute(
                new SFtpClientTemplate<InputStream>(path, false) {
                  @Override
                  public InputStream execute(@NonNull final SFTPClient client) throws IOException {
                    final RemoteFile rf =
                        client.open(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
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
          LOG.warn("failed to get input stream for smb file", e);
        }
        break;
      case FTP:
        inputStream =
            NetCopyClientUtils.INSTANCE.execute(
                new FtpClientTemplate<InputStream>(path, false) {
                  public InputStream executeWithFtpClient(@NonNull FTPClient ftpClient)
                      throws IOException {
                    return ftpClient.retrieveFileStream(
                        NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
                  }
                });
        break;
      case DOCUMENT_FILE:
        ContentResolver contentResolver = context.getContentResolver();
        DocumentFile documentSourceFile = getDocumentFile(false);
        try {
          inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          LOG.warn("failed to get input stream for document file", e);
          inputStream = null;
        }
        break;
      case OTG:
        contentResolver = context.getContentResolver();
        documentSourceFile = OTGUtil.getDocumentFile(path, context, false);
        try {
          inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          LOG.warn("failed to get input stream for otg file", e);
          inputStream = null;
        }
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        CloudStorage cloudStorageOneDrive = dataUtils.getAccount(mode);
        LOG.debug(CloudUtil.stripPath(mode, path));
        inputStream = cloudStorageOneDrive.download(CloudUtil.stripPath(mode, path));
        break;
      default:
        try {
          inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
          inputStream = null;
          LOG.warn("failed to get input stream", e);
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
        return NetCopyClientUtils.INSTANCE.execute(
            new SshClientTemplate<OutputStream>(path, false) {
              @Override
              public OutputStream executeWithSSHClient(@NonNull final SSHClient ssh)
                  throws IOException {
                final SFTPClient client = ssh.newSFTPClient();
                final RemoteFile rf =
                    client.open(
                        NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path),
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
                        LOG.warn("Error closing stream", e);
                      }
                    }
                  }
                };
              }
            });
      case FTP:
        outputStream =
            NetCopyClientUtils.INSTANCE.execute(
                new FtpClientTemplate<OutputStream>(path, false) {
                  public OutputStream executeWithFtpClient(@NonNull FTPClient ftpClient)
                      throws IOException {
                    ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
                    String remotePath = NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path);
                    OutputStream _outputStream = ftpClient.storeFileStream(remotePath);
                    return new OutputStream() {
                      @Override
                      public void write(int b) throws IOException {
                        _outputStream.write(b);
                      }

                      @Override
                      public void close() throws IOException {
                        _outputStream.close();
                        ftpClient.completePendingCommand();
                      }
                    };
                  }
                });
        return outputStream;
      case SMB:
        try {
          outputStream = getSmbFile().getOutputStream();
        } catch (IOException e) {
          outputStream = null;
          LOG.warn("failed to get output stream for smb file", e);
        }
        break;
      case DOCUMENT_FILE:
        ContentResolver contentResolver = context.getContentResolver();
        DocumentFile documentSourceFile = getDocumentFile(true);
        try {
          outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          LOG.warn("failed to get output stream for document file", e);
          outputStream = null;
        }
        break;
      case OTG:
        contentResolver = context.getContentResolver();
        documentSourceFile = OTGUtil.getDocumentFile(path, context, true);
        try {
          outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          LOG.warn("failed to get output stream for otg file", e);
          outputStream = null;
        }
        break;
      default:
        try {
          outputStream = FileUtil.getOutputStream(getFile(), context);
        } catch (Exception e) {
          outputStream = null;
          LOG.warn("failed to get output stream", e);
        }
    }
    return outputStream;
  }

  public boolean exists() {
    boolean exists = false;
    if (isSftp()) {
      final Boolean executionReturn =
          NetCopyClientUtils.INSTANCE.<SSHClient, Boolean>execute(
              new SFtpClientTemplate<Boolean>(path, false) {
                @Override
                public Boolean execute(SFTPClient client) throws IOException {
                  try {
                    return client.stat(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path))
                        != null;
                  } catch (SFTPException notFound) {
                    return false;
                  }
                }
              });
      if (executionReturn == null) {
        LOG.error("Error obtaining existance of file over SFTP");
      }
      //noinspection SimplifiableConditionalExpression
      exists = executionReturn == null ? false : executionReturn;
    } else if (isSmb()) {
      try {
        SmbFile smbFile = getSmbFile(2000);
        exists = smbFile != null && smbFile.exists();
      } catch (SmbException e) {
        LOG.warn("failed to find existence for smb file", e);
        exists = false;
      }
    } else if (isFtp()) {
      exists = getFtpFile() != null;
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
      LOG.info("Failed to find file", e);
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
        && !isSftp()
        && !isFtp();
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
        LOG.warn("failed to get last modified for smb file", e);
        return false;
      }
    } else if (isFtp()) {
      return Boolean.TRUE.equals(
          NetCopyClientUtils.INSTANCE.execute(
              new FtpClientTemplate<Boolean>(path, false) {
                public Boolean executeWithFtpClient(@NonNull FTPClient ftpClient)
                    throws IOException {
                  Calendar calendar = Calendar.getInstance();
                  calendar.setTimeInMillis(date);
                  DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                  df.setCalendar(calendar);
                  return ftpClient.setModificationTime(
                      NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path),
                      df.format(calendar.getTime()));
                }
              }));
    } else {
      File f = getFile();
      return f.setLastModified(date);
    }
  }

  public void mkdir(Context context) {
    if (isSftp()) {
      NetCopyClientUtils.INSTANCE.execute(
          new SFtpClientTemplate<Void>(path, false) {
            @Override
            public Void execute(@NonNull SFTPClient client) {
              try {
                client.mkdir(NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
              } catch (IOException e) {
                LOG.error("Error making directory over SFTP", e);
              }
              // FIXME: anything better than throwing a null to make Rx happy?
              return null;
            }
          });
    } else if (isFtp()) {
      NetCopyClientUtils.INSTANCE.execute(
          new FtpClientTemplate<Void>(path, false) {
            public Void executeWithFtpClient(@NonNull FTPClient ftpClient) throws IOException {
              ExtensionsKt.makeDirectoryTree(
                  ftpClient, NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
              return null;
            }
          });
    } else if (isSmb()) {
      try {
        getSmbFile().mkdirs();
      } catch (SmbException e) {
        LOG.warn("failed to make dir for smb file", e);
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
    } else if (isCloudDriveFile()) {
      CloudStorage cloudStorageDropbox = dataUtils.getAccount(mode);
      try {
        cloudStorageDropbox.createFolder(CloudUtil.stripPath(mode, path));
      } catch (Exception e) {
        LOG.warn("failed to create folder for cloud file", e);
      }
    } else MakeDirectoryOperation.mkdir(getFile(), context);
  }

  public boolean delete(Context context, boolean rootmode)
      throws ShellNotRunningException, SmbException {
    if (isSftp()) {
      Boolean retval =
          NetCopyClientUtils.INSTANCE.<SSHClient, Boolean>execute(
              new SFtpClientTemplate<Boolean>(path, false) {
                @Override
                public Boolean execute(@NonNull SFTPClient client) throws IOException {
                  String _path = NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path);
                  if (isDirectory(AppConfig.getInstance())) client.rmdir(_path);
                  else client.rm(_path);
                  return client.statExistence(_path) == null;
                }
              });
      return retval != null && retval;
    } else if (isFtp()) {
      Boolean retval =
          NetCopyClientUtils.INSTANCE.<FTPClient, Boolean>execute(
              new FtpClientTemplate<Boolean>(path, false) {
                @Override
                public Boolean executeWithFtpClient(@NonNull FTPClient ftpClient)
                    throws IOException {
                  return ftpClient.deleteFile(
                      NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path));
                }
              });
      return retval != null && retval;
    } else if (isSmb()) {
      try {
        getSmbFile().delete();
      } catch (SmbException e) {
        LOG.error("Error delete SMB file", e);
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
        if (isDirectory(c)) {

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

  /**
   * Open this hybrid file
   *
   * @param activity
   * @param doShowDialog should show confirmation dialog (in case of deeplink)
   */
  public void openFile(MainActivity activity, boolean doShowDialog) {
    if (doShowDialog) {
      AtomicReference<String> md5 = new AtomicReference<>(activity.getString(R.string.calculating));
      AtomicReference<String> sha256 =
          new AtomicReference<>(activity.getString(R.string.calculating));
      AtomicReference<String> pathToDisplay = new AtomicReference<>();
      pathToDisplay.set(path);
      if (isSftp() || isSmb() || isFtp()) {
        LOG.debug("convert authorised path to simple path for display");
        pathToDisplay.set(parseAndFormatUriForDisplay(path));
      }

      AtomicReference<String> dialogContent =
          new AtomicReference<>(
              String.format(
                  activity.getResources().getString(R.string.open_file_confirmation),
                  getName(activity),
                  pathToDisplay.get(),
                  Formatter.formatShortFileSize(activity, length(activity)),
                  md5.get(),
                  sha256.get()));
      MaterialDialog dialog =
          GeneralDialogCreation.showOpenFileDeeplinkDialog(
              this, activity, dialogContent.get(), () -> openFileInternal(activity));
      dialog.show();
      getMd5Checksum(
          activity,
          s -> {
            md5.set(s);
            dialogContent.set(
                String.format(
                    activity.getResources().getString(R.string.open_file_confirmation),
                    getName(activity),
                    pathToDisplay.get(),
                    Formatter.formatShortFileSize(activity, length(activity)),
                    md5.get(),
                    sha256.get()));
            dialog.setContent(dialogContent.get());
            return null;
          });
      getSha256Checksum(
          activity,
          s -> {
            sha256.set(s);
            dialogContent.set(
                String.format(
                    activity.getResources().getString(R.string.open_file_confirmation),
                    getName(activity),
                    pathToDisplay.get(),
                    Formatter.formatShortFileSize(activity, length(activity)),
                    md5.get(),
                    sha256.get()));
            dialog.setContent(dialogContent.get());
            return null;
          });
    } else {
      openFileInternal(activity);
    }
  }

  public void getMd5Checksum(Context context, Function<String, Void> callback) {
    Single.fromCallable(
            () -> {
              try {
                switch (mode) {
                  case SFTP:
                    String md5Command = "md5sum -b \"%s\" | cut -c -32";
                    return SshClientUtils.execute(getSftpHash(md5Command));
                  default:
                    byte[] b = createChecksum(context);
                    String result = "";

                    for (byte aB : b) {
                      result += Integer.toString((aB & 0xff) + 0x100, 16).substring(1);
                    }
                    return result;
                }
              } catch (Exception e) {
                LOG.warn("failed to get md5 checksum for sftp file", e);
                return context.getString(R.string.error);
              }
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new SingleObserver<String>() {
              @Override
              public void onSubscribe(Disposable d) {}

              @Override
              public void onSuccess(String t) {
                callback.apply(t);
              }

              @Override
              public void onError(Throwable e) {
                LOG.warn("failed to get md5 for sftp file", e);
                callback.apply(context.getString(R.string.error));
              }
            });
  }

  public void getSha256Checksum(Context context, Function<String, Void> callback) {
    Single.fromCallable(
            () -> {
              try {
                switch (mode) {
                  case SFTP:
                    String shaCommand = "sha256sum -b \"%s\" | cut -c -64";
                    return SshClientUtils.execute(getSftpHash(shaCommand));
                  default:
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                    byte[] input = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                    int length;
                    InputStream inputStream = getInputStream(context);
                    while ((length = inputStream.read(input)) != -1) {
                      if (length > 0) messageDigest.update(input, 0, length);
                    }

                    byte[] hash = messageDigest.digest();

                    StringBuilder hexString = new StringBuilder();

                    for (byte aHash : hash) {
                      // convert hash to base 16
                      String hex = Integer.toHexString(0xff & aHash);
                      if (hex.length() == 1) hexString.append('0');
                      hexString.append(hex);
                    }
                    inputStream.close();
                    return hexString.toString();
                }
              } catch (IOException | NoSuchAlgorithmException ne) {
                LOG.warn("failed to get sha checksum for sftp file", ne);
                return context.getString(R.string.error);
              }
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new SingleObserver<String>() {
              @Override
              public void onSubscribe(Disposable d) {}

              @Override
              public void onSuccess(String t) {
                callback.apply(t);
              }

              @Override
              public void onError(Throwable e) {
                LOG.warn("failed to get sha256 for sftp file", e);
                callback.apply(context.getString(R.string.error));
              }
            });
  }

  private SshClientSessionTemplate<String> getSftpHash(String command) {
    return new SshClientSessionTemplate<String>(path) {
      @Override
      public String execute(Session session) throws IOException {
        String extractedPath = NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path);
        String fullCommand = String.format(command, extractedPath);
        Session.Command cmd = session.exec(fullCommand);
        String result = new String(IOUtils.readFully(cmd.getInputStream()).toByteArray());
        cmd.close();
        if (cmd.getExitStatus() == 0) {
          return result;
        } else {
          return null;
        }
      }
    };
  }

  private byte[] createChecksum(Context context) throws Exception {
    InputStream fis = getInputStream(context);

    byte[] buffer = new byte[8192];
    MessageDigest complete = MessageDigest.getInstance("MD5");
    int numRead;

    do {
      numRead = fis.read(buffer);
      if (numRead > 0) {
        complete.update(buffer, 0, numRead);
      }
    } while (numRead != -1);

    fis.close();
    return complete.digest();
  }

  private void openFileInternal(MainActivity activity) {
    switch (mode) {
      case SMB:
        FileUtils.launchSMB(this, activity);
        break;
      case SFTP:
        Toast.makeText(
                activity,
                activity.getResources().getString(R.string.please_wait),
                Toast.LENGTH_LONG)
            .show();
        SshClientUtils.launchSftp(this, activity);
        break;
      case OTG:
        FileUtils.openFile(
            OTGUtil.getDocumentFile(path, activity, false), activity, activity.getPrefs());
        break;
      case DOCUMENT_FILE:
        FileUtils.openFile(
            OTGUtil.getDocumentFile(
                path, SafRootHolder.getUriRoot(), activity, OpenMode.DOCUMENT_FILE, false),
            activity,
            activity.getPrefs());
        break;
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
        Toast.makeText(
                activity,
                activity.getResources().getString(R.string.please_wait),
                Toast.LENGTH_LONG)
            .show();
        CloudUtil.launchCloud(this, mode, activity);
        break;
      default:
        FileUtils.openFile(new File(path), activity, activity.getPrefs());
        break;
    }
  }
}
