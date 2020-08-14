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

package com.amaze.filemanager.database;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.database.models.utilities.Bookmark;
import com.amaze.filemanager.database.models.utilities.Grid;
import com.amaze.filemanager.database.models.utilities.Hidden;
import com.amaze.filemanager.database.models.utilities.History;
import com.amaze.filemanager.database.models.utilities.SftpEntry;
import com.amaze.filemanager.database.models.utilities.SmbEntry;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.SmbUtil;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * Created by Vishal on 29-05-2017. Class handles database with tables having list of various
 * utilities like history, hidden files, list paths, grid paths, bookmarks, SMB entry
 *
 * <p>Try to use these functions from a background thread
 */
public class UtilsHandler {

  private final Context context;

  private final UtilitiesDatabase utilitiesDatabase;

  public UtilsHandler(@NonNull Context context, @NonNull UtilitiesDatabase utilitiesDatabase) {
    this.context = context;
    this.utilitiesDatabase = utilitiesDatabase;
  }

  public enum Operation {
    HISTORY,
    HIDDEN,
    LIST,
    GRID,
    BOOKMARKS,
    SMB,
    SFTP
  }

  public void saveToDatabase(OperationData operationData) {
    AppConfig.runInBackground(
        () -> {
          switch (operationData.type) {
            case HIDDEN:
              utilitiesDatabase.hiddenEntryDao().insert(new Hidden(operationData.path));
              break;
            case HISTORY:
              utilitiesDatabase.historyEntryDao().insert(new History(operationData.path));
              break;
            case LIST:
              utilitiesDatabase
                  .listEntryDao()
                  .insert(
                      new com.amaze.filemanager.database.models.utilities.List(operationData.path));
              break;
            case GRID:
              utilitiesDatabase.gridEntryDao().insert(new Grid(operationData.path));
              break;
            case BOOKMARKS:
              utilitiesDatabase
                  .bookmarkEntryDao()
                  .insert(new Bookmark(operationData.name, operationData.path));
              break;
            case SMB:
              utilitiesDatabase
                  .smbEntryDao()
                  .insert(new SmbEntry(operationData.name, operationData.path));
              break;
            case SFTP:
              utilitiesDatabase
                  .sftpEntryDao()
                  .insert(
                      new SftpEntry(
                          operationData.path,
                          operationData.name,
                          operationData.hostKey,
                          operationData.sshKeyName,
                          operationData.sshKey));
              break;
            default:
              throw new IllegalStateException("Unidentified operation!");
          }
        });
  }

  public void removeFromDatabase(OperationData operationData) {
    AppConfig.runInBackground(
        () -> {
          switch (operationData.type) {
            case HIDDEN:
              utilitiesDatabase.hiddenEntryDao().deleteByPath(operationData.path);
              break;
            case HISTORY:
              utilitiesDatabase.historyEntryDao().deleteByPath(operationData.path);
              break;
            case LIST:
              utilitiesDatabase.listEntryDao().deleteByPath(operationData.path);
              break;
            case GRID:
              utilitiesDatabase.gridEntryDao().deleteByPath(operationData.path);
              break;
            case BOOKMARKS:
              removeBookmarksPath(operationData.name, operationData.path);
              break;
            case SMB:
              removeSmbPath(operationData.name, operationData.path);
              break;
            case SFTP:
              removeSftpPath(operationData.name, operationData.path);
              break;
            default:
              throw new IllegalStateException("Unidentified operation!");
          }
        });
  }

  public void addCommonBookmarks() {
    File sd = Environment.getExternalStorageDirectory();

    String[] dirs =
        new String[] {
          new File(sd, Environment.DIRECTORY_DCIM).getAbsolutePath(),
          new File(sd, Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
          new File(sd, Environment.DIRECTORY_MOVIES).getAbsolutePath(),
          new File(sd, Environment.DIRECTORY_MUSIC).getAbsolutePath(),
          new File(sd, Environment.DIRECTORY_PICTURES).getAbsolutePath()
        };

    for (String dir : dirs) {
      saveToDatabase(new OperationData(Operation.BOOKMARKS, new File(dir).getName(), dir));
    }
  }

  public void updateSsh(
      String connectionName,
      String oldConnectionName,
      String path,
      String hostKey,
      String sshKeyName,
      String sshKey) {

    SftpEntry entry = utilitiesDatabase.sftpEntryDao().findByName(oldConnectionName);

    entry.name = connectionName;
    entry.path = path;
    entry.hostKey = hostKey;

    if (sshKeyName != null && sshKey != null) {
      entry.sshKeyName = sshKeyName;
      entry.sshKey = sshKey;
    }

    AppConfig.runInBackground(() -> utilitiesDatabase.sftpEntryDao().update(entry));
  }

  public LinkedList<String> getHistoryLinkedList() {
    LinkedList<String> paths = new LinkedList<>();
    for (History history : utilitiesDatabase.historyEntryDao().list()) {
      paths.add(history.path);
    }
    return paths;
  }

  public ConcurrentRadixTree<VoidValue> getHiddenFilesConcurrentRadixTree() {
    ConcurrentRadixTree<VoidValue> paths =
        new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());

    for (String path : utilitiesDatabase.hiddenEntryDao().listPaths()) {
      paths.put(path, VoidValue.SINGLETON);
    }
    return paths;
  }

