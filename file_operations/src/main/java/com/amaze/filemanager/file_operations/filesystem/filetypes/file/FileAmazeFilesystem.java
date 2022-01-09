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

package com.amaze.filemanager.file_operations.filesystem.filetypes.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/**
 * This is in in essence calls to UnixFilesystem, but that class is not public so all calls must go
 * through java.io.File
 */
public class FileAmazeFilesystem extends AmazeFilesystem {
  public static final FileAmazeFilesystem INSTANCE = new FileAmazeFilesystem();

  public static final String TAG = FileAmazeFilesystem.class.getSimpleName();

  static {
    AmazeFile.addFilesystem(INSTANCE);
  }

  private FileAmazeFilesystem() { }

  @Override
  public boolean isPathOfThisFilesystem(@NonNull String path) {
    return path.charAt(0) == getSeparator();
  }

  @Override
  public String getPrefix() {
    return null;
  }

  @Override
  public char getSeparator() {
    return File.separatorChar;
  }

  @NonNull
  @Override
  public String normalize(@NonNull String path) {
    return new File(path).getPath();
  }

  @Override
  public int prefixLength(@NonNull String path) {
    if (path.length() == 0) return 0;
    return (path.charAt(0) == '/') ? 1 : 0;
  }

  @NonNull
  @Override
  public String resolve(String parent, String child) {
    return new File(parent, child).getPath();
  }

  @NonNull
  @Override
  public String getDefaultParent() {
    return new File(new File(""), "").getPath();
  }

  @Override
  public boolean isAbsolute(AmazeFile f) {
    return new File(f.getPath()).isAbsolute();
  }

  @NonNull
  @Override
  public String resolve(AmazeFile f) {
    return new File(f.getPath()).getAbsolutePath();
  }

  @NonNull
  @Override
  public String canonicalize(String path) throws IOException {
    return new File(path).getCanonicalPath();
  }

  public boolean exists(AmazeFile f) {
    return f.exists();
  }

  public boolean isFile(AmazeFile f) {
    return f.isFile();
  }

  public boolean isDirectory(AmazeFile f) {
    return f.isDirectory();
  }

  public boolean isHidden(AmazeFile f) {
    return f.isHidden();
  }

  public boolean canExecute(AmazeFile f){
    return new File(f.getPath()).canExecute();
  }

  public boolean canWrite(AmazeFile f){
    return new File(f.getPath()).canWrite();
  }

  public boolean canRead(AmazeFile f){
    return new File(f.getPath()).canRead();
  }

  public boolean canAccess(AmazeFile f){
    return new File(f.getPath()).exists();
  }

  public boolean setExecutable(AmazeFile f, boolean enable, boolean owneronly) {
    return new File(f.getPath()).setExecutable(enable, owneronly);
  }

  public boolean setWritable(AmazeFile f, boolean enable, boolean owneronly) {
    return new File(f.getPath()).setWritable(enable, owneronly);
  }

  public boolean setReadable(AmazeFile f, boolean enable, boolean owneronly) {
    return new File(f.getPath()).setReadable(enable, owneronly);
  }

  public boolean setCheckExists(AmazeFile f, boolean enable, boolean owneronly) {
    throw new IllegalArgumentException("This properties cannot be set!");
  }


  @Override
  public long getLastModifiedTime(AmazeFile f) {
    return new File(f.getPath()).lastModified();
  }

  @Override
  public long getLength(AmazeFile f, @NonNull ContextProvider contextProvider) throws IOException {
    return new File(f.getPath()).length();
  }

  @Override
  public boolean createFileExclusively(String pathname) throws IOException {
    return new File(pathname).createNewFile();
  }

  @Override
  public boolean delete(AmazeFile f, @NonNull ContextProvider contextProvider) {
    if (f.isDirectory()) {
      AmazeFile[] children = f.listFiles();
      if (children != null) {
        for (AmazeFile child : children) {
          delete(child, contextProvider);
        }
      }

      // Try the normal way
      if (new File(f.getPath()).delete()) {
        return true;
      }

      final Context context = contextProvider.getContext();

      // Try with Storage Access Framework.
      if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        DocumentFile document =
            ExternalSdCardOperation.getDocumentFile(
                f, true, context, UriForSafPersistance.get(context));
        if (document != null && document.delete()) {
          return true;
        }
      }

      // Try the Kitkat workaround.
      if (context != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, f.getAbsolutePath());
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Delete the created entry, such that content provider will delete the file.
        resolver.delete(
            MediaStore.Files.getContentUri("external"),
            MediaStore.MediaColumns.DATA + "=?",
            new String[] {f.getAbsolutePath()});
      }

