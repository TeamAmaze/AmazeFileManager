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

package com.amaze.filemanager.database.models;

import static com.amaze.filemanager.database.UtilsHandler.Operation.BOOKMARKS;
import static com.amaze.filemanager.database.UtilsHandler.Operation.GRID;
import static com.amaze.filemanager.database.UtilsHandler.Operation.HIDDEN;
import static com.amaze.filemanager.database.UtilsHandler.Operation.HISTORY;
import static com.amaze.filemanager.database.UtilsHandler.Operation.LIST;
import static com.amaze.filemanager.database.UtilsHandler.Operation.SFTP;
import static com.amaze.filemanager.database.UtilsHandler.Operation.SMB;

import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.UtilsHandler.Operation;

import android.text.TextUtils;

public class OperationData {
  public final Operation type;
  public final String path;
  public final String name;
  public final String hostKey;
  public final String sshKeyName;
  public final String sshKey;

  /**
   * Constructor for types {@link Operation#HIDDEN}, {@link Operation#HISTORY}, {@link
   * Operation#LIST} or {@link Operation#GRID}
   */
  public OperationData(Operation type, String path) {
    if (type != HIDDEN && type != HISTORY && type != LIST && type != GRID) {
      throw new IllegalArgumentException("Wrong constructor for object type");
    }

    this.type = type;
    this.path = path;

    name = null;
    hostKey = null;
    sshKeyName = null;
    sshKey = null;
  }

  /** Constructor for types {@link Operation#BOOKMARKS} or {@link Operation#SMB} */
  public OperationData(Operation type, String name, String path) {
    if (type != BOOKMARKS && type != SMB)
      throw new IllegalArgumentException("Wrong constructor for object type");

    this.type = type;
    this.path = path;
    this.name = name;

    hostKey = null;
    sshKeyName = null;
    sshKey = null;
  }

  /**
   * Constructor for {@link Operation#SFTP} {@param hostKey}, {@param sshKeyName} and {@param
   * sshKey} may be null for when {@link OperationData} is used for {@link
   * UtilsHandler#removeFromDatabase(OperationData)}
   */
  public OperationData(
      Operation type, String path, String name, String hostKey, String sshKeyName, String sshKey) {
    if (type != SFTP) throw new IllegalArgumentException("Wrong constructor for object type");

    this.type = type;
    this.path = path;
    this.name = name;
    this.hostKey = hostKey;
    this.sshKeyName = sshKeyName;
    this.sshKey = sshKey;
  }

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder("OperationData type=[")
            .append(type)
            .append("],path=[")
            .append(path)
            .append("]");

    if (!TextUtils.isEmpty(hostKey)) sb.append(",hostKey=[").append(hostKey).append(']');

    if (!TextUtils.isEmpty(sshKeyName))
      sb.append(",sshKeyName=[").append(sshKeyName).append("],sshKey=[redacted]");

    return sb.toString();
  }
}
