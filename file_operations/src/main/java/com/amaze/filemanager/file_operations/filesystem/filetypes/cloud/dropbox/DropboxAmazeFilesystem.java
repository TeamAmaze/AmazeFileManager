package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.dropbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFileSystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.SmbAmazeFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DropboxAmazeFilesystem extends AmazeFileSystem {
  public static final String TAG = DropboxAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "";
  public static final char SEPARATOR = '/';

  public static final DropboxAmazeFilesystem INSTANCE = new DropboxAmazeFilesystem();

  private DropboxAmazeFilesystem() {}

  @Override
  public boolean isPathOfThisFilesystem(@NonNull String path) {
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

  @NonNull
  @Override
  public String normalize(@NonNull String path) {
    return null;
  }

  @Override
  public int prefixLength(@NonNull String path) {
    return 0;
  }

  @NonNull
  @Override
  public String resolve(String parent, String child) {
    return null;
  }

  @NonNull
  @Override
  public String getDefaultParent() {
    return null;
  }

  @NonNull
  @Override
  public String fromURIPath(@NonNull String path) {
    return null;
  }

  @Override
  public boolean isAbsolute(AmazeFile f) {
    return false;
  }

  @NonNull
  @Override
  public String resolve(AmazeFile f) {
    return null;
  }

  @NonNull
  @Override
  public String canonicalize(String path) throws IOException {
    return null;
  }

  @Override
  public int getBooleanAttributes(AmazeFile f) {
    return 0;
  }

  @Override
  public boolean checkAccess(AmazeFile f, int access) {
    return false;
  }

  @Override
  public boolean setPermission(AmazeFile f, int access, boolean enable, boolean owneronly) {
    return false;
  }

  @Override
  public long getLastModifiedTime(AmazeFile f) {
    return 0;
  }

  @Override
  public long getLength(AmazeFile f) throws IOException {
    return 0;
  }

  @Override
  public boolean createFileExclusively(String pathname) throws IOException {
    return false;
  }

  @Override
  public boolean delete(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return false;
  }

  @Nullable
  @Override
  public String[] list(AmazeFile f) {
    return new String[0];
  }

  @Nullable
  @Override
  public InputStream getInputStream(AmazeFile f) {
    return null;
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return null;
  }

  @Override
  public boolean createDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return false;
  }

  @Override
  public boolean rename(AmazeFile f1, AmazeFile f2) {
    return false;
  }

  @Override
  public boolean setLastModifiedTime(AmazeFile f, long time) {
    return false;
  }

  @Override
  public boolean setReadOnly(AmazeFile f) {
    return false;
  }

  @Override
  public AmazeFile[] listRoots() {
    return new AmazeFile[0];
  }

  @Override
  public long getSpace(AmazeFile f, int t) {
    return 0;
  }

  @Override
  public int compare(AmazeFile f1, AmazeFile f2) {
    return 0;
  }

  @Override
  public int hashCode(AmazeFile f) {
    return 0;
  }
}
