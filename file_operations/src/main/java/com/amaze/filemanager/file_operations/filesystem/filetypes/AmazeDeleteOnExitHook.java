/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.amaze.filemanager.file_operations.filesystem.filetypes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * This class holds a set of filenames to be deleted on VM exit through a shutdown hook.
 * A set is used both to prevent double-insertion of the same file as well as offer
 * quick removal.
 */

public class AmazeDeleteOnExitHook {
  private static LinkedHashSet<String> files = new LinkedHashSet<>();
  static {
    // BEGIN Android-changed: Use Runtime.addShutdownHook() rather than SharedSecrets.
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        runHooks();
      }
    });
    // END Android-changed: Use Runtime.addShutdownHook() rather than SharedSecrets.
  }

  private AmazeDeleteOnExitHook() {}

  static synchronized void add(String file) {
    if(files == null) {
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