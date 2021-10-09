/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Native;

public abstract class AmazeFileSystem implements Closeable {

  /* -- Normalization and construction -- */

  /**
   * Is the path of this filesystem?
   */
  public abstract boolean isPathOfThisFilesystem(String path);

  /**
   * Return the local filesystem's name-separator character.
   */
  public abstract char getSeparator();

  /**
   * Return the local filesystem's path-separator character.
   */
  public abstract char getPathSeparator();

  /**
   * Convert the given pathname string to normal form.  If the string is
   * already in normal form then it is simply returned.
   */
  public abstract String normalize(String path);

  /**
   * Compute the length of this pathname string's prefix.  The pathname
   * string must be in normal form.
   */
  public abstract int prefixLength(String path);

  /**
   * Resolve the child pathname string against the parent.
   * Both strings must be in normal form, and the result
   * will be in normal form.
   */
  public abstract String resolve(String parent, String child);

  /**
   * Return the parent pathname string to be used when the parent-directory
   * argument in one of the two-argument File constructors is the empty
   * pathname.
   */
  public abstract String getDefaultParent();

  /**
   * Post-process the given URI path string if necessary.  This is used on
   * win32, e.g., to transform "/c:/foo" into "c:/foo".  The path string
   * still has slash separators; code in the File class will translate them
   * after this method returns.
   */
  public abstract String fromURIPath(String path);


  /* -- Path operations -- */

  /**
   * Tell whether or not the given abstract pathname is absolute.
   */
  public abstract boolean isAbsolute(AmazeFile f);

  /**
   * Resolve the given abstract pathname into absolute form.  Invoked by the
   * getAbsolutePath and getCanonicalPath methods in the F class.
   */
  public abstract String resolve(AmazeFile f);

  public abstract String canonicalize(String path) throws IOException;


  /* -- Attribute accessors -- */

  /* Constants for simple boolean attributes */
  @Native
  public static final int BA_EXISTS    = 0x01;
  @Native public static final int BA_REGULAR   = 0x02;
  @Native public static final int BA_DIRECTORY = 0x04;
  @Native public static final int BA_HIDDEN    = 0x08;

  /**
   * Return the simple boolean attributes for the file or directory denoted
   * by the given abstract pathname, or zero if it does not exist or some
   * other I/O error occurs.
   */
  public abstract int getBooleanAttributes(AmazeFile f);

  @Native public static final int ACCESS_READ    = 0x04;
  @Native public static final int ACCESS_WRITE   = 0x02;
  @Native public static final int ACCESS_EXECUTE = 0x01;
  // Android-added: b/25878034, to support F.exists() reimplementation.
  public static final int ACCESS_CHECK_EXISTS = 0x08;

  /**
   * Check whether the file or directory denoted by the given abstract
   * pathname may be accessed by this process.  The second argument specifies
   * which access, ACCESS_READ, ACCESS_WRITE or ACCESS_EXECUTE, to check.
   * Return false if access is denied or an I/O error occurs
   */
  public abstract boolean checkAccess(AmazeFile f, int access);
  /**
   * Set on or off the access permission (to owner only or to all) to the file
   * or directory denoted by the given abstract pathname, based on the parameters
   * enable, access and oweronly.
   */
  public abstract boolean setPermission(AmazeFile f, int access, boolean enable, boolean owneronly);

  /**
   * Return the time at which the file or directory denoted by the given
   * abstract pathname was last modified, or zero if it does not exist or
   * some other I/O error occurs.
   */
  public abstract long getLastModifiedTime(AmazeFile f);

  /**
   * Return the length in bytes of the file denoted by the given abstract
   * pathname, or zero if it does not exist, is a directory, or some other
   * I/O error occurs.
   */
  public abstract long getLength(AmazeFile f) throws IOException;


  /* -- File operations -- */

  /**
   * Create a new empty file with the given pathname.  Return
   * <code>true</code> if the file was created and <code>false</code> if a
   * file or directory with the given pathname already exists.  Throw an
   * IOException if an I/O error occurs.
   */
  public abstract boolean createFileExclusively(String pathname)
          throws IOException;

