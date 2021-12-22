package com.amaze.filemanager.file_operations.filesystem.filetypes.smb;

import static com.amaze.filemanager.file_operations.filesystem.filetypes.smb.CifsContexts.SMB_URI_PREFIX;
import static com.amaze.filemanager.file_operations.filesystem.filetypes.smb.CifsContexts.clearBaseContexts;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcifs.SmbConstants;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import kotlin.NotImplementedError;

/**
 * Root is
 * "smb://<user>:<password>@<ip>"
 * or "smb://<ip>"
 * or "smb://<user>:<password>@<ip>/?disableIpcSigningCheck=true"
 * or "smb://<ip>/?disableIpcSigningCheck=true"
 * Relative paths are not supported
 */
public class SmbAmazeFileSystem extends AmazeFileSystem {
  public static final String TAG = SmbAmazeFileSystem.class.getSimpleName();

  public static final String PREFIX = SMB_URI_PREFIX;
  public static final char SEPARATOR = '/';
  public static final String PARAM_DISABLE_IPC_SIGNING_CHECK = "disableIpcSigningCheck";

  private static final Pattern IPv4_PATTERN = Pattern.compile("[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+");
  private static final Pattern METADATA_PATTERN = Pattern.compile("([?][a-zA-Z]+=(\")?[a-zA-Z]+(\")?)+");

  private SmbAmazeFileSystem() { }

  public static final SmbAmazeFileSystem INSTANCE = new SmbAmazeFileSystem();

  @Override
  public boolean isPathOfThisFilesystem(String path) {
    return path.startsWith(SmbAmazeFileSystem.PREFIX);
  }

  @Override
  public char getSeparator() {
    return SEPARATOR;
  }

  @Override
  public char getPathSeparator() {
    return 0;
  }

  @Override
  public String normalize(String pathname) {
    try {
      String canonical = canonicalize(pathname);
      return canonical.substring(0, canonical.length()-1);
    } catch (MalformedURLException e) {
      Log.e(TAG, "Error getting canonical path for SMB file", e);
      return null;
    }
  }

  @Override
  public int prefixLength(String path) {
    if (path.length() == 0) {
      return 0;
    }

    Matcher matcherMetadata = METADATA_PATTERN.matcher(path);
    if(matcherMetadata.find()) {
      return matcherMetadata.end();
    }

    Matcher matcher = IPv4_PATTERN.matcher(path);
    matcher.find();
    return matcher.end();
  }

  @Override
  public String resolve(String parent, String child) {
    final String prefix = parent.substring(0, prefixLength(parent));
    final String simplePathParent = parent.substring(prefixLength(parent));
    final String simplePathChild = child.substring(prefixLength(child));

    return prefix + basicUnixResolve(simplePathParent, simplePathChild);
  }

  /**
   * This makes no sense for SMB
   */
  @Override
  public String getDefaultParent() {
    throw new IllegalStateException("There is no default SMB path");
  }

  @Override
  public String fromURIPath(String path) {
    throw new NotImplementedError();
  }

  @Override
  public boolean isAbsolute(AmazeFile f) {
    return f.getPath().startsWith(PREFIX);
  }

  @Override
  public String resolve(AmazeFile f) {
    if (isAbsolute(f)) {
      return f.getPath();
    }

    throw new IllegalArgumentException("Relative paths are not supported");
  }

  @Override
  public String canonicalize(String path) throws MalformedURLException {
    return create(path).getCanonicalPath();
  }

  @Override
  public int getBooleanAttributes(AmazeFile f) {
    try {
      SmbFile smbFile = create(f.getPath());
      int r = 0;

      if (smbFile.exists()) {
        r |= BA_EXISTS;

        if (smbFile.getType() == SmbConstants.TYPE_FILESYSTEM) {
          r |= BA_REGULAR;
        }

        if (smbFile.isDirectory()) {
          r |= BA_DIRECTORY;
        }

        if (smbFile.isHidden()) {
          r |= BA_HIDDEN;
        }
      }

      return r;
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Failed to get attributes for SMB file", e);
      return 0;
    }
  }

