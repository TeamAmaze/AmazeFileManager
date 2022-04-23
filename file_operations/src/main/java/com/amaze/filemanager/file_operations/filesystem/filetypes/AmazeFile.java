/*
 * Copyright (C) 2014-2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.box.BoxAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.dropbox.DropboxAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.gdrive.GoogledriveAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.onedrive.OnedriveAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.FileAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.SmbAmazeFileSystem;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.Deprecated;
import kotlin.NotImplementedError;

// Android-added: Info about UTF-8 usage in filenames.
/**
 * An abstract representation of file and directory pathnames.
 *
 * <p>User interfaces and operating systems use system-dependent <em>pathname strings</em> to name
 * files and directories. This class presents an abstract, system-independent view of hierarchical
 * pathnames. An <em>abstract pathname</em> has two components:
 *
 * <ol>
 *   <li>An optional system-dependent <em>prefix</em> string, such as a disk-drive specifier, <code>
 *       "/"</code>&nbsp;for the UNIX root directory, or <code>"\\\\"</code>&nbsp;for a Microsoft
 *       Windows UNC pathname, and
 *   <li>A sequence of zero or more string <em>names</em>.
 * </ol>
 *
 * The first name in an abstract pathname may be a directory name or, in the case of Microsoft
 * Windows UNC pathnames, a hostname. Each subsequent name in an abstract pathname denotes a
 * directory; the last name may denote either a directory or a file. The <em>empty</em> abstract
 * pathname has no prefix and an empty name sequence.
 *
 * <p>The conversion of a pathname string to or from an abstract pathname is inherently
 * system-dependent. When an abstract pathname is converted into a pathname string, each name is
 * separated from the next by a single copy of the default <em>separator character</em>. The default
 * name-separator character is defined by the system property <code>file.separator</code>, and is
 * made available in the public static fields <code>{@link
 * #separator}</code> and <code>{@link #separatorChar}</code> of this class. When a pathname string
 * is converted into an abstract pathname, the names within it may be separated by the default
 * name-separator character or by any other name-separator character that is supported by the
 * underlying system.
 *
 * <p>A pathname, whether abstract or in string form, may be either <em>absolute</em> or
 * <em>relative</em>. An absolute pathname is complete in that no other information is required in
 * order to locate the file that it denotes. A relative pathname, in contrast, must be interpreted
 * in terms of information taken from some other pathname. By default the classes in the <code>
 * java.io</code> package always resolve relative pathnames against the current user directory. This
 * directory is named by the system property <code>user.dir</code>, and is typically the directory
 * in which the Java virtual machine was invoked.
 *
 * <p>The <em>parent</em> of an abstract pathname may be obtained by invoking the {@link #getParent}
 * method of this class and consists of the pathname's prefix and each name in the pathname's name
 * sequence except for the last. Each directory's absolute pathname is an ancestor of any
 * <tt>AmazeFile</tt> object with an absolute abstract pathname which begins with the directory's
 * absolute pathname. For example, the directory denoted by the abstract pathname <tt>"/usr"</tt> is
 * an ancestor of the directory denoted by the pathname <tt>"/usr/local/bin"</tt>.
 *
 * <p>The prefix concept is used to handle root directories on UNIX platforms, and drive specifiers,
 * root directories and UNC pathnames on Microsoft Windows platforms, as follows:
 *
 * <ul>
 *   <li>For UNIX platforms, the prefix of an absolute pathname is always <code>"/"</code>. Relative
 *       pathnames have no prefix. The abstract pathname denoting the root directory has the prefix
 *       <code>"/"</code> and an empty name sequence.
 *   <li>For Microsoft Windows platforms, the prefix of a pathname that contains a drive specifier
 *       consists of the drive letter followed by <code>":"</code> and possibly followed by <code>
 *       "\\"</code> if the pathname is absolute. The prefix of a UNC pathname is <code>"\\\\"
 *       </code>; the hostname and the share name are the first two names in the name sequence. A
 *       relative pathname that does not specify a drive has no prefix.
 * </ul>
 *
 * <p>Instances of this class may or may not denote an actual file-system object such as a file or a
 * directory. If it does denote such an object then that object resides in a <i>partition</i>. A
 * partition is an operating system-specific portion of storage for a file system. A single storage
 * device (e.g. a physical disk-drive, flash memory, CD-ROM) may contain multiple partitions. The
 * object, if any, will reside on the partition <a name="partName">named</a> by some ancestor of the
 * absolute form of this pathname.
 *
 * <p>A file system may implement restrictions to certain operations on the actual file-system
 * object, such as reading, writing, and executing. These restrictions are collectively known as
 * <i>access permissions</i>. The file system may have multiple sets of access permissions on a
 * single object. For example, one set may apply to the object's <i>owner</i>, and another may apply
 * to all other users. The access permissions on an object may cause some methods in this class to
 * fail.
 *
 * <p>On Android strings are converted to UTF-8 byte sequences when sending filenames to the
 * operating system, and byte sequences returned by the operating system (from the various {@code
 * list} methods) are converted to strings by decoding them as UTF-8 byte sequences.
 *
 * @author unascribed
 */
public class AmazeFile implements Parcelable, Comparable<AmazeFile> {

  public static final String TAG = AmazeFile.class.getSimpleName();

  private static ContextProvider contextProvider;

  /** The FileSystem object representing the platform's local file system. */
  private AmazeFileSystem fs;

  /**
   * This abstract pathname's normalized pathname string. A normalized pathname string uses the
   * default name-separator character and does not contain any duplicate or redundant separators.
   */
  @NonNull private final String path;

  /** The flag indicating whether the file path is invalid. */
  private transient PathStatus status = null;

  /** The length of this abstract pathname's prefix, or zero if it has no prefix. */
  private final transient int prefixLength;

  /**
   * The system-dependent default name-separator character. This field is initialized to contain the
   * first character of the value of the system property <code>file.separator</code>. On UNIX
   * systems the value of this field is <code>'/'</code>; on Microsoft Windows systems it is <code>
   * '\\'</code>.
   *
   * @see java.lang.System#getProperty(java.lang.String)
   */
  public final char separatorChar;

  /**
   * The system-dependent default name-separator character, represented as a string for convenience.
   * This string contains a single character, namely <code>{@link #separatorChar}</code>.
   */
  public final String separator;

  /**
   * The system-dependent path-separator character. This field is initialized to contain the first
   * character of the value of the system property <code>path.separator</code>. This character is
   * used to separate filenames in a sequence of files given as a <em>path list</em>. On UNIX
   * systems, this character is <code>':'</code>; on Microsoft Windows systems it is <code>';'
   * </code>.
   *
   * @see java.lang.System#getProperty(java.lang.String)
   */
  public final char pathSeparatorChar;

  /**
   * The system-dependent path-separator character, represented as a string for convenience. This
   * string contains a single character, namely <code>{@link #pathSeparatorChar}</code>.
   */
  public final String pathSeparator;

  /** Enum type that indicates the status of a file path. */
  private enum PathStatus {
    INVALID,
    CHECKED
  };

  /**
   * Check if the file has an invalid path. Currently, the inspection of a file path is very
   * limited, and it only covers Nul character check. Returning true means the path is definitely
   * invalid/garbage. But returning false does not guarantee that the path is valid.
   *
   * @return true if the file path is invalid.
   */
  final boolean isInvalid() {
    if (status == null) {
      status = (this.path.indexOf('\u0000') < 0) ? PathStatus.CHECKED : PathStatus.INVALID;
    }
    return status == PathStatus.INVALID;
  }

  /** Returns the length of this abstract pathname's prefix. For use by FileSystem classes. */
  int getPrefixLength() {
    return prefixLength;
  }

  /* -- Constructors -- */