  /**
   * Delete the file or directory denoted by the given abstract pathname,
   * returning <code>true</code> if and only if the operation succeeds.
   */
  public abstract boolean delete(AmazeFile f);

  /**
   * List the elements of the directory denoted by the given abstract
   * pathname.  Return an array of strings naming the elements of the
   * directory if successful; otherwise, return <code>null</code>.
   */
  public abstract String[] list(AmazeFile f);

  public abstract InputStream getInputStream(AmazeFile f);

  public abstract OutputStream getOutputStream(AmazeFile f);

  /**
   * Create a new directory denoted by the given abstract pathname,
   * returning <code>true</code> if and only if the operation succeeds.
   */
  public abstract boolean createDirectory(AmazeFile f);

  /**
   * Rename the file or directory denoted by the first abstract pathname to
   * the second abstract pathname, returning <code>true</code> if and only if
   * the operation succeeds.
   */
  public abstract boolean rename(AmazeFile f1, AmazeFile f2);

  /**
   * Set the last-modified time of the file or directory denoted by the
   * given abstract pathname, returning <code>true</code> if and only if the
   * operation succeeds.
   */
  public abstract boolean setLastModifiedTime(AmazeFile f, long time);

  /**
   * Mark the file or directory denoted by the given abstract pathname as
   * read-only, returning <code>true</code> if and only if the operation
   * succeeds.
   */
  public abstract boolean setReadOnly(AmazeFile f);


  /* -- Filesystem interface -- */

  /**
   * List the available filesystem roots.
   */
  public abstract AmazeFile[] listRoots();

  /* -- Disk usage -- */
  @Native public static final int SPACE_TOTAL  = 0;
  @Native public static final int SPACE_FREE   = 1;
  @Native public static final int SPACE_USABLE = 2;

  public abstract long getSpace(AmazeFile f, int t);

  /* -- Basic infrastructure -- */

  /**
   * Compare two abstract pathnames lexicographically.
   */
  public abstract int compare(AmazeFile f1, AmazeFile f2);

  /**
   * Compute the hash code of an abstract pathname.
   */
  public abstract int hashCode(AmazeFile f);

  // Flags for enabling/disabling performance optimizations for file
  // name canonicalization
  // Android-changed: Disabled caches for security reasons (b/62301183)
  //static boolean useCanonCaches      = true;
  //static boolean useCanonPrefixCache = true;
  static boolean useCanonCaches      = false;
  static boolean useCanonPrefixCache = false;

  private static boolean getBooleanProperty(String prop, boolean defaultVal) {
    String val = System.getProperty(prop);
    if (val == null) return defaultVal;
    if (val.equalsIgnoreCase("true")) {
      return true;
    } else {
      return false;
    }
  }

  static {
    useCanonCaches      = getBooleanProperty("sun.io.useCanonCaches",
            useCanonCaches);
    useCanonPrefixCache = getBooleanProperty("sun.io.useCanonPrefixCache",
            useCanonPrefixCache);
  }

  /*
   * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
   *
   * A normal Unix pathname does not contain consecutive slashes and does not end
   * with a slash. The empty string and "/" are special cases that are also
   * considered normal.
   */
  public static String simpleUnixNormalize(String pathname) {
    int n = pathname.length();
    char[] normalized = pathname.toCharArray();
    int index = 0;
    char prevChar = 0;
    for (int i = 0; i < n; i++) {
      char current = normalized[i];
      // Remove duplicate slashes.
      if (!(current == '/' && prevChar == '/')) {
        normalized[index++] = current;
      }

      prevChar = current;
    }

    // Omit the trailing slash, except when pathname == "/".
    if (prevChar == '/' && n > 1) {
      index--;
    }

    return (index != n) ? new String(normalized, 0, index) : pathname;
  }

  /*
   * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
  // Invariant: Both |parent| and |child| are normalized paths.
  public static String basicUnixResolve(String parent, String child) {
    if (child.isEmpty() || child.equals("/")) {
      return parent;
    }

    if (child.charAt(0) == '/') {
      if (parent.equals("/")) return child;
      return parent + child;
    }

    if (parent.equals("/")) return parent + child;
    return parent + '/' + child;
  }

  public static int basicUnixHashCode(String path) {
    return path.hashCode() ^ 1234321;
  }

}