  @Override
  public boolean checkAccess(AmazeFile f, int access) {
    try {
      switch (access) {
        case ACCESS_EXECUTE:
          throw new NotImplementedError();
        case ACCESS_WRITE:
          return create(f.getPath()).canWrite();
        case ACCESS_READ:
          return create(f.getPath()).canRead();
        case ACCESS_CHECK_EXISTS:
          SmbFile file = create(f.getPath());
          file.setConnectTimeout(2000);
          file.exists();
        default:
          throw new IllegalStateException();
      }
    } catch (MalformedURLException | SmbException e) {
      Log.e(TAG, "Error getting SMB file to check access", e);
      return false;
    }
  }

  @Override
  public boolean setPermission(AmazeFile f, int access, boolean enable, boolean owneronly) {
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
  public long getLength(AmazeFile f) throws SmbException, MalformedURLException {
    return create(f.getPath()).length();
  }

  private static SmbFile create(String path) throws MalformedURLException {
    if(!path.endsWith(SEPARATOR + "")) {
      path = path + SEPARATOR;
    }
    Uri uri = Uri.parse(path);
    boolean disableIpcSigningCheck =
            Boolean.parseBoolean(uri.getQueryParameter(PARAM_DISABLE_IPC_SIGNING_CHECK));
    String userInfo = uri.getUserInfo();
    return new SmbFile(
            path.indexOf('?') < 0 ? path : path.substring(0, path.indexOf('?')),
            CifsContexts.createWithDisableIpcSigningCheck(path, disableIpcSigningCheck)
                    .withCredentials(createFrom(userInfo)));
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
  private static @NonNull
  NtlmPasswordAuthenticator createFrom(@Nullable String userInfo) {
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
  public boolean delete(AmazeFile f) {
    try {
      create(f.getPath()).delete();
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error deleting SMB file", e);
      return false;
    }
  }

  @Override
  public String[] list(AmazeFile f) {
    String[] list;
    try {
      list = create(f.getPath()).list();
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error listing SMB files", e);
      return null;
    }
    final String prefix = f.getPath().substring(0, prefixLength(f.getPath()));

    for (int i = 0; i < list.length; i++) {
      list[i] = SmbAmazeFileSystem.INSTANCE.normalize(prefix + getSeparator() + list[i]);
    }

    return list;
  }

  @Override
  public InputStream getInputStream(AmazeFile f) {
    try {
      return create(f.getPath()).getInputStream();
    } catch (IOException e) {
      Log.e(TAG, "Error creating SMB output stream", e);
      return null;
    }
  }

  @Override
  public OutputStream getOutputStream(AmazeFile f) {
    try {
      return create(f.getPath()).getOutputStream();
    } catch (IOException e) {
      Log.e(TAG, "Error creating SMB output stream", e);
      return null;
    }
  }

  @Override
  public boolean createDirectory(AmazeFile f) {
    try {
      create(f.getPath()).mkdir();
      return true;
    } catch (SmbException | MalformedURLException e) {
      Log.e(TAG, "Error creating SMB directory", e);
      return false;
    }
  }

  @Override
  public boolean rename(AmazeFile f1, AmazeFile f2) {
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

  @Override
  public long getSpace(AmazeFile f, int t) {
    switch (t) {
      case SPACE_TOTAL:
        // TODO: Find total storage space of SMB when JCIFS adds support
        throw new NotImplementedError();
      case SPACE_FREE:
        try {
          return create(f.getPath()).getDiskFreeSpace();
        } catch (SmbException | MalformedURLException e) {
          Log.e(TAG, "Error getting SMB file to read free volume space", e);
        }
      case SPACE_USABLE:
        // TODO: Find total storage space of SMB when JCIFS adds support
        throw new NotImplementedError();
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public int compare(AmazeFile f1, AmazeFile f2) {
    return f1.getPath().compareTo(f2.getPath());
  }

  @Override
  public int hashCode(AmazeFile f) {
    return basicUnixHashCode(f.getPath());
  }

  @Override
  public void close() throws IOException {
    clearBaseContexts();
  }
}
