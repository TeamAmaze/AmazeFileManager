/*
 * Copyright (C) 2014-2010 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.file_operations.filesystem.filetypes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * This class holds a set of filenames to be deleted on VM exit through a shutdown hook. A set is
 * used both to prevent double-insertion of the same file as well as offer quick removal.
 */
public class AmazeDeleteOnExitHook {
  private static LinkedHashSet<String> files = new LinkedHashSet<>();

  static {
    // BEGIN Android-changed: Use Runtime.addShutdownHook() rather than SharedSecrets.
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              public void run() {
                runHooks();
              }
            });
    // END Android-changed: Use Runtime.addShutdownHook() rather than SharedSecrets.
  }

  private AmazeDeleteOnExitHook() {}

  static synchronized void add(String file) {
    if (files == null) {
      // DeleteOnExitHook is running. Too late to add a file
      throw new IllegalStateException("Shutdown in progress");
    }

    files.add(file);
  }

  static void runHooks() {
    LinkedHashSet<String> theFiles;

    synchronized (AmazeDeleteOnExitHook.class) {
      theFiles = files;
      files = null;
    }

    ArrayList<String> toBeDeleted = new ArrayList<>(theFiles);

    // reverse the list to maintain previous jdk deletion order.
    // Last in first deleted.
    Collections.reverse(toBeDeleted);
    for (String filename : toBeDeleted) {
      (new File(filename)).delete();
    }
  }
}
