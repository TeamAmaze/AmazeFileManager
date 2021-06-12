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

package com.amaze.filemanager.file_operations.shadows.jcifs.smb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

@Implements(SmbFile.class)
public class ShadowSmbFile {

  private File file = null;

  @Implementation
  public void __constructor__(URL url, NtlmPasswordAuthentication auth) {
    // intentionally empty
  }

  public void setFile(File file) {
    this.file = file;
  }

  @Implementation
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }

  @Implementation
  public long length() throws SmbException {
    return file.length();
  }

  @Implementation
  public SmbFile[] listFiles() {
    return new SmbFile[0];
  }
}
