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

package com.amaze.filemanager.file_operations.filesystem.filetypes.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SftpAmazeFilesystem extends AmazeFilesystem {
  private static final String TAG = SftpAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "ssh:/";

  @Override
  public String getPrefix() {
    return PREFIX;
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
  @org.jetbrains.annotations.Nullable
  @Override
  public String[] list(AmazeFile f) {
    return new String[0];
  }

  @Nullable
  @org.jetbrains.annotations.Nullable
  @Override
  public InputStream getInputStream(AmazeFile f) {
    return null;
  }

  @Nullable
  @org.jetbrains.annotations.Nullable
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
