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

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.files.FileUtils;

import androidx.documentfile.provider.DocumentFile;

import eu.chainfire.libsuperuser.Shell;

public class RootHelper {

  /**
   * Runs the command and stores output in a list. The listener is set on the handler thread {@link
   * MainActivity#handlerThread} thus any code run in callback must be thread safe. Command is run
   * from the root context (u:r:SuperSU0)
   *
   * @param cmd the command
   * @return a list of results. Null only if the command passed is a blocking call or no output is
   *     there for the command passed
   */
  public static ArrayList<String> runShellCommandToList(String cmd)
      throws ShellNotRunningException {
    final ArrayList<String> result = new ArrayList<>();
    // callback being called on a background handler thread
    runShellCommandWithCallback(cmd, (commandCode, exitCode, output) -> result.addAll(output));
    return result;
  }

  /**
   * Command is run from the root context (u:r:SuperSU0)
   *
   * @param cmd the command
   */
  public static void runShellCommand(String cmd) throws ShellNotRunningException {
    if (MainActivity.shellInteractive == null || !MainActivity.shellInteractive.isRunning())
      throw new ShellNotRunningException();
    MainActivity.shellInteractive.addCommand(cmd);
    MainActivity.shellInteractive.waitForIdle();
  }

  /**
   * Runs the command on an interactive shell. Provides a listener for the caller to interact. The
   * caller is executed on a worker background thread, hence any calls from the callback should be
   * thread safe. Command is run from superuser context (u:r:SuperSU0)
   *
   * @param cmd the command
   */
  public static void runShellCommandWithCallback(String cmd, Shell.OnCommandResultListener callback)
      throws ShellNotRunningException {
    if (MainActivity.shellInteractive == null || !MainActivity.shellInteractive.isRunning())
      throw new ShellNotRunningException();
    MainActivity.shellInteractive.addCommand(cmd, 0, callback);
    MainActivity.shellInteractive.waitForIdle();
  }

  /**
   * @param cmd the command
   * @return a list of results. Null only if the command passed is a blocking call or no output is
   *     there for the command passed
   * @deprecated Use {@link #runShellCommand(String)} instead which runs command on an interactive
   *     shell
   *     <p>Runs the command and stores output in a list. The listener is set on the caller thread,
   *     thus any code run in callback must be thread safe. Command is run from a third-party level
   *     context (u:r:init_shell0) Not callback supported as the shell is not interactive
   */
  public static List<String> runNonRootShellCommand(String cmd) {
    return Shell.SH.run(cmd);
  }

  public static String getCommandLineString(String input) {
    return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
  }

  private static final String UNIX_ESCAPE_EXPRESSION =
      "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";

  /**
   * Loads files in a path using basic filesystem callbacks
   *
   * @param path the path
   */
  public static ArrayList<HybridFileParcelable> getFilesList(
      String path, boolean showHidden, OnFileFound listener) {
    File f = new File(path);
    ArrayList<HybridFileParcelable> files = new ArrayList<>();
    try {
      if (f.exists() && f.isDirectory()) {
        for (File x : f.listFiles()) {
          long size = 0;
          if (!x.isDirectory()) size = x.length();
          HybridFileParcelable baseFile =
              new HybridFileParcelable(
                  x.getPath(), parseFilePermission(x), x.lastModified(), size, x.isDirectory());
          baseFile.setName(x.getName());
          baseFile.setMode(OpenMode.FILE);
          if (showHidden) {
            files.add(baseFile);
            listener.onFileFound(baseFile);
          } else {
            if (!x.isHidden()) {
              files.add(baseFile);
              listener.onFileFound(baseFile);
            }
          }
        }
      }
    } catch (Exception e) {
    }
    return files;
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

  public static HybridFileParcelable generateBaseFile(DocumentFile file) {
    long size = 0;
    if (!file.isDirectory()) size = file.length();
    HybridFileParcelable baseFile =
        new HybridFileParcelable(
            file.getName(),
            parseDocumentFilePermission(file),
            file.lastModified(),
            size,
            file.isDirectory());
    baseFile.setName(file.getName());
    baseFile.setMode(OpenMode.OTG);

    return baseFile;
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
      ArrayList<HybridFileParcelable> ls = getFilesList(p, true, true, null);
      for (HybridFileParcelable strings : ls) {
        if (strings.getPath() != null && strings.getPath().equals(path)) {
          return true;
        }
      }
    }
    return false;
  }

