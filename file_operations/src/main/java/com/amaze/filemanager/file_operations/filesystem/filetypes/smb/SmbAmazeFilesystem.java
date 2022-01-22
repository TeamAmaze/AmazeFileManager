/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.file_operations.filesystem.filetypes.smb;

import static com.amaze.filemanager.file_operations.filesystem.filetypes.smb.CifsContexts.SMB_URI_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jcifs.CIFSContext;
import jcifs.SmbConstants;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import kotlin.NotImplementedError;

/**
 * Root is "smb://<user>:<password>@<ip>" or "smb://<ip>" or
 * "smb://<user>:<password>@<ip>/?disableIpcSigningCheck=true" or
 * "smb://<ip>/?disableIpcSigningCheck=true" Relative paths are not supported
 */
public class SmbAmazeFilesystem extends AmazeFilesystem {
  public static final String TAG = SmbAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = SMB_URI_PREFIX;
  public static final String PARAM_DISABLE_IPC_SIGNING_CHECK = "disableIpcSigningCheck";

  private static final Pattern IPv4_PATTERN =
      Pattern.compile("[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+");
  private static final Pattern METADATA_PATTERN =
      Pattern.compile("([?][a-zA-Z]+=(\")?[a-zA-Z]+(\")?)+");

  public static final SmbAmazeFilesystem INSTANCE = new SmbAmazeFilesystem();

  static {
    AmazeFile.addFilesystem(INSTANCE);
  }

  private SmbAmazeFilesystem() {}

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @NonNull
  @Override
  public String normalize(@NonNull String pathname) {
    String canonical;
    try {
      canonical = canonicalize(pathname);
    } catch (MalformedURLException e) {
      Log.e(TAG, "Error getting SMB file canonical path", e);
      canonical = pathname.substring(0, prefixLength(pathname)) + "/";
    }
    return canonical;
  }

  @Override
  public int prefixLength(@NonNull String path) {
    if (path.length() == 0) {
      throw new IllegalArgumentException(
          "This should never happen, all paths must start with SMB prefix");
    }

    Matcher matcherMetadata = METADATA_PATTERN.matcher(path);
    if (matcherMetadata.find()) {
      return matcherMetadata.end();
    }

    Matcher matcher = IPv4_PATTERN.matcher(path);
    matcher.find();
    return matcher.end();
  }

  @NonNull
  @Override
  public String resolve(String parent, String child) {
    final String prefix = parent.substring(0, prefixLength(parent));
    final String simplePathParent = parent.substring(prefixLength(parent));
    final String simplePathChild = child.substring(prefixLength(child));

    return prefix + basicUnixResolve(simplePathParent, simplePathChild);
  }

  /** This makes no sense for SMB */
  @NonNull
  @Override
  public String getDefaultParent() {
    throw new IllegalStateException("There is no default SMB path");
  }

  @Override
  public boolean isAbsolute(AmazeFile f) {
    return f.getPath().startsWith(PREFIX);
  }

  @NonNull
  @Override
  public String resolve(AmazeFile f) {
    if (isAbsolute(f)) {
      return f.getPath();
    }

    throw new IllegalArgumentException("Relative paths are not supported");
  }

  @NonNull
  @Override
  public String canonicalize(String path) throws MalformedURLException {
    return create(path).getCanonicalPath();
  }

