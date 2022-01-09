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

package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.cloudrail.si.types.SpaceAllocation;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.NotImplementedError;

public abstract class CloudAmazeFilesystem extends AmazeFilesystem {
  public static final String TAG = CloudAmazeFilesystem.class.getSimpleName();

  public abstract Account getAccount();

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
    String canonical;
    try {
      canonical = canonicalize(path);
    } catch (IOException e) {
      Log.e(TAG, "Error getting Dropbox file canonical path", e);
      canonical = path + "/";
    }
    return canonical.substring(0, canonical.length() - 1);
  }

  @NonNull
  @Override
  public String resolve(String parent, String child) {
    return getPrefix() + new File(removePrefix(parent), child);
  }

  @NonNull
  @Override
  public String getDefaultParent() {
    return getPrefix() + "/";
  }

  @Override
  public boolean isAbsolute(AmazeFile f) {
    return true; // We don't accept relative paths for cloud
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
  public String canonicalize(String path) throws IOException {
    return getPrefix() + new File(removePrefix(path)).getCanonicalPath();
  }

  public final int getBooleanAttributes(AmazeFile f) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    final CloudMetaData metadata = getAccount().getAccount().getMetadata(noPrefixPath);
    int r = 0;

    if (account.exists(noPrefixPath)) {
      r |= BA_EXISTS;

      r |= BA_REGULAR; // all files are regular (probably)

      if (metadata.getFolder()) {
        r |= BA_DIRECTORY;
      }

      // No way to know if its hidden
    }

    return r;
  }

  public final boolean checkAccess(AmazeFile f, int access) {
    switch (access) {
      case ACCESS_EXECUTE:
        return false; // You aren't executing anything at the cloud
      case ACCESS_WRITE:
        return true; // Probably, can't check
      case ACCESS_READ:
        return true; // Probably, can't check
      case ACCESS_CHECK_EXISTS:
        final CloudStorage account = getAccount().getAccount();
        Objects.requireNonNull(account);
        final String noPrefixPath = removePrefix(f.getPath());

        return account.exists(noPrefixPath);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public boolean setPermission(AmazeFile f, int access, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }

  @Override
  public long getLastModifiedTime(AmazeFile f) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    // TODO check that this actually returns seconds since epoch
    return account.getMetadata(noPrefixPath).getContentModifiedAt();
  }

  @Override
  public long getLength(AmazeFile f, @NonNull ContextProvider contextProvider) throws IOException {
    if (f.isDirectory()) {
      return 0;
    }

    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());

    return account.getMetadata(noPrefixPath).getSize();
  }

  @Override
  public boolean createFileExclusively(String pathname) throws IOException {
    return false;
  }

  @Override
  public boolean delete(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    account.delete(noPrefixPath);
    return true; // This seems to never fail
  }

  @Nullable
  @Override
  public String[] list(AmazeFile f) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    final List<CloudMetaData> metadatas = account.getChildren(noPrefixPath);

    String[] list = new String[metadatas.size()];
    for (int i = 0; i < list.length; i++) {
      list[i] = normalize(getPrefix() + metadatas.get(i).getPath());
    }
    return list;
  }

  @Nullable
  @Override
  public InputStream getInputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    return account.download(noPrefixPath);
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    throw new NotImplementedError();
  }

  @Override
  public boolean createDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    account.createFolder(noPrefixPath);
    return true; // This seems to never fail
  }

  @Override
  public boolean rename(AmazeFile f1, AmazeFile f2) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    account.move(removePrefix(f1.getPath()), removePrefix(f2.getPath()));
    return true; // This seems to never fail
  }

  @Override
  public boolean setLastModifiedTime(AmazeFile f, long time) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    final String noPrefixPath = removePrefix(f.getPath());
    // TODO check that this actually returns seconds since epoch
    account.getMetadata(noPrefixPath).setContentModifiedAt(time);
    return true; // This seems to never fail
  }

  @Override
  public boolean setReadOnly(AmazeFile f) {
    return false; // This doesn't seem possible
  }

  @Override
  public AmazeFile[] listRoots() {
    return new AmazeFile[] {new AmazeFile(getPrefix() + "/")};
  }

  @Override
  public long getSpace(AmazeFile f, int t) {
    final CloudStorage account = getAccount().getAccount();
    Objects.requireNonNull(account);
    SpaceAllocation spaceAllocation = account.getAllocation();

    switch (t) {
      case SPACE_TOTAL:
        return spaceAllocation.getTotal();
      case SPACE_FREE:
      case SPACE_USABLE:
        // The assumption is made that all free space is usable
        return spaceAllocation.getTotal() - spaceAllocation.getUsed();
      default:
        throw new IllegalStateException();
    }
  }
}