  /** Internal constructor for already-normalized pathname strings. */
  private AmazeFile(@NonNull String pathname, int prefixLength) {
    loadFilesystem(pathname);
    separatorChar = fs.getSeparator();
    separator = "" + separatorChar;
    pathSeparatorChar = fs.getPathSeparator();
    pathSeparator = "" + pathSeparatorChar;
    this.path = pathname;
    this.prefixLength = prefixLength;
  }

  /**
   * Internal constructor for already-normalized pathname strings. The parameter order is used to
   * disambiguate this method from the public(AmazeFile, String) constructor.
   */
  private AmazeFile(@NonNull String child, @NonNull AmazeFile parent) {
    assert (!parent.path.equals(""));
    loadFilesystem(parent.path);
    separatorChar = fs.getSeparator();
    separator = "" + separatorChar;
    pathSeparatorChar = fs.getPathSeparator();
    pathSeparator = "" + pathSeparatorChar;
    this.path = fs.resolve(parent.path, child);
    this.prefixLength = parent.prefixLength;
  }

  /**
   * Creates a new <code>AmazeFile</code> instance by converting the given pathname string into an
   * abstract pathname. If the given string is the empty string, then the result is the empty
   * abstract pathname.
   *
   * @param pathname A pathname string
   */
  public AmazeFile(@NonNull String pathname) {
    loadFilesystem(pathname);
    separatorChar = fs.getSeparator();
    separator = "" + separatorChar;
    pathSeparatorChar = fs.getPathSeparator();
    pathSeparator = "" + pathSeparatorChar;
    this.path = fs.normalize(pathname);
    this.prefixLength = fs.prefixLength(this.path);
  }

  /* Note: The two-argument File constructors do not interpret an empty
  parent abstract pathname as the current user directory.  An empty parent
  instead causes the child to be resolved against the system-dependent
  directory defined by the FileSystem.getDefaultParent method.  On Unix
  this default is "/", while on Microsoft Windows it is "\\".  This is required for
  compatibility with the original behavior of this class. */

  /**
   * Creates a new <code>AmazeFile</code> instance from a parent pathname string and a child
   * pathname string.
   *
   * <p>If <code>parent</code> is <code>null</code> then the new <code>AmazeFile</code> instance is
   * created as if by invoking the single-argument <code>AmazeFile</code> constructor on the given
   * <code>child</code> pathname string.
   *
   * <p>Otherwise the <code>parent</code> pathname string is taken to denote a directory, and the
   * <code>child</code> pathname string is taken to denote either a directory or a file. If the
   * <code>child</code> pathname string is absolute then it is converted into a relative pathname in
   * a system-dependent way. If <code>parent</code> is the empty string then the new <code>AmazeFile
   * </code> instance is created by converting <code>child</code> into an abstract pathname and
   * resolving the result against a system-dependent default directory. Otherwise each pathname
   * string is converted into an abstract pathname and the child abstract pathname is resolved
   * against the parent.
   *
   * @param parent The parent pathname string
   * @param child The child pathname string
   */
  public AmazeFile(@Nullable String parent, @NonNull String child) {
    // BEGIN Android-changed: b/25859957, app-compat; don't substitute empty parent.
    if (parent != null && !parent.isEmpty()) {
      loadFilesystem(parent);
      separatorChar = fs.getSeparator();
      separator = "" + separatorChar;
      pathSeparatorChar = fs.getPathSeparator();
      pathSeparator = "" + pathSeparatorChar;
      this.path = fs.resolve(fs.normalize(parent), fs.normalize(child));
      // END Android-changed: b/25859957, app-compat; don't substitute empty parent.
    } else {
      loadFilesystem(child);
      separatorChar = fs.getSeparator();
      separator = "" + separatorChar;
      pathSeparatorChar = fs.getPathSeparator();
      pathSeparator = "" + pathSeparatorChar;
      this.path = fs.normalize(child);
    }
    this.prefixLength = fs.prefixLength(this.path);
  }

  /**
   * Creates a new <code>AmazeFile</code> instance from a parent abstract pathname and a child
   * pathname string.
   *
   * <p>If <code>parent</code> is <code>null</code> then the new <code>AmazeFile</code> instance is
   * created as if by invoking the single-argument <code>AmazeFile</code> constructor on the given
   * <code>child</code> pathname string.
   *
   * <p>Otherwise the <code>parent</code> abstract pathname is taken to denote a directory, and the
   * <code>child</code> pathname string is taken to denote either a directory or a file. If the
   * <code>child</code> pathname string is absolute then it is converted into a relative pathname in
   * a system-dependent way. If <code>parent</code> is the empty abstract pathname then the new
   * <code>AmazeFile</code> instance is created by converting <code>child</code> into an abstract
   * pathname and resolving the result against a system-dependent default directory. Otherwise each
   * pathname string is converted into an abstract pathname and the child abstract pathname is
   * resolved against the parent.
   *
   * @param parent The parent abstract pathname
   * @param child The child pathname string
   */
  public AmazeFile(@Nullable AmazeFile parent, @NonNull String child) {
    if (parent != null) {
      loadFilesystem(parent.getPath());
      separatorChar = fs.getSeparator();
      separator = "" + separatorChar;
      pathSeparatorChar = fs.getPathSeparator();
      pathSeparator = "" + pathSeparatorChar;

      if (parent.path.equals("")) {
        this.path = fs.resolve(fs.getDefaultParent(), fs.normalize(child));
      } else {
        this.path = fs.resolve(parent.path, fs.normalize(child));
      }
    } else {
      loadFilesystem(child);
      separatorChar = fs.getSeparator();
      separator = "" + separatorChar;
      pathSeparatorChar = fs.getPathSeparator();
      pathSeparator = "" + pathSeparatorChar;
      this.path = fs.normalize(child);
    }
    this.prefixLength = fs.prefixLength(this.path);
  }

  /**
   * Creates a new <tt>AmazeFile</tt> instance by converting the given <tt>file:</tt> URI into an
   * abstract pathname.
   *
   * <p>The exact form of a <tt>file:</tt> URI is system-dependent, hence the transformation
   * performed by this constructor is also system-dependent.
   *
   * <p>For a given abstract pathname <i>f</i> it is guaranteed that
   *
   * <blockquote>
   *
   * <tt> new AmazeFile(</tt><i>&nbsp;f</i><tt>.{@link #toURI()
   * toURI}()).equals(</tt><i>&nbsp;f</i><tt>.{@link #getAbsoluteFile() getAbsoluteFile}()) </tt>
   *
   * </blockquote>
   *
   * so long as the original abstract pathname, the URI, and the new abstract pathname are all
   * created in (possibly different invocations of) the same Java virtual machine. This relationship
   * typically does not hold, however, when a <tt>file:</tt> URI that is created in a virtual
   * machine on one operating system is converted into an abstract pathname in a virtual machine on
   * a different operating system.
   *
   * @param uri An absolute, hierarchical URI with a scheme equal to <tt>"file"</tt>, a non-empty
   *     path component, and undefined authority, query, and fragment components
   * @throws IllegalArgumentException If the preconditions on the parameter do not hold
   * @see #toURI()
   * @see java.net.URI
   */
  public AmazeFile(@NonNull URI uri) {
    // Check our many preconditions
    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException("URI is not absolute");
    }
    if (uri.isOpaque()) {
      throw new IllegalArgumentException("URI is not hierarchical");
    }
    String scheme = uri.getScheme();
    if ((scheme == null) || !scheme.equalsIgnoreCase("file")) {
      throw new IllegalArgumentException("URI scheme is not \"file\"");
    }
    if (uri.getAuthority() != null) {
      throw new IllegalArgumentException("URI has an authority component");
    }
    if (uri.getFragment() != null) {
      throw new IllegalArgumentException("URI has a fragment component");
    }
    if (uri.getQuery() != null) {
      throw new IllegalArgumentException("URI has a query component");
    }
    String p = uri.getPath();
    if (p.equals("")) {
      throw new IllegalArgumentException("URI path component is empty");
    }