  static boolean contains(String[] a, String name) {
    for (String s : a) {
      // Log.e("checking",s);
      if (s.equals(name)) return true;
    }
    return false;
  }

  /** Whether toTest file is directory or not TODO: Avoid parsing ls */
  public static boolean isDirectory(String toTest, boolean root, int count)
      throws ShellNotRunningException {
    File f = new File(toTest);
    String name = f.getName();
    String p = f.getParent();
    if (p != null && p.length() > 0) {
      ArrayList<String> ls = runShellCommandToList("ls -l " + p);
      for (String s : ls) {
        if (contains(s.split(" "), name)) {
          try {
            HybridFileParcelable path = FileUtils.parseName(s);
            if (path.getPermission().trim().startsWith("d")) return true;
            else if (path.getPermission().trim().startsWith("l")) {
              if (count > 5) return f.isDirectory();
              else return isDirectory(path.getLink().trim(), root, ++count);
            } else return f.isDirectory();
          } catch (Exception e) {
            e.printStackTrace();
          }
          break;
        }
      }
    }
    return f.isDirectory();
  }

  private static boolean isDirectory(HybridFileParcelable path) {
    return path.getPermission().startsWith("d") || new File(path.getPath()).isDirectory();
  }

  /** Callback to setting type of file to handle, while loading list of files */
  public interface GetModeCallBack {
    void getMode(OpenMode mode);
  }

  /**
   * Get a list of files using shell, supposing the path is not a SMB/OTG/Custom (*.apk/images)
   * TODO: Avoid parsing ls
   *
   * @param root whether root is available or not
   * @param showHidden to show hidden files
   * @param getModeCallBack callback to set the type of file
   * @deprecated use getFiles()
   */
  public static ArrayList<HybridFileParcelable> getFilesList(
      String path, boolean root, boolean showHidden, GetModeCallBack getModeCallBack) {
    final ArrayList<HybridFileParcelable> files = new ArrayList<>();
    getFiles(path, root, showHidden, getModeCallBack, files::add);
    return files;
  }

  /**
   * Get files using shell, supposing the path is not a SMB/OTG/Custom (*.apk/images) TODO: Avoid
   * parsing ls
   *
   * @param root whether root is available or not
   * @param showHidden to show hidden files
   * @param getModeCallBack callback to set the type of file
   */
  public static void getFiles(
      String path,
      boolean root,
      boolean showHidden,
      GetModeCallBack getModeCallBack,
      OnFileFound fileCallback) {
    OpenMode mode = OpenMode.FILE;
    if (root && !path.startsWith("/storage") && !path.startsWith("/sdcard")) {
      try {
        // we're rooted and we're trying to load file with superuser
        // we're at the root directories, superuser is required!
        List<String> ls;
        String cpath = getCommandLineString(path);
        // ls = Shell.SU.run("ls -l " + cpath);
        ls = runShellCommandToList("ls -l " + (showHidden ? "-a " : "") + "\"" + cpath + "\"");
        if (ls != null) {
          for (String file : ls) {
            if (!file.contains("Permission denied")) {
              HybridFileParcelable array = FileUtils.parseName(file);
              if (array != null) {
                array.setMode(OpenMode.ROOT);
                array.setName(array.getPath());
                if (!path.equals("/")) {
                  array.setPath(path + "/" + array.getPath());
                } else {
                  // root of filesystem, don't concat another '/'
                  array.setPath(path + array.getPath());
                }
                if (array.getLink().trim().length() > 0) {
                  boolean isdirectory = isDirectory(array.getLink(), root, 0);
                  array.setDirectory(isdirectory);
                } else array.setDirectory(isDirectory(array));
                fileCallback.onFileFound(array);
              }
            }
          }
          mode = OpenMode.ROOT;
        }

        if (getModeCallBack != null) getModeCallBack.getMode(mode);
      } catch (ShellNotRunningException e) {
        e.printStackTrace();
      }
    } else if (FileUtils.canListFiles(new File(path))) {
      // we're taking a chance to load files using basic java filesystem
      getFilesList(path, showHidden, fileCallback);
      mode = OpenMode.FILE;
    } else {
      // we couldn't load files using native java filesystem callbacks
      // maybe the access is not allowed due to android system restrictions, we'll see later
      mode = OpenMode.FILE;
    }

    if (getModeCallBack != null) getModeCallBack.getMode(mode);
  }
}
