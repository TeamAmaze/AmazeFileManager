package com.amaze.filemanager.file_operations.filesystem.filetypes.otg;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.SmbAmazeFilesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OtgAmazeFilesystem extends AmazeFilesystem {
  public static final String TAG = OtgAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "otg:/";

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @Override
  public int prefixLength(@NonNull String path) {
    if (path.length() == 0) {
      throw new IllegalArgumentException(
              "This should never happen, all paths must start with OTG prefix");
    }

    return super.prefixLength(path);
  }

  @NonNull
  @Override
  public String normalize(@NonNull String path) {
    return null;
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

  @Override
  public boolean isAbsolute(AmazeFile f) {
    return true; // Relative paths are not supported
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


}