      return !f.exists();
    }

    if (new File(f.getPath()).delete()) {
      return true;
    }

    final Context context = contextProvider.getContext();

    // Try with Storage Access Framework.
    if (context != null
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        && ExternalSdCardOperation.isOnExtSdCard(f, context)) {
      DocumentFile document =
          ExternalSdCardOperation.getDocumentFile(
              f, false, context, UriForSafPersistance.get(context));
      if (document == null) {
        return true;
      }

      if (document.delete()) {
        return true;
      }
    }

    // Try the Kitkat workaround.
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
      ContentResolver resolver = context.getContentResolver();
      try {
        Uri uri = MediaStoreHack.getUriFromFile(f.getAbsolutePath(), context);
        if (uri == null) {
          return false;
        }
        resolver.delete(uri, null, null);
        return !f.exists();
      } catch (SecurityException e) {
        Log.e(TAG, "Security exception when checking for file " + f.getAbsolutePath(), e);
      }
    }

    return false;
  }

  @Nullable
  @Override
  public String[] list(AmazeFile f) {
    return new File(f.getPath()).list();
  }

  @Nullable
  @Override
  public InputStream getInputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      return new FileInputStream(f.getPath());
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Cannot find file", e);
      return null;
    }
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    try {
      if (f.canWrite()) {
        return new FileOutputStream(f.getPath());
      } else {
        final Context context = contextProvider.getContext();
        if (context == null) {
          return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          // Storage Access Framework
          DocumentFile targetDocument =
              ExternalSdCardOperation.getDocumentFile(
                  f, false, context, UriForSafPersistance.get(context));

          if (targetDocument == null) {
            return null;
          }

          return context.getContentResolver().openOutputStream(targetDocument.getUri());
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
          // Workaround for Kitkat ext SD card
          return MediaStoreHack.getOutputStream(context, f.getPath());
        }
      }

      return null;
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Cannot find file", e);
      return null;
    }
  }

  @Override
  public boolean createDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    if (new File(f.getPath()).mkdir()) {
      return true;
    }

    final Context context = contextProvider.getContext();

    // Try with Storage Access Framework.
    if (context != null
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        && ExternalSdCardOperation.isOnExtSdCard(f, context)) {
      String preferenceUri = UriForSafPersistance.get(context);

      DocumentFile document =
          ExternalSdCardOperation.getDocumentFile(f, true, context, preferenceUri);
      if (document == null) {
        return false;
      }
      // getDocumentFile implicitly creates the directory.
      return document.exists();
    }

    // Try the Kitkat workaround.
    if (context != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
      try {
        return MediaStoreHack.mkdir(context, f);
      } catch (IOException e) {
        return false;
      }
    }

    return false;
  }

  @Override
  public boolean rename(AmazeFile f1, AmazeFile f2) {
    return new File(f1.getPath()).renameTo(new File(f2.getPath()));
  }

  @Override
  public boolean setLastModifiedTime(AmazeFile f, long time) {
    return new File(f.getPath()).setLastModified(time);
  }

  @Override
  public boolean setReadOnly(AmazeFile f) {
    return new File(f.getPath()).setReadOnly();
  }

  @Override
  public AmazeFile[] listRoots() {
    File[] roots = File.listRoots();
    AmazeFile[] amazeRoots = new AmazeFile[roots.length];

    for (int i = 0; i < roots.length; i++) {
      amazeRoots[i] = new AmazeFile(roots[i].getPath());
    }

    return amazeRoots;
  }

  public long getTotalSpace(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return new File(f.getPath()).getTotalSpace();
  }
  public long getFreeSpace(AmazeFile f) {
    return new File(f.getPath()).getFreeSpace();
  }
  public long getUsableSpace(AmazeFile f) {
    return new File(f.getPath()).getUsableSpace();
  }

  @Override
  public int compare(AmazeFile f1, AmazeFile f2) {
    return new File(f1.getPath()).compareTo(new File(f2.getPath()));
  }

  @Override
  public int hashCode(AmazeFile f) {
    return new File(f.getPath()).hashCode();
  }
}
