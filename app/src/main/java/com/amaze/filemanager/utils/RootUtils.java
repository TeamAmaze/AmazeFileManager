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

package com.amaze.filemanager.utils;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.RootHelper;

import android.os.Build;

public class RootUtils {
  public static final int CHMOD_READ = 4, CHMOD_WRITE = 2, CHMOD_EXECUTE = 1;
  public static final String DATA_APP_DIR = "/data/app";
  /**
   * This is the chmod command, it should be used with String.format(). String.format(CHMOD_COMMAND,
   * options, permsOctalInt, path);
   */
  public static final String CHMOD_COMMAND = "chmod %s %o \"%s\"";

  private static final String LS = "ls -lAnH \"%\" --color=never";
  private static final String LSDIR = "ls -land \"%\" --color=never";
  public static final String SYSTEM_APP_DIR = "/system/app";
  private static final Pattern mLsPattern;

  static {
    mLsPattern = Pattern.compile(".[rwxsStT-]{9}\\s+.*");
  }

  /**
   * Mount filesystem associated with path for writable access (rw) Since we don't have the root of
   * filesystem to remount, we need to parse output of # mount command.
   *
   * @param path the path on which action to perform
   * @return String the root of mount point that was ro, and mounted to rw; null otherwise
   */
  private static String mountFileSystemRW(String path) throws ShellNotRunningException {
    String command = "mount";
    ArrayList<String> output = RootHelper.runShellCommandToList(command);
    String mountPoint = "", types = null, mountArgument = null;
    for (String line : output) {
      String[] words = line.split(" ");

      // mount command output for older Androids
      // <code>/dev/block/vda /system ext4 ro,seclabel,relatime,data=ordered 0 0</code>
      String mountPointOutputFromShell = words[1];
      String mountPointFileSystemTypeFromShell = words[2];
      String mountPointArgumentFromShell = words[3];
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // mount command output for Android version >= 7
        // <code>/dev/block/bootdevice/by-name/system on /system type ext4
        // (ro,seclabel,relatime,data=ordered)</code>
        mountPointOutputFromShell = words[2];
        mountPointFileSystemTypeFromShell = words[4];
        mountPointArgumentFromShell = words[5];
      }

      if (path.startsWith(mountPointOutputFromShell)) {
        // current found point is bigger than last one, hence not a conflicting one
        // we're finding the best match, this omits for eg. / and /sys when we're actually
        // looking for /system
        if (mountPointOutputFromShell.length() > mountPoint.length()) {
          mountPoint = mountPointOutputFromShell;
          types = mountPointFileSystemTypeFromShell;
          mountArgument = mountPointArgumentFromShell;
        }
      }
    }