  public ArrayList<String> getListViewList() {
    return new ArrayList<>(Arrays.asList(utilitiesDatabase.listEntryDao().listPaths()));
  }

  public ArrayList<String> getGridViewList() {
    return new ArrayList<>(Arrays.asList(utilitiesDatabase.gridEntryDao().listPaths()));
  }

  public ArrayList<String[]> getBookmarksList() {

    ArrayList<String[]> row = new ArrayList<>();
    for (Bookmark bookmark : utilitiesDatabase.bookmarkEntryDao().list()) {
      row.add(new String[] {bookmark.name, bookmark.path});
    }
    return row;
  }

  public ArrayList<String[]> getSmbList() {
    ArrayList<String[]> retval = new ArrayList<String[]>();
    for (SmbEntry entry : utilitiesDatabase.smbEntryDao().list()) {

      try {
        String path = SmbUtil.getSmbDecryptedPath(context, entry.path);
        retval.add(new String[] {entry.name, path});
      } catch (GeneralSecurityException | IOException e) {
        e.printStackTrace();

        // failing to decrypt the path, removing entry from database
        Toast.makeText(
                context, context.getString(R.string.failed_smb_decrypt_path), Toast.LENGTH_LONG)
            .show();
        removeSmbPath(entry.name, "");
        continue;
      }
    }
    return retval;
  }

  public List<String[]> getSftpList() {
    ArrayList<String[]> retval = new ArrayList<String[]>();
    for (SftpEntry entry : utilitiesDatabase.sftpEntryDao().list()) {
      String path = SshClientUtils.decryptSshPathAsNecessary(entry.path);

      if (path == null) {
        Log.e("ERROR", "Error decrypting path: " + entry.path);
        // failing to decrypt the path, removing entry from database
        Toast.makeText(
                context, context.getString(R.string.failed_smb_decrypt_path), Toast.LENGTH_LONG)
            .show();
      } else {
        retval.add(new String[] {entry.name, path});
      }
    }
    return retval;
  }

  public String getSshHostKey(String uri) {
    uri = SshClientUtils.encryptSshPathAsNecessary(uri);
    if (uri != null) {
      return utilitiesDatabase.sftpEntryDao().getSshHostKey(uri);
    } else {
      return null;
    }
  }

  public String getSshAuthPrivateKeyName(String uri) {
    return utilitiesDatabase.sftpEntryDao().getSshAuthPrivateKeyName(uri);
  }

  public String getSshAuthPrivateKey(String uri) {
    return utilitiesDatabase.sftpEntryDao().getSshAuthPrivateKey(uri);
  }

  private void removeBookmarksPath(String name, String path) {
    utilitiesDatabase.bookmarkEntryDao().deleteByNameAndPath(name, path);
  }

  /**
   * Remove SMB entry
   *
   * @param path the path we get from saved runtime variables is a decrypted, to remove entry, we
   *     must encrypt it's password fiend first first
   */
  private void removeSmbPath(String name, String path) {
    if ("".equals(path)) utilitiesDatabase.smbEntryDao().deleteByName(name);
    else utilitiesDatabase.smbEntryDao().deleteByNameAndPath(name, path);
  }

  private void removeSftpPath(String name, String path) {
    if ("".equals(path)) utilitiesDatabase.sftpEntryDao().deleteByName(name);
    else utilitiesDatabase.sftpEntryDao().deleteByNameAndPath(name, path);
  }

  public void renameBookmark(String oldName, String oldPath, String newName, String newPath) {
    Bookmark bookmark = utilitiesDatabase.bookmarkEntryDao().findByNameAndPath(oldName, oldPath);
    bookmark.name = newName;
    bookmark.path = newPath;

    utilitiesDatabase.bookmarkEntryDao().update(bookmark);
  }

  public void renameSMB(String oldName, String oldPath, String newName, String newPath) {
    SmbEntry smbEntry = utilitiesDatabase.smbEntryDao().findByNameAndPath(oldName, oldPath);
    smbEntry.name = newName;
    smbEntry.path = newPath;

    utilitiesDatabase.smbEntryDao().update(smbEntry);
  }

  public void clearTable(Operation table) {
    AppConfig.runInBackground(
        () -> {
          switch (table) {
            case HISTORY:
              utilitiesDatabase.historyEntryDao().clear();
              break;
            default:
              break;
          }
        });
  }
}
