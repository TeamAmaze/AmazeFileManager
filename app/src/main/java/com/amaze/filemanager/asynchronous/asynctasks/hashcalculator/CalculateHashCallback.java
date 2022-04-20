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

package com.amaze.filemanager.asynchronous.asynctasks.hashcalculator;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;

import android.content.Context;

import androidx.annotation.WorkerThread;

/** Generates hashes from files (MD5 and SHA256) */
public class CalculateHashCallback implements Callable<Hash> {

  private final AmazeFile file;
  private final ContextProvider contextProvider;

  public CalculateHashCallback(AmazeFile file, final Context context) {
    this.file = file;
    this.contextProvider = () -> context;
  }

  @WorkerThread
  @Override
  public Hash call() throws Exception {
    String md5 = null;
    String sha256 = null;

    if (!file.isDirectory(contextProvider)) {
      md5 = file.getHashMD5(contextProvider);
      sha256 = file.getHashSHA256(contextProvider);
    }

    Objects.requireNonNull(md5);
    Objects.requireNonNull(sha256);

    return new Hash(md5, sha256);
  }
}