  public boolean exists(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      SmbFile smbFile = create(f.getPath());
      return smbFile.exists();
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Failed to get attributes for SMB file", e);
      return false;
    }
  }

  public boolean isFile(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      SmbFile smbFile = create(f.getPath());
      return smbFile.getType() == SmbConstants.TYPE_FILESYSTEM;
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Failed to get attributes for SMB file", e);
      return false;
    }
  }

  public boolean isDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      SmbFile smbFile = create(f.getPath());
      return smbFile.isDirectory();
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Failed to get attributes for SMB file", e);
      return false;
    }
  }

  public boolean isHidden(AmazeFile f) {
    try {
      SmbFile smbFile = create(f.getPath());
      return smbFile.isHidden();
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Failed to get attributes for SMB file", e);
      return false;
    }
  }

  public boolean canExecute(AmazeFile f, @NonNull ContextProvider contextProvider) {
    throw new NotImplementedError();
  }

  public boolean canWrite(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      return create(f.getPath()).canWrite();
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Error getting SMB file to check access", e);
      return false;
    }
  }

  public boolean canRead(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      return create(f.getPath()).canRead();
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Error getting SMB file to check access", e);
      return false;
    }
  }

  public boolean canAccess(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      SmbFile file = create(f.getPath());
      file.setConnectTimeout(2000);
      return file.exists();
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Error getting SMB file to check access", e);
      return false;
    }
  }

  public boolean setExecutable(AmazeFile f, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }

  public boolean setWritable(AmazeFile f, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }

  public boolean setReadable(AmazeFile f, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }

  @Override
  public long getLastModifiedTime(AmazeFile f) {
    try {
      return create(f.getPath()).getLastModified();
    } catch (MalformedURLException e) {
      Log.e(TAG, "Error getting SMB file to get last modified time", e);
      return 0;
    }
  }

  @Override
  public long getLength(AmazeFile f, @NonNull ContextProvider contextProvider)
      throws SmbException, MalformedURLException {
    return create(f.getPath()).length();
  }

  public static SmbFile create(String path) throws MalformedURLException {
    String processedPath;
    if (!path.endsWith(STANDARD_SEPARATOR + "")) {
      processedPath = path + STANDARD_SEPARATOR;
    } else {
      processedPath = path;
    }
    Uri uri = Uri.parse(processedPath);
    boolean disableIpcSigningCheck =
        Boolean.parseBoolean(uri.getQueryParameter(PARAM_DISABLE_IPC_SIGNING_CHECK));
    String userInfo = uri.getUserInfo();

    final String noExtraInfoPath;
    if (path.contains("?")) {
      noExtraInfoPath = path.substring(0, path.indexOf('?'));
    } else {
      noExtraInfoPath = path;
    }

    final CIFSContext context =
        CifsContexts.createWithDisableIpcSigningCheck(path, disableIpcSigningCheck)
            .withCredentials(createFrom(userInfo));

    return new SmbFile(noExtraInfoPath, context);
  }

  /**
   * Create {@link NtlmPasswordAuthenticator} from given userInfo parameter.
   *
   * <p>Logic borrowed directly from jcifs-ng's own code. They should make that protected
   * constructor public...
   *
   * @param userInfo authentication string, must be already URL decoded. {@link Uri} shall do this
   *     for you already
   * @return {@link NtlmPasswordAuthenticator} instance
   */
  private static @NonNull NtlmPasswordAuthenticator createFrom(@Nullable String userInfo) {
    if (!TextUtils.isEmpty(userInfo)) {
      String dom = null;
      String user = null;
      String pass = null;
      int i;
      int u;
      int end = userInfo.length();
      for (i = 0, u = 0; i < end; i++) {
        char c = userInfo.charAt(i);
        if (c == ';') {
          dom = userInfo.substring(0, i);
          u = i + 1;
        } else if (c == ':') {
          pass = userInfo.substring(i + 1);
          break;
        }
      }
      user = userInfo.substring(u, i);
      return new NtlmPasswordAuthenticator(dom, user, pass);
    } else {
      return new NtlmPasswordAuthenticator();
    }
  }

  @Override
  public boolean createFileExclusively(String pathname) throws IOException {
    create(pathname).mkdirs();
    return true;
  }

  @Override
  public boolean delete(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      create(f.getPath()).delete();
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error deleting SMB file", e);
      return false;
    }
  }

  @Override
  public String[] list(AmazeFile f, @NonNull ContextProvider contextProvider) {
    String[] list;
    try {
      list = create(f.getPath()).list();
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error listing SMB files", e);
      return null;
    }
    final String prefix = f.getPath().substring(0, prefixLength(f.getPath()));

    for (int i = 0; i < list.length; i++) {
      list[i] = SmbAmazeFilesystem.INSTANCE.normalize(prefix + getSeparator() + list[i]);
    }

    return list;
  }

  @Nullable
  @Override
  public InputStream getInputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      return create(f.getPath()).getInputStream();
    } catch (IOException e) {
      Log.e(TAG, "Error creating SMB output stream", e);
      return null;
    }
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      return create(f.getPath()).getOutputStream();
    } catch (IOException e) {
      Log.e(TAG, "Error creating SMB output stream", e);
      return null;
    }
  }

  @Override
  public boolean createDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      create(f.getPath()).mkdir();
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error creating SMB directory", e);
      return false;
    }
  }

  @Override
  public boolean rename(AmazeFile f1, AmazeFile f2, @NonNull ContextProvider contextProvider) {
    try {
      create(f1.getPath()).renameTo(create(f2.getPath()));
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error getting SMB files for a rename", e);
      return false;
    }
  }

  @Override
  public boolean setLastModifiedTime(AmazeFile f, long time) {
    try {
      create(f.getPath()).setLastModified(time);
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error getting SMB file to set modified time", e);
      return false;
    }
  }

  @Override
  public boolean setReadOnly(AmazeFile f) {
    try {
      create(f.getPath()).setReadOnly();
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error getting SMB file to set read only", e);
      return false;
    }
  }

  @Override
  public AmazeFile[] listRoots() {
    throw new NotImplementedError();
  }

  public long getTotalSpace(AmazeFile f, @NonNull ContextProvider contextProvider) {
    // TODO: Find total storage space of SMB when JCIFS adds support
    throw new NotImplementedError();
  }

  public long getFreeSpace(AmazeFile f) {
    try {
      return create(f.getPath()).getDiskFreeSpace();
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error getting SMB file to read free volume space", e);
      return 0;
    }
  }

  public long getUsableSpace(AmazeFile f) {
    // TODO: Find total storage space of SMB when JCIFS adds support
    throw new NotImplementedError();
  }
}