    if (!mountPoint.equals("") && types != null && mountArgument != null) {
      // we have the mountpoint, check for mount options if already rw
      if (mountArgument.contains("rw")) {
        // already a rw filesystem return
        return null;
      } else if (mountArgument.contains("ro")) {
        // read-only file system, remount as rw
        String mountCommand = "mount -o rw,remount " + mountPoint;
        ArrayList<String> mountOutput = RootHelper.runShellCommandToList(mountCommand);

        if (mountOutput.size() != 0) {
          // command failed, and we got a reason echo'ed
          return null;
        } else return mountPoint;
      }
    }
    return null;
  }

  /**
   * Mount path for read-only access (ro)
   *
   * @param path the root of device/filesystem to be mounted as ro
   */
  private static void mountFileSystemRO(String path) throws ShellNotRunningException {
    String command = "umount -r \"" + path + "\"";
    RootHelper.runShellCommand(command);
  }

  /** Copies file using root */
  public static void copy(String source, String destination) throws ShellNotRunningException {
    // remounting destination as rw
    String mountPoint = mountFileSystemRW(destination);

    RootHelper.runShellCommand("cp -r \"" + source + "\" \"" + destination + "\"");

    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }
  }

  /**
   * Change permissions for a given file path - requires root
   *
   * @param filePath given file path
   * @param updatedPermissions octal notation for permissions
   * @param isDirectory is given path a directory or file
   */
  public static void changePermissions(
      String filePath,
      int updatedPermissions,
      boolean isDirectory,
      OnOperationPerform onOperationPerform)
      throws ShellNotRunningException {

    String mountPoint = mountFileSystemRW(filePath);

    String options = isDirectory ? "-R" : "";
    String command = String.format(CHMOD_COMMAND, options, updatedPermissions, filePath);

    RootHelper.runShellCommandWithCallback(
        command,
        (commandCode, exitCode, output) -> {
          if (exitCode < 0) {
            onOperationPerform.callback(false);
          } else {
            onOperationPerform.callback(true);
          }
        });

    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }
  }

  /**
   * Creates an empty directory using root
   *
   * @param path path to new directory
   * @param name name of directory
   */
  public static void mkDir(String path, String name) throws ShellNotRunningException {

    String mountPoint = mountFileSystemRW(path);

    RootHelper.runShellCommand("mkdir \"" + path + "/" + name + "\"");
    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }
  }

  /**
   * Creates an empty file using root
   *
   * @param path path to new file
   */
  public static void mkFile(String path) throws ShellNotRunningException {
    String mountPoint = mountFileSystemRW(path);

    RootHelper.runShellCommand("touch \"" + path + "\"");
    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }
  }

  /** Returns file permissions in octal notation Method requires busybox */
  private static int getFilePermissions(String path) throws ShellNotRunningException {
    String line = RootHelper.runShellCommandToList("stat -c  %a \"" + path + "\"").get(0);

    return Integer.valueOf(line);
  }

  /**
   * Recursively removes a path with it's contents (if any)
   *
   * @return boolean whether file was deleted or not
   */
  public static boolean delete(String path) throws ShellNotRunningException {
    String mountPoint = mountFileSystemRW(path);
    ArrayList<String> result = RootHelper.runShellCommandToList("rm -rf \"" + path + "\"");

    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }

    return result.size() != 0;
  }

  /** Moves file using root */
  public static void move(String path, String destination) throws ShellNotRunningException {
    // remounting destination as rw
    String mountPoint = mountFileSystemRW(destination);

    // mountOwnerRW(mountPath);
    RootHelper.runShellCommand("mv \"" + path + "\" \"" + destination + "\"");

    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }
  }

  /**
   * Renames file using root
   *
   * @param oldPath path to file before rename
   * @param newPath path to file after rename
   * @return if rename was successful or not
   */
  public static boolean rename(String oldPath, String newPath) throws ShellNotRunningException {
    String mountPoint = mountFileSystemRW(oldPath);
    ArrayList<String> output =
        RootHelper.runShellCommandToList("mv \"" + oldPath + "\" \"" + newPath + "\"");

    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }

    return output.size() == 0;
  }

  public static void cat(String sourcePath, String destinationPath)
      throws ShellNotRunningException {

    String mountPoint = mountFileSystemRW(destinationPath);

    RootHelper.runShellCommand("cat \"" + sourcePath + "\" > \"" + destinationPath + "\"");
    if (mountPoint != null) {
      // we mounted the filesystem as rw, let's mount it back to ro
      mountFileSystemRO(mountPoint);
    }
  }

  /**
   * This converts from a set of booleans to OCTAL permissions notations. For use with {@link
   * RootUtils#CHMOD_COMMAND} (true, false, false, true, true, false, false, false, true) => 0461
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
    int u = ((ur ? CHMOD_READ : 0) | (uw ? CHMOD_WRITE : 0) | (ux ? CHMOD_EXECUTE : 0)) << 6;
    int g = ((gr ? CHMOD_READ : 0) | (gw ? CHMOD_WRITE : 0) | (gx ? CHMOD_EXECUTE : 0)) << 3;
    int o = (or ? CHMOD_READ : 0) | (ow ? CHMOD_WRITE : 0) | (ox ? CHMOD_EXECUTE : 0);
    return u | g | o;
  }
}
