package com.amaze.filemanager.filesystem.otg;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.amaze.filemanager.utils.OTGUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import kotlin.NotImplementedError;

public class OtgAmazeFilesystem extends AmazeFilesystem {
  public static final String TAG = OtgAmazeFilesystem.class.getSimpleName();

  public static final OtgAmazeFilesystem INSTANCE = new OtgAmazeFilesystem();

  public static final String PREFIX = "otg:/";

  static {
    AmazeFile.addFilesystem(INSTANCE);
  }

  private OtgAmazeFilesystem() {}

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
    return PREFIX + getSeparator();
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
  public long getLength(AmazeFile f, @NonNull ContextProvider contextProvider) throws IOException {
    @Nullable
    final Context context = contextProvider.getContext();

    if(context == null) {
      throw new IOException("Error obtaining context for OTG");
    }

    return OTGUtil.getDocumentFile(f.getPath(), context, false).length();
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
  public InputStream getInputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    @Nullable
    final Context context = contextProvider.getContext();
    ContentResolver contentResolver = context.getContentResolver();
    DocumentFile documentSourceFile = OTGUtil.getDocumentFile(f.getPath(), context, false);
    try {
      return contentResolver.openInputStream(documentSourceFile.getUri());
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Error obtaining input stream for OTG", e);
      return null;
    }
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    @Nullable
            final Context context = contextProvider.getContext();
    ContentResolver contentResolver = context.getContentResolver();
    DocumentFile documentSourceFile = OTGUtil.getDocumentFile(f.getPath(), context, true);
    try {
      return contentResolver.openOutputStream(documentSourceFile.getUri());
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Error output stream for OTG", e);
      return null;
    }
  }

  @Override
  public boolean createDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    if(f.exists()) {
      return true;
    }

    @Nullable
            final Context context = contextProvider.getContext();
    if(context == null) {
      return false;
    }

    final String parent = f.getParent();
    if(parent == null) {
      return false;
    }

    DocumentFile parentDirectory = OTGUtil.getDocumentFile(parent, context, true);


    if (parentDirectory.isDirectory()) {
      parentDirectory.createDirectory(f.getName());
      return true;
    }

    return false;
  }

  @Override
  public boolean rename(AmazeFile f1, AmazeFile f2) {
    return false;
  }

  @Override
  public boolean setLastModifiedTime(AmazeFile f, long time) {
    throw new NotImplementedError();
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