    loadFilesystem(uri.toString());
    separatorChar = fs.getSeparator();
    separator = "" + separatorChar;
    pathSeparatorChar = fs.getPathSeparator();
    pathSeparator = "" + pathSeparatorChar;

    // Okay, now initialize
    p = fs.fromURIPath(p);
    if (File.separatorChar != '/') {
      p = p.replace('/', File.separatorChar);
    }
    this.path = fs.normalize(p);
    this.prefixLength = fs.prefixLength(this.path);
  }

  private void loadFilesystem(String path) {
    if (SmbAmazeFileSystem.INSTANCE.isPathOfThisFilesystem(path)) {
      fs = SmbAmazeFileSystem.INSTANCE;
    } else if (FileAmazeFilesystem.INSTANCE.isPathOfThisFilesystem(path)) {
      fs = FileAmazeFilesystem.INSTANCE;
    } else if (BoxAmazeFilesystem.INSTANCE.isPathOfThisFilesystem(path)) {
      fs = DropboxAmazeFilesystem.INSTANCE;
    } else if (DropboxAmazeFilesystem.INSTANCE.isPathOfThisFilesystem(path)) {
      fs = DropboxAmazeFilesystem.INSTANCE;
    } else if (GoogledriveAmazeFilesystem.INSTANCE.isPathOfThisFilesystem(path)) {
      fs = DropboxAmazeFilesystem.INSTANCE;
    } else if (OnedriveAmazeFilesystem.INSTANCE.isPathOfThisFilesystem(path)) {
      fs = DropboxAmazeFilesystem.INSTANCE;
    }
  }

  /* -- Path-component accessors -- */

  /**
   * Returns the name of the file or directory denoted by this abstract pathname. This is just the
   * last name in the pathname's name sequence. If the pathname's name sequence is empty, then the
   * empty string is returned.
   *
   * @return The name of the file or directory denoted by this abstract pathname, or the empty
   *     string if this pathname's name sequence is empty
   */
  @NonNull
  public String getName() {
    int index = path.lastIndexOf(separatorChar);
    if (index < prefixLength) return path.substring(prefixLength);
    return path.substring(index + 1);
  }

  /**
   * Returns the pathname string of this abstract pathname's parent, or <code>null</code> if this
   * pathname does not name a parent directory.
   *
   * <p>The <em>parent</em> of an abstract pathname consists of the pathname's prefix, if any, and
   * each name in the pathname's name sequence except for the last. If the name sequence is empty
   * then the pathname does not name a parent directory.
   *
   * @return The pathname string of the parent directory named by this abstract pathname, or <code>
   *     null</code> if this pathname does not name a parent
   */
  @Nullable
  public String getParent() {
    int index = path.lastIndexOf(separatorChar);
    if (index < prefixLength) {
      if ((prefixLength > 0) && (path.length() > prefixLength)) {
        return path.substring(0, prefixLength);
      }
      return null;
    }
    return path.substring(0, index);
  }

  /**
   * Returns the abstract pathname of this abstract pathname's parent, or <code>null</code> if this
   * pathname does not name a parent directory.
   *
   * <p>The <em>parent</em> of an abstract pathname consists of the pathname's prefix, if any, and
   * each name in the pathname's name sequence except for the last. If the name sequence is empty
   * then the pathname does not name a parent directory.
   *
   * @return The abstract pathname of the parent directory named by this abstract pathname, or
   *     <code>null</code> if this pathname does not name a parent
   */
  @Nullable
  public AmazeFile getParentFile() {
    String p = this.getParent();
    if (p == null) {
      return null;
    }
    return new AmazeFile(p, this.prefixLength);
  }

  /**
   * Converts this abstract pathname into a pathname string. The resulting string uses the {@link
   * #separator default name-separator character} to separate the names in the name sequence.
   *
   * @return The string form of this abstract pathname
   */
  @NonNull
  public String getPath() {
    return path;
  }

  /* -- Path operations -- */

  // Android-changed: Android-specific path information
  /**
   * Tests whether this abstract pathname is absolute. The definition of absolute pathname is system
   * dependent. On Android, absolute paths start with the character '/'.
   *
   * @return <code>true</code> if this abstract pathname is absolute, <code>false</code> otherwise
   */
  public boolean isAbsolute() {
    return fs.isAbsolute(this);
  }

  // Android-changed: Android-specific path information
  /**
   * Returns the absolute path of this file. An absolute path is a path that starts at a root of the
   * file system. On Android, there is only one root: {@code /}.
   *
   * <p>A common use for absolute paths is when passing paths to a {@code Process} as command-line
   * arguments, to remove the requirement implied by relative paths, that the child must have the
   * same working directory as its parent.
   *
   * @return The absolute pathname string denoting the same file or directory as this abstract
   *     pathname
   * @see java.io.File#isAbsolute()
   */
  @NonNull
  public String getAbsolutePath() {
    return fs.resolve(this);
  }

  /**
   * Returns the absolute form of this abstract pathname. Equivalent to <code>
   * new&nbsp;File(this.{@link #getAbsolutePath})</code>.
   *
   * @return The absolute abstract pathname denoting the same file or directory as this abstract
   *     pathname
   */
  @NonNull
  public AmazeFile getAbsoluteFile() {
    String absPath = getAbsolutePath();
    return new AmazeFile(absPath, fs.prefixLength(absPath));
  }

  /**
   * Returns the canonical pathname string of this abstract pathname.
   *
   * <p>A canonical pathname is both absolute and unique. The precise definition of canonical form
   * is system-dependent. This method first converts this pathname to absolute form if necessary, as
   * if by invoking the {@link #getAbsolutePath} method, and then maps it to its unique form in a
   * system-dependent way. This typically involves removing redundant names such as <tt>"."</tt> and
   * <tt>".."</tt> from the pathname, resolving symbolic links (on UNIX platforms), and converting
   * drive letters to a standard case (on Microsoft Windows platforms).
   *
   * <p>Every pathname that denotes an existing file or directory has a unique canonical form. Every
   * pathname that denotes a nonexistent file or directory also has a unique canonical form. The
   * canonical form of the pathname of a nonexistent file or directory may be different from the
   * canonical form of the same pathname after the file or directory is created. Similarly, the
   * canonical form of the pathname of an existing file or directory may be different from the
   * canonical form of the same pathname after the file or directory is deleted.
   *
   * @return The canonical pathname string denoting the same file or directory as this abstract
   *     pathname
   * @throws IOException If an I/O error occurs, which is possible because the construction of the
   *     canonical pathname may require filesystem queries
   * @see Path#toRealPath
   */
  @NonNull
  public String getCanonicalPath() throws IOException {
    if (isInvalid()) {
      throw new IOException("Invalid file path");
    }
    return fs.canonicalize(fs.resolve(this));
  }

  /**
   * Returns the canonical form of this abstract pathname. Equivalent to <code>
   * new&nbsp;File(this.{@link #getCanonicalPath})</code>.
   *
   * @return The canonical pathname string denoting the same file or directory as this abstract
   *     pathname
   * @throws IOException If an I/O error occurs, which is possible because the construction of the
   *     canonical pathname may require filesystem queries
   * @see Path#toRealPath
   */
  @NonNull
  public AmazeFile getCanonicalFile() throws IOException {
    String canonPath = getCanonicalPath();
    return new AmazeFile(canonPath, fs.prefixLength(canonPath));
  }

  @NonNull
  private static String slashify(String path, boolean isDirectory) {
    String p = path;
    if (File.separatorChar != '/') p = p.replace(File.separatorChar, '/');
    if (!p.startsWith("/")) p = "/" + p;
    if (!p.endsWith("/") && isDirectory) p = p + "/";
    return p;
  }

  /**
   * Constructs a <tt>file:</tt> URI that represents this abstract pathname.
   *
   * <p>The exact form of the URI is system-dependent. If it can be determined that the file denoted
   * by this abstract pathname is a directory, then the resulting URI will end with a slash.
   *
   * <p>For a given abstract pathname <i>f</i>, it is guaranteed that
   *
   * <blockquote>
   *
   * <tt> new {@link #AmazeFile(java.net.URI)
   * File}(</tt><i>&nbsp;f</i><tt>.toURI()).equals(</tt><i>&nbsp;f</i><tt>.{@link #getAbsoluteFile()
   * getAbsoluteFile}()) </tt>
   *
   * </blockquote>
   *
   * so long as the original abstract pathname, the URI, and the new abstract pathname are all
   * created in (possibly different invocations of) the same Java virtual machine. Due to the
   * system-dependent nature of abstract pathnames, however, this relationship typically does not
   * hold when a <tt>file:</tt> URI that is created in a virtual machine on one operating system is
   * converted into an abstract pathname in a virtual machine on a different operating system.
   *
   * <p>Note that when this abstract pathname represents a UNC pathname then all components of the
   * UNC (including the server name component) are encoded in the {@code URI} path. The authority
   * component is undefined, meaning that it is represented as {@code null}. The {@link Path} class
   * defines the {@link Path#toUri toUri} method to encode the server name in the authority
   * component of the resulting {@code URI}. The {@link #toPath toPath} method may be used to obtain
   * a {@code Path} representing this abstract pathname.
   *
   * @return An absolute, hierarchical URI with a scheme equal to <tt>"file"</tt>, a path
   *     representing this abstract pathname, and undefined authority, query, and fragment
   *     components
   * @see #AmazeFile(java.net.URI)
   * @see java.net.URI
   * @see java.net.URI#toURL()
   */
  @Deprecated(message = "Left for reference, do not use")
  @NonNull
  public URI toURI() {
    try {
      AmazeFile f = getAbsoluteFile();
      String sp = slashify(f.getPath(), f.isDirectory());
      if (sp.startsWith("//")) sp = "//" + sp;
      return new URI("file", null, sp, null);
    } catch (URISyntaxException x) {
      throw new RuntimeException(x); // Can't happen
    }
  }

  /* -- Attribute accessors -- */

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on android
  /**
   * Tests whether the application can read the file denoted by this abstract pathname.
   *
   * @return <code>true</code> if and only if the file specified by this abstract pathname exists
   *     <em>and</em> can be read by the application; <code>false</code> otherwise
   */
  public boolean canRead() {
    if (isInvalid()) {
      return false;
    }
    return fs.checkAccess(this, AmazeFileSystem.ACCESS_READ);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on android
  /**
   * Tests whether the application can modify the file denoted by this abstract pathname.
   *
   * @return <code>true</code> if and only if the file system actually contains a file denoted by
   *     this abstract pathname <em>and</em> the application is allowed to write to the file; <code>
   *     false</code> otherwise.
   */
  public boolean canWrite() {
    if (isInvalid()) {
      return false;
    }
    return fs.checkAccess(this, AmazeFileSystem.ACCESS_WRITE);
  }

  /**
   * Tests whether the file or directory denoted by this abstract pathname exists.
   *
   * @return <code>true</code> if and only if the file or directory denoted by this abstract
   *     pathname exists; <code>false</code> otherwise
   */
  public boolean exists() {
    if (isInvalid()) {
      return false;
    }

    // Android-changed: b/25878034 work around SELinux stat64 denial.
    return fs.checkAccess(this, AmazeFileSystem.ACCESS_CHECK_EXISTS);
  }

  /**
   * Tests whether the file denoted by this abstract pathname is a directory.
   *
   * <p>Where it is required to distinguish an I/O exception from the case that the file is not a
   * directory, or where several attributes of the same file are required at the same time, then the
   * {@link java.nio.file.Files#readAttributes(Path,Class,LinkOption[]) Files.readAttributes} method
   * may be used.
   *
   * @return <code>true</code> if and only if the file denoted by this abstract pathname exists
   *     <em>and</em> is a directory; <code>false</code> otherwise
   */
  public boolean isDirectory() {
    if (isInvalid()) {
      return false;
    }
    return ((fs.getBooleanAttributes(this) & AmazeFileSystem.BA_DIRECTORY) != 0);
  }

  /**
   * Tests whether the file denoted by this abstract pathname is a normal file. A file is
   * <em>normal</em> if it is not a directory and, in addition, satisfies other system-dependent
   * criteria. Any non-directory file created by a Java application is guaranteed to be a normal
   * file.
   *
   * <p>Where it is required to distinguish an I/O exception from the case that the file is not a
   * normal file, or where several attributes of the same file are required at the same time, then
   * the {@link java.nio.file.Files#readAttributes(Path,Class,LinkOption[]) Files.readAttributes}
   * method may be used.
   *
   * @return <code>true</code> if and only if the file denoted by this abstract pathname exists
   *     <em>and</em> is a normal file; <code>false</code> otherwise
   */
  public boolean isFile() {
    if (isInvalid()) {
      return false;
    }
    return ((fs.getBooleanAttributes(this) & AmazeFileSystem.BA_REGULAR) != 0);
  }

  /**
   * Tests whether the file named by this abstract pathname is a hidden file. The exact definition
   * of <em>hidden</em> is system-dependent. On UNIX systems, a file is considered to be hidden if
   * its name begins with a period character (<code>'.'</code>). On Microsoft Windows systems, a
   * file is considered to be hidden if it has been marked as such in the filesystem.
   *
   * @return <code>true</code> if and only if the file denoted by this abstract pathname is hidden
   *     according to the conventions of the underlying platform
   */
  public boolean isHidden() {
    if (isInvalid()) {
      return false;
    }
    return ((fs.getBooleanAttributes(this) & AmazeFileSystem.BA_HIDDEN) != 0);
  }

  /**
   * Returns the time that the file denoted by this abstract pathname was last modified.
   *
   * <p>Where it is required to distinguish an I/O exception from the case where {@code 0L} is
   * returned, or where several attributes of the same file are required at the same time, or where
   * the time of last access or the creation time are required, then the {@link
   * java.nio.file.Files#readAttributes(Path,Class,LinkOption[]) Files.readAttributes} method may be
   * used.
   *
   * @return A <code>long</code> value representing the time the file was last modified, measured in
   *     milliseconds since the epoch (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
   *     file does not exist or if an I/O error occurs
   */
  public long lastModified() {
    if (isInvalid()) {
      return 0L;
    }
    return fs.getLastModifiedTime(this);
  }

  /**
   * Returns the length of the file denoted by this abstract pathname. The return value is
   * unspecified if this pathname denotes a directory.
   *
   * <p>Where it is required to distinguish an I/O exception from the case that {@code 0L} is
   * returned, or where several attributes of the same file are required at the same time, then the
   * {@link java.nio.file.Files#readAttributes(Path,Class,LinkOption[]) Files.readAttributes} method
   * may be used.
   *
   * @return The length, in bytes, of the file denoted by this abstract pathname, or <code>0L</code>
   *     if the file does not exist. Some operating systems may return <code>0L</code> for pathnames
   *     denoting system-dependent entities such as devices or pipes.
   */
  public long length() throws IOException {
    if (isInvalid()) {
      return 0L;
    }
    return fs.getLength(this);
  }

  /* -- File operations -- */

  /**
   * Atomically creates a new, empty file named by this abstract pathname if and only if a file with
   * this name does not yet exist. The check for the existence of the file and the creation of the
   * file if it does not exist are a single operation that is atomic with respect to all other
   * filesystem activities that might affect the file.
   *
   * <p>Note: this method should <i>not</i> be used for file-locking, as the resulting protocol
   * cannot be made to work reliably. The {@link java.nio.channels.FileLock FileLock} facility
   * should be used instead.
   *
   * @return <code>true</code> if the named file does not exist and was successfully created; <code>
   *     false</code> if the named file already exists
   * @throws IOException If an I/O error occurred
   */
  public boolean createNewFile() throws IOException {
    if (isInvalid()) {
      throw new IOException("Invalid file path");
    }
    return fs.createFileExclusively(path);
  }

  /**
   * Deletes the file or directory denoted by this abstract pathname.
   *
   * <p>Note that the {@link java.nio.file.Files} class defines the {@link
   * java.nio.file.Files#delete(Path) delete} method to throw an {@link IOException} when a file
   * cannot be deleted. This is useful for error reporting and to diagnose why a file cannot be
   * deleted.
   *
   * @return <code>true</code> if and only if the file or directory is successfully deleted; <code>
   *     false</code> otherwise
   */
  public boolean delete() {
    if (isInvalid()) {
      return false;
    }
    return fs.delete(this, contextProvider);
  }

  // Android-added: Additional information about Android behaviour.
  /**
   * Requests that the file or directory denoted by this abstract pathname be deleted when the
   * virtual machine terminates. Files (or directories) are deleted in the reverse order that they
   * are registered. Invoking this method to delete a file or directory that is already registered
   * for deletion has no effect. Deletion will be attempted only for normal termination of the
   * virtual machine, as defined by the Java Language Specification.
   *
   * <p>Once deletion has been requested, it is not possible to cancel the request. This method
   * should therefore be used with care.
   *
   * <p>Note: this method should <i>not</i> be used for file-locking, as the resulting protocol
   * cannot be made to work reliably. The {@link java.nio.channels.FileLock FileLock} facility
   * should be used instead.
   *
   * <p><i>Note that on Android, the application lifecycle does not include VM termination, so
   * calling this method will not ensure that files are deleted</i>. Instead, you should use the
   * most appropriate out of:
   *
   * <ul>
   *   <li>Use a {@code finally} clause to manually invoke {@link #delete}.
   *   <li>Maintain your own set of files to delete, and process it at an appropriate point in your
   *       application's lifecycle.
   *   <li>Use the Unix trick of deleting the file as soon as all readers and writers have opened
   *       it. No new readers/writers will be able to access the file, but all existing ones will
   *       still have access until the last one closes the file.
   * </ul>
   *
   * @see #delete
   */
  public void deleteOnExit() {
    if (isInvalid()) {
      return;
    }
    AmazeDeleteOnExitHook.add(path);
  }

  /**
   * Returns an array of strings naming the files and directories in the directory denoted by this
   * abstract pathname.
   *
   * <p>If this abstract pathname does not denote a directory, then this method returns {@code
   * null}. Otherwise an array of strings is returned, one for each file or directory in the
   * directory. Names denoting the directory itself and the directory's parent directory are not
   * included in the result. Each string is a file name rather than a complete path.
   *
   * <p>There is no guarantee that the name strings in the resulting array will appear in any
   * specific order; they are not, in particular, guaranteed to appear in alphabetical order.
   *
   * <p>Note that the {@link java.nio.file.Files} class defines the {@link
   * java.nio.file.Files#newDirectoryStream(Path) newDirectoryStream} method to open a directory and
   * iterate over the names of the files in the directory. This may use less resources when working
   * with very large directories, and may be more responsive when working with remote directories.
   *
   * @return An array of strings naming the files and directories in the directory denoted by this
   *     abstract pathname. The array will be empty if the directory is empty. Returns {@code null}
   *     if this abstract pathname does not denote a directory, or if an I/O error occurs.
   */
  @Nullable
  public String[] list() {
    if (isInvalid()) {
      return null;
    }
    return fs.list(this);
  }

  /**
   * Returns an array of strings naming the files and directories in the directory denoted by this
   * abstract pathname that satisfy the specified filter. The behavior of this method is the same as
   * that of the {@link #list()} method, except that the strings in the returned array must satisfy
   * the filter. If the given {@code filter} is {@code null} then all names are accepted. Otherwise,
   * a name satisfies the filter if and only if the value {@code true} results when the {@link
   * AmazeFilenameFilter#accept AmazeFilenameFilter.accept(File,&nbsp;String)} method of the filter
   * is invoked on this abstract pathname and the name of a file or directory in the directory that
   * it denotes.
   *
   * @param filter A filename filter
   * @return An array of strings naming the files and directories in the directory denoted by this
   *     abstract pathname that were accepted by the given {@code filter}. The array will be empty
   *     if the directory is empty or if no names were accepted by the filter. Returns {@code null}
   *     if this abstract pathname does not denote a directory, or if an I/O error occurs.
   * @see java.nio.file.Files#newDirectoryStream(Path,String)
   */
  @Nullable
  public String[] list(AmazeFilenameFilter filter) {
    String names[] = list();
    if ((names == null) || (filter == null)) {
      return names;
    }
    List<String> v = new ArrayList<>();
    for (int i = 0; i < names.length; i++) {
      if (filter.accept(this, names[i])) {
        v.add(names[i]);
      }
    }
    return v.toArray(new String[v.size()]);
  }

  /**
   * Returns an array of abstract pathnames denoting the files in the directory denoted by this
   * abstract pathname.
   *
   * <p>If this abstract pathname does not denote a directory, then this method returns {@code
   * null}. Otherwise an array of {@code File} objects is returned, one for each file or directory
   * in the directory. Pathnames denoting the directory itself and the directory's parent directory
   * are not included in the result. Each resulting abstract pathname is constructed from this
   * abstract pathname using the {@link #File(File, String) File(File,&nbsp;String)} constructor.
   * Therefore if this pathname is absolute then each resulting pathname is absolute; if this
   * pathname is relative then each resulting pathname will be relative to the same directory.
   *
   * <p>There is no guarantee that the name strings in the resulting array will appear in any
   * specific order; they are not, in particular, guaranteed to appear in alphabetical order.
   *
   * <p>Note that the {@link java.nio.file.Files} class defines the {@link
   * java.nio.file.Files#newDirectoryStream(Path) newDirectoryStream} method to open a directory and
   * iterate over the names of the files in the directory. This may use less resources when working
   * with very large directories.
   *
   * @return An array of abstract pathnames denoting the files and directories in the directory
   *     denoted by this abstract pathname. The array will be empty if the directory is empty.
   *     Returns {@code null} if this abstract pathname does not denote a directory, or if an I/O
   *     error occurs.
   */
  @Nullable
  public AmazeFile[] listFiles() {
    String[] ss = list();
    if (ss == null) return null;
    int n = ss.length;
    AmazeFile[] fs = new AmazeFile[n];
    for (int i = 0; i < n; i++) {
      fs[i] = new AmazeFile(ss[i], this);
    }
    return fs;
  }

  /**
   * Returns an array of abstract pathnames denoting the files and directories in the directory
   * denoted by this abstract pathname that satisfy the specified filter. The behavior of this
   * method is the same as that of the {@link #listFiles()} method, except that the pathnames in the
   * returned array must satisfy the filter. If the given {@code filter} is {@code null} then all
   * pathnames are accepted. Otherwise, a pathname satisfies the filter if and only if the value
   * {@code true} results when the {@link AmazeFilenameFilter#accept
   * AmazeFilenameFilter.accept(File,&nbsp;String)} method of the filter is invoked on this abstract
   * pathname and the name of a file or directory in the directory that it denotes.
   *
   * @param filter A filename filter
   * @return An array of abstract pathnames denoting the files and directories in the directory
   *     denoted by this abstract pathname. The array will be empty if the directory is empty.
   *     Returns {@code null} if this abstract pathname does not denote a directory, or if an I/O
   *     error occurs.
   * @see java.nio.file.Files#newDirectoryStream(Path,String)
   */
  @Nullable
  public AmazeFile[] listFiles(AmazeFilenameFilter filter) {
    String ss[] = list();
    if (ss == null) return null;
    ArrayList<AmazeFile> files = new ArrayList<>();
    for (String s : ss)
      if ((filter == null) || filter.accept(this, s)) files.add(new AmazeFile(s, this));
    return files.toArray(new AmazeFile[files.size()]);
  }

  /**
   * Returns an array of abstract pathnames denoting the files and directories in the directory
   * denoted by this abstract pathname that satisfy the specified filter. The behavior of this
   * method is the same as that of the {@link #listFiles()} method, except that the pathnames in the
   * returned array must satisfy the filter. If the given {@code filter} is {@code null} then all
   * pathnames are accepted. Otherwise, a pathname satisfies the filter if and only if the value
   * {@code true} results when the {@link FileFilter#accept FileFilter.accept(File)} method of the
   * filter is invoked on the pathname.
   *
   * @param filter A file filter
   * @return An array of abstract pathnames denoting the files and directories in the directory
   *     denoted by this abstract pathname. The array will be empty if the directory is empty.
   *     Returns {@code null} if this abstract pathname does not denote a directory, or if an I/O
   *     error occurs.
   * @see java.nio.file.Files#newDirectoryStream(Path,java.nio.file.DirectoryStream.Filter)
   */
  @Nullable
  public AmazeFile[] listFiles(AmazeFileFilter filter) {
    String ss[] = list();
    if (ss == null) return null;
    ArrayList<AmazeFile> files = new ArrayList<>();
    for (String s : ss) {
      AmazeFile f = new AmazeFile(s, this);
      if ((filter == null) || filter.accept(f)) files.add(f);
    }
    return files.toArray(new AmazeFile[files.size()]);
  }

  @NonNull
  public InputStream getInputStream() {
    return fs.getInputStream(this);
  }

  @NonNull
  public OutputStream getOutputStream() {
    return fs.getOutputStream(this, contextProvider);
  }

  /**
   * Creates the directory named by this abstract pathname.
   *
   * @return <code>true</code> if and only if the directory was created; <code>false</code>
   *     otherwise
   */
  public boolean mkdir() {
    if (isInvalid()) {
      return false;
    }
    return fs.createDirectory(this, contextProvider);
  }

  /**
   * Creates the directory named by this abstract pathname, including any necessary but nonexistent
   * parent directories. Note that if this operation fails it may have succeeded in creating some of
   * the necessary parent directories.
   *
   * @return <code>true</code> if and only if the directory was created, along with all necessary
   *     parent directories; <code>false</code> otherwise
   */
  public boolean mkdirs() {
    if (exists()) {
      return false;
    }
    if (mkdir()) {
      return true;
    }
    AmazeFile canonFile = null;
    try {
      canonFile = getCanonicalFile();
    } catch (IOException e) {
      return false;
    }

    AmazeFile parent = canonFile.getParentFile();
    return (parent != null && (parent.mkdirs() || parent.exists()) && canonFile.mkdir());
  }

  // Android-changed: Replaced generic platform info with Android specific one.
  /**
   * Renames the file denoted by this abstract pathname.
   *
   * <p>Many failures are possible. Some of the more likely failures include:
   *
   * <ul>
   *   <li>Write permission is required on the directories containing both the source and
   *       destination paths.
   *   <li>Search permission is required for all parents of both paths.
   *   <li>Both paths be on the same mount point. On Android, applications are most likely to hit
   *       this restriction when attempting to copy between internal storage and an SD card.
   * </ul>
   *
   * <p>The return value should always be checked to make sure that the rename operation was
   * successful.
   *
   * <p>Note that the {@link java.nio.file.Files} class defines the {@link java.nio.file.Files#move
   * move} method to move or rename a file in a platform independent manner.
   *
   * @param dest The new abstract pathname for the named file
   * @return <code>true</code> if and only if the renaming succeeded; <code>false</code> otherwise
   */
  public boolean renameTo(@NonNull AmazeFile dest) {
    if (this.isInvalid() || dest.isInvalid()) {
      return false;
    }
    return fs.rename(this, dest);
  }

  /**
   * Sets the last-modified time of the file or directory named by this abstract pathname.
   *
   * <p>All platforms support file-modification times to the nearest second, but some provide more
   * precision. The argument will be truncated to fit the supported precision. If the operation
   * succeeds and no intervening operations on the file take place, then the next invocation of the
   * <code>{@link #lastModified}</code> method will return the (possibly truncated) <code>time
   * </code> argument that was passed to this method.
   *
   * @param time The new last-modified time, measured in milliseconds since the epoch (00:00:00 GMT,
   *     January 1, 1970)
   * @return <code>true</code> if and only if the operation succeeded; <code>false</code> otherwise
   * @throws IllegalArgumentException If the argument is negative
   */
  public boolean setLastModified(long time) {
    if (time < 0) throw new IllegalArgumentException("Negative time");
    if (isInvalid()) {
      return false;
    }
    return fs.setLastModifiedTime(this, time);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * Marks the file or directory named by this abstract pathname so that only read operations are
   * allowed. After invoking this method the file or directory will not change until it is either
   * deleted or marked to allow write access. Whether or not a read-only file or directory may be
   * deleted depends upon the underlying system.
   *
   * @return <code>true</code> if and only if the operation succeeded; <code>false</code> otherwise
   */
  public boolean setReadOnly() {
    if (isInvalid()) {
      return false;
    }
    return fs.setReadOnly(this);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * Sets the owner's or everybody's write permission for this abstract pathname.
   *
   * <p>The {@link java.nio.file.Files} class defines methods that operate on file attributes
   * including file permissions. This may be used when finer manipulation of file permissions is
   * required.
   *
   * @param writable If <code>true</code>, sets the access permission to allow write operations; if
   *     <code>false</code> to disallow write operations
   * @param ownerOnly If <code>true</code>, the write permission applies only to the owner's write
   *     permission; otherwise, it applies to everybody. If the underlying file system can not
   *     distinguish the owner's write permission from that of others, then the permission will
   *     apply to everybody, regardless of this value.
   * @return <code>true</code> if and only if the operation succeeded. The operation will fail if
   *     the user does not have permission to change the access permissions of this abstract
   *     pathname.
   */
  public boolean setWritable(boolean writable, boolean ownerOnly) {
    if (isInvalid()) {
      return false;
    }
    return fs.setPermission(this, AmazeFileSystem.ACCESS_WRITE, writable, ownerOnly);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * A convenience method to set the owner's write permission for this abstract pathname.
   *
   * <p>An invocation of this method of the form <tt>file.setWritable(arg)</tt> behaves in exactly
   * the same way as the invocation
   *
   * <pre>
   *     file.setWritable(arg, true) </pre>
   *
   * @param writable If <code>true</code>, sets the access permission to allow write operations; if
   *     <code>false</code> to disallow write operations
   * @return <code>true</code> if and only if the operation succeeded. The operation will fail if
   *     the user does not have permission to change the access permissions of this abstract
   *     pathname.
   */
  public boolean setWritable(boolean writable) {
    return setWritable(writable, true);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * Sets the owner's or everybody's read permission for this abstract pathname.
   *
   * <p>The {@link java.nio.file.Files} class defines methods that operate on file attributes
   * including file permissions. This may be used when finer manipulation of file permissions is
   * required.
   *
   * @param readable If <code>true</code>, sets the access permission to allow read operations; if
   *     <code>false</code> to disallow read operations
   * @param ownerOnly If <code>true</code>, the read permission applies only to the owner's read
   *     permission; otherwise, it applies to everybody. If the underlying file system can not
   *     distinguish the owner's read permission from that of others, then the permission will apply
   *     to everybody, regardless of this value.
   * @return <code>true</code> if and only if the operation succeeded. The operation will fail if
   *     the user does not have permission to change the access permissions of this abstract
   *     pathname. If <code>readable</code> is <code>false</code> and the underlying file system
   *     does not implement a read permission, then the operation will fail.
   */
  public boolean setReadable(boolean readable, boolean ownerOnly) {
    if (isInvalid()) {
      return false;
    }
    return fs.setPermission(this, AmazeFileSystem.ACCESS_READ, readable, ownerOnly);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * A convenience method to set the owner's read permission for this abstract pathname.
   *
   * <p>An invocation of this method of the form <tt>file.setReadable(arg)</tt> behaves in exactly
   * the same way as the invocation
   *
   * <pre>
   *     file.setReadable(arg, true) </pre>
   *
   * @param readable If <code>true</code>, sets the access permission to allow read operations; if
   *     <code>false</code> to disallow read operations
   * @return <code>true</code> if and only if the operation succeeded. The operation will fail if
   *     the user does not have permission to change the access permissions of this abstract
   *     pathname. If <code>readable</code> is <code>false</code> and the underlying file system
   *     does not implement a read permission, then the operation will fail.
   */
  public boolean setReadable(boolean readable) {
    return setReadable(readable, true);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * Sets the owner's or everybody's execute permission for this abstract pathname.
   *
   * <p>The {@link java.nio.file.Files} class defines methods that operate on file attributes
   * including file permissions. This may be used when finer manipulation of file permissions is
   * required.
   *
   * @param executable If <code>true</code>, sets the access permission to allow execute operations;
   *     if <code>false</code> to disallow execute operations
   * @param ownerOnly If <code>true</code>, the execute permission applies only to the owner's
   *     execute permission; otherwise, it applies to everybody. If the underlying file system can
   *     not distinguish the owner's execute permission from that of others, then the permission
   *     will apply to everybody, regardless of this value.
   * @return <code>true</code> if and only if the operation succeeded. The operation will fail if
   *     the user does not have permission to change the access permissions of this abstract
   *     pathname. If <code>executable</code> is <code>false</code> and the underlying file system
   *     does not implement an execute permission, then the operation will fail.
   */
  public boolean setExecutable(boolean executable, boolean ownerOnly) {
    if (isInvalid()) {
      return false;
    }
    return fs.setPermission(this, AmazeFileSystem.ACCESS_EXECUTE, executable, ownerOnly);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * A convenience method to set the owner's execute permission for this abstract pathname.
   *
   * <p>An invocation of this method of the form <tt>file.setExcutable(arg)</tt> behaves in exactly
   * the same way as the invocation
   *
   * <pre>
   *     file.setExecutable(arg, true) </pre>
   *
   * @param executable If <code>true</code>, sets the access permission to allow execute operations;
   *     if <code>false</code> to disallow execute operations
   * @return <code>true</code> if and only if the operation succeeded. The operation will fail if
   *     the user does not have permission to change the access permissions of this abstract
   *     pathname. If <code>executable</code> is <code>false</code> and the underlying file system
   *     does not implement an execute permission, then the operation will fail.
   */
  public boolean setExecutable(boolean executable) {
    return setExecutable(executable, true);
  }

  // Android-changed. Removed javadoc comment about special privileges
  // that doesn't make sense on Android.
  /**
   * Tests whether the application can execute the file denoted by this abstract pathname.
   *
   * @return <code>true</code> if and only if the abstract pathname exists <em>and</em> the
   *     application is allowed to execute the file
   */
  public boolean canExecute() {
    if (isInvalid()) {
      return false;
    }
    return fs.checkAccess(this, AmazeFileSystem.ACCESS_EXECUTE);
  }

  /* -- Filesystem interface -- */

  // Android-changed: Replaced generic platform info with Android specific one.
  /**
   * Returns the file system roots. On Android and other Unix systems, there is a single root,
   * {@code /}.
   */
  public AmazeFile[] listRoots() {
    return fs.listRoots();
  }

  /* -- Disk usage -- */

  /**
   * Returns the size of the partition <a href="#partName">named</a> by this abstract pathname.
   *
   * @return The size, in bytes, of the partition or <tt>0L</tt> if this abstract pathname does not
   *     name a partition If there is no way to determine, total space is -1
   */
  public long getTotalSpace() {
    if (isInvalid()) {
      return 0L;
    }
    try {
      return fs.getSpace(this, AmazeFileSystem.SPACE_TOTAL);
    } catch (NotImplementedError e) {
      Log.w(TAG, "Call to unimplemented fuction", e);
      return -1;
    }
  }

  /**
   * Returns the number of unallocated bytes in the partition <a href="#partName">named</a> by this
   * abstract path name.
   *
   * <p>The returned number of unallocated bytes is a hint, but not a guarantee, that it is possible
   * to use most or any of these bytes. The number of unallocated bytes is most likely to be
   * accurate immediately after this call. It is likely to be made inaccurate by any external I/O
   * operations including those made on the system outside of this virtual machine. This method
   * makes no guarantee that write operations to this file system will succeed.
   *
   * @return The number of unallocated bytes on the partition or <tt>0L</tt> if the abstract
   *     pathname does not name a partition. This value will be less than or equal to the total file
   *     system size returned by {@link #getTotalSpace}.
   */
  public long getFreeSpace() {
    if (isInvalid()) {
      return 0L;
    }
    return fs.getSpace(this, AmazeFileSystem.SPACE_FREE);
  }

  // Android-added: Replaced generic platform info with Android specific one.
  /**
   * Returns the number of bytes available to this virtual machine on the partition <a
   * href="#partName">named</a> by this abstract pathname. When possible, this method checks for
   * write permissions and other operating system restrictions and will therefore usually provide a
   * more accurate estimate of how much new data can actually be written than {@link #getFreeSpace}.
   *
   * <p>The returned number of available bytes is a hint, but not a guarantee, that it is possible
   * to use most or any of these bytes. The number of unallocated bytes is most likely to be
   * accurate immediately after this call. It is likely to be made inaccurate by any external I/O
   * operations including those made on the system outside of this virtual machine. This method
   * makes no guarantee that write operations to this file system will succeed.
   *
   * <p>On Android (and other Unix-based systems), this method returns the number of free bytes
   * available to non-root users, regardless of whether you're actually running as root, and
   * regardless of any quota or other restrictions that might apply to the user. (The {@code
   * getFreeSpace} method returns the number of bytes potentially available to root.)
   *
   * @return The number of available bytes on the partition or <tt>0L</tt> if the abstract pathname
   *     does not name a partition. On systems where this information is not available, this method
   *     will be equivalent to a call to {@link #getFreeSpace}. If there is no way to determine the
   *     current space left -1 is returned.
   */
  public long getUsableSpace() {
    if (isInvalid()) {
      return 0L;
    }
    try {
      return fs.getSpace(this, AmazeFileSystem.SPACE_USABLE);
    } catch (NotImplementedError e) {
      Log.w(TAG, "Call to unimplemented fuction", e);
      return -1;
    }
  }

  /* -- Temporary files -- */

  private static class TempDirectory {
    // file name generation
    private static final SecureRandom random = new SecureRandom();

    @NonNull
    static AmazeFile generateFile(String prefix, String suffix, AmazeFile dir) throws IOException {
      // Android-changed: Use Math.randomIntInternal. This (pseudo) random number
      // is initialized post-fork

      long n = random.nextLong();
      if (n == Long.MIN_VALUE) {
        n = 0; // corner case
      } else {
        n = Math.abs(n);
      }

      // Android-changed: Reject invalid file prefixes
      // Use only the file name from the supplied prefix
      // prefix = (new AmazeFile(prefix)).getName();

      String name = prefix + Long.toString(n) + suffix;
      AmazeFile f = new AmazeFile(dir, name);
      if (!name.equals(f.getName()) || f.isInvalid()) {
        if (System.getSecurityManager() != null)
          throw new IOException("Unable to create temporary file");
        else throw new IOException("Unable to create temporary file, " + f);
      }
      return f;
    }
  }

  /**
   * Creates a new empty file in the specified directory, using the given prefix and suffix strings
   * to generate its name. If this method returns successfully then it is guaranteed that:
   *
   * <ol>
   *   <li>The file denoted by the returned abstract pathname did not exist before this method was
   *       invoked, and
   *   <li>Neither this method nor any of its variants will return the same abstract pathname again
   *       in the current invocation of the virtual machine.
   * </ol>
   *
   * This method provides only part of a temporary-file facility. To arrange for a file created by
   * this method to be deleted automatically, use the <code>{@link #deleteOnExit}</code> method.
   *
   * <p>The <code>prefix</code> argument must be at least three characters long. It is recommended
   * that the prefix be a short, meaningful string such as <code>"hjb"</code> or <code>"mail"</code>
   * . The <code>suffix</code> argument may be <code>null</code>, in which case the suffix <code>
   * ".tmp"</code> will be used.
   *
   * <p>To create the new file, the prefix and the suffix may first be adjusted to fit the
   * limitations of the underlying platform. If the prefix is too long then it will be truncated,
   * but its first three characters will always be preserved. If the suffix is too long then it too
   * will be truncated, but if it begins with a period character (<code>'.'</code>) then the period
   * and the first three characters following it will always be preserved. Once these adjustments
   * have been made the name of the new file will be generated by concatenating the prefix, five or
   * more internally-generated characters, and the suffix.
   *
   * <p>If the <code>directory</code> argument is <code>null</code> then the system-dependent
   * default temporary-file directory will be used. The default temporary-file directory is
   * specified by the system property <code>java.io.tmpdir</code>. On UNIX systems the default value
   * of this property is typically <code>"/tmp"</code> or <code>"/var/tmp"</code>; on Microsoft
   * Windows systems it is typically <code>"C:\\WINNT\\TEMP"</code>. A different value may be given
   * to this system property when the Java virtual machine is invoked, but programmatic changes to
   * this property are not guaranteed to have any effect upon the temporary directory used by this
   * method.
   *
   * @param prefix The prefix string to be used in generating the file's name; must be at least
   *     three characters long
   * @param suffix The suffix string to be used in generating the file's name; may be <code>null
   *     </code>, in which case the suffix <code>".tmp"</code> will be used
   * @param directory The directory in which the file is to be created, or <code>null</code> if the
   *     default temporary-file directory is to be used
   * @return An abstract pathname denoting a newly-created empty file
   * @throws IllegalArgumentException If the <code>prefix</code> argument contains fewer than three
   *     characters
   * @throws IOException If a file could not be created
   */
  @NonNull
  public AmazeFile createTempFile(
      @NonNull String prefix, @Nullable String suffix, @Nullable AmazeFile directory)
      throws IOException {
    if (prefix.length() < 3) {
      throw new IllegalArgumentException("Prefix string too short");
    }

    // Android-changed: Handle java.io.tmpdir changes.
    AmazeFile tmpdir =
        (directory != null) ? directory : new AmazeFile(System.getProperty("java.io.tmpdir", "."));
    AmazeFile f;
    do {
      f = TempDirectory.generateFile(prefix, suffix != null ? suffix : ".tmp", tmpdir);
    } while ((fs.getBooleanAttributes(f) & AmazeFileSystem.BA_EXISTS) != 0);

    if (!fs.createFileExclusively(f.getPath()))
      throw new IOException("Unable to create temporary file");

    return f;
  }

  /**
   * Creates an empty file in the default temporary-file directory, using the given prefix and
   * suffix to generate its name. Invoking this method is equivalent to invoking <code>
   * {@link #createTempFile(java.lang.String,
   * java.lang.String, java.io.File)
   * createTempFile(prefix,&nbsp;suffix,&nbsp;null)}</code>.
   *
   * <p>The {@link
   * java.nio.file.Files#createTempFile(String,String,java.nio.file.attribute.FileAttribute[])
   * Files.createTempFile} method provides an alternative method to create an empty file in the
   * temporary-file directory. Files created by that method may have more restrictive access
   * permissions to files created by this method and so may be more suited to security-sensitive
   * applications.
   *
   * @param prefix The prefix string to be used in generating the file's name; must be at least
   *     three characters long
   * @param suffix The suffix string to be used in generating the file's name; may be <code>null
   *     </code>, in which case the suffix <code>".tmp"</code> will be used
   * @return An abstract pathname denoting a newly-created empty file
   * @throws IllegalArgumentException If the <code>prefix</code> argument contains fewer than three
   *     characters
   * @throws IOException If a file could not be created
   * @see java.nio.file.Files#createTempDirectory(String,FileAttribute[])
   */
  @NonNull
  public AmazeFile createTempFile(String prefix, String suffix) throws IOException {
    return createTempFile(prefix, suffix, null);
  }

  /* -- Basic infrastructure -- */

  /**
   * Compares two abstract pathnames lexicographically. The ordering defined by this method depends
   * upon the underlying system. On UNIX systems, alphabetic case is significant in comparing
   * pathnames; on Microsoft Windows systems it is not.
   *
   * @param pathname The abstract pathname to be compared to this abstract pathname
   * @return Zero if the argument is equal to this abstract pathname, a value less than zero if this
   *     abstract pathname is lexicographically less than the argument, or a value greater than zero
   *     if this abstract pathname is lexicographically greater than the argument
   */
  public int compareTo(AmazeFile pathname) {
    return fs.compare(this, pathname);
  }

  /**
   * Tests this abstract pathname for equality with the given object. Returns <code>true</code> if
   * and only if the argument is not <code>null</code> and is an abstract pathname that denotes the
   * same file or directory as this abstract pathname. Whether or not two abstract pathnames are
   * equal depends upon the underlying system. On UNIX systems, alphabetic case is significant in
   * comparing pathnames; on Microsoft Windows systems it is not.
   *
   * @param obj The object to be compared with this abstract pathname
   * @return <code>true</code> if and only if the objects are the same; <code>false</code> otherwise
   */
  public boolean equals(Object obj) {
    if (obj instanceof AmazeFile) {
      return compareTo((AmazeFile) obj) == 0;
    }
    return false;
  }

  /**
   * Computes a hash code for this abstract pathname. Because equality of abstract pathnames is
   * inherently system-dependent, so is the computation of their hash codes. On UNIX systems, the
   * hash code of an abstract pathname is equal to the exclusive <em>or</em> of the hash code of its
   * pathname string and the decimal value <code>1234321</code>. On Microsoft Windows systems, the
   * hash code is equal to the exclusive <em>or</em> of the hash code of its pathname string
   * converted to lower case and the decimal value <code>1234321</code>. Locale is not taken into
   * account on lowercasing the pathname string.
   *
   * @return A hash code for this abstract pathname
   */
  public int hashCode() {
    return fs.hashCode(this);
  }

  /**
   * Returns the pathname string of this abstract pathname. This is just the string returned by the
   * <code>{@link #getPath}</code> method.
   *
   * @return The string form of this abstract pathname
   */
  @NonNull
  public String toString() {
    return getPath();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(getPath());
  }

  public static final Parcelable.Creator<AmazeFile> CREATOR =
      new Parcelable.Creator<AmazeFile>() {
        public AmazeFile createFromParcel(Parcel in) {
          return new AmazeFile(Objects.requireNonNull(in.readString()));
        }

        public AmazeFile[] newArray(int size) {
          return new AmazeFile[size];
        }
      };
}
