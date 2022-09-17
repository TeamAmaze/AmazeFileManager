/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.cloudrail.si.interfaces;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.cloudrail.si.types.CloudMetaData;
import com.cloudrail.si.types.SpaceAllocation;

public class CloudStorage {

  public List<CloudMetaData> getChildren(String str) {
    return Collections.emptyList();
  }

  public void logout() {}

  public void delete(String path) {}

  public void move(String path1, String path2) {}

  public void copy(String path1, String path2) {}

  public boolean exists(String path) {
    return false;
  }

  public void loadAsString(String str) {}

  public SpaceAllocation getAllocation() {
    return new SpaceAllocation();
  }

  public void login() {}

  public String saveAsString() {
    return "";
  }

  public String getUserLogin() {
    return "";
  }

  public void useAdvancedAuthentication() {}

  public void createFolder(String extSyncFolder) {}

  public InputStream download(String path) {
    return null;
  }

  public InputStream getThumbnail(String path) {
    return null;
  }

  public void upload(String extSyncFile, InputStream outStream, long length, boolean b) {}

  public CloudMetaData getMetadata(String str) {
    return null;
  }

  public String createShareLink(String str) {
    return "";
  }
}
