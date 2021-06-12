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

package com.amaze.filemanager.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.utils.Utils;

import androidx.documentfile.provider.DocumentFile;

public class RootHelper {

  public static final int CHMOD_READ = 4;
  public static final int CHMOD_WRITE = 2;
  public static final int CHMOD_EXECUTE = 1;

  private static final String UNIX_INPUT_WHITELIST = "[^a-zA-Z0-9@/:}{\\-_=+.,'\"\\s]";

  public static String getCommandLineString(String input) {
    return input.replaceAll(UNIX_INPUT_WHITELIST, "");
  }

  public static HybridFileParcelable generateBaseFile(File x, boolean showHidden) {
    long size = 0;
    if (!x.isDirectory()) size = x.length();
    HybridFileParcelable baseFile =
        new HybridFileParcelable(
            x.getPath(), parseFilePermission(x), x.lastModified(), size, x.isDirectory());
    baseFile.setName(x.getName());
    baseFile.setMode(OpenMode.FILE);
    if (showHidden) {
      return (baseFile);
    } else if (!x.isHidden()) {
      return (baseFile);
    }
    return null;
  }

  public static String parseFilePermission(File f) {
    String per = "";
    if (f.canRead()) {
      per = per + "r";
    }
    if (f.canWrite()) {
      per = per + "w";
    }
    if (f.canExecute()) {
      per = per + "x";
    }
    return per;
  }

  public static String parseDocumentFilePermission(DocumentFile file) {
    String per = "";
    if (file.canRead()) {
      per = per + "r";
    }
    if (file.canWrite()) {
      per = per + "w";
    }
    if (file.canWrite()) {
      per = per + "x";
    }
    return per;
  }

  /**
   * Whether a file exist at a specified path. We try to reload a list and conform from that list of
   * parent's children that the file we're looking for is there or not.
   */
  public static boolean fileExists(String path) {
    File f = new File(path);
    String p = f.getParent();
    if (p != null && p.length() > 0) {
      List<HybridFileParcelable> filesList = new ArrayList<>();
      ListFilesCommand.INSTANCE.listFiles(
          p,
          true,
          true,
          openMode -> null,
          hybridFileParcelable -> {
            filesList.add(hybridFileParcelable);
            return null;
          });
      for (HybridFileParcelable strings : filesList) {
        if (strings.getPath() != null && strings.getPath().equals(path)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Whether toTest file is directory or not */
  public static boolean isDirectory(String toTest, int count) throws ShellNotRunningException {
    File file = new File(toTest);
    String name = file.getName();
    String parentPath = file.getParent();
    if (!Utils.isNullOrEmpty(parentPath)) {
      List<String> resultLines =
          ListFilesCommand.INSTANCE
              .executeRootCommand(getCommandLineString(parentPath), true, false)
              .getFirst();
      for (String currentLine : resultLines) {
        if (contains(currentLine.split(" "), name)) {
          try {
            HybridFileParcelable parsedFile = FileUtils.parseName(currentLine, true);
            if (parsedFile.getPermission().trim().startsWith("d")) return true;
            else if (parsedFile.getPermission().trim().startsWith("l")) {
              if (count > 5) return file.isDirectory();
              else return isDirectory(parsedFile.getLink().trim(), count + 1);
            } else return file.isDirectory();
          } catch (Exception e) {
            e.printStackTrace();
          }
          break;
        }
      }
    }
    return file.isDirectory();
  }

  static boolean contains(String[] a, String name) {
    for (String s : a) {
      // Log.e("checking",s);
      if (s.equals(name)) return true;
    }
    return false;
  }

  /**
   * Get a list of files using shell, supposing the path is not a SMB/OTG/Custom (*.apk/images)
   * TODO: Avoid parsing ls
   *
   * @param root whether root is available or not
   * @param showHidden to show hidden files
   */
  public static ArrayList<HybridFileParcelable> getFilesList(
      String path, boolean root, boolean showHidden) {
    ArrayList<HybridFileParcelable> files = new ArrayList<>();
    ListFilesCommand.INSTANCE.listFiles(
        path,
        root,
        showHidden,
        openMode -> null,
        hybridFileParcelable -> {
          files.add(hybridFileParcelable);
          return null;
        });
    return files;
  }

  /**
   * This converts from a set of booleans to OCTAL permissions notations. For use with {@link
   * com.amaze.filemanager.filesystem.root.ChangeFilePermissionsCommand->CHMOD_COMMAND} (true,
   * false, false, true, true, false, false, false, true) => 0461
   */
  public static int permissionsToOctalString(
      boolean ur,
      boolean uw,
      boolean ux,
      boolean gr,
      boolean gw,
      boolean gx,
      boolean or,
      boolean ow,
      boolean ox) {
    int u = getPermissionInOctal(ur, uw, ux) << 6;
    int g = getPermissionInOctal(gr, gw, gx) << 3;
    int o = getPermissionInOctal(or, ow, ox);
    return u | g | o;
  }

  private static int getPermissionInOctal(boolean read, boolean write, boolean execute) {
    return (read ? CHMOD_READ : 0) | (write ? CHMOD_WRITE : 0) | (execute ? CHMOD_EXECUTE : 0);
  }
}
