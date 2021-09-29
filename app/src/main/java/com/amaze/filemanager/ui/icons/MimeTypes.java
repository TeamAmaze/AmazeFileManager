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

package com.amaze.filemanager.ui.icons;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import com.amaze.filemanager.filesystem.files.CryptUtil;

import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

public final class MimeTypes {

  public static final String ALL_MIME_TYPES = "*/*";

  // construct a with an approximation of the capacity
  private static final HashMap<String, String> MIME_TYPES = new HashMap<>(1 + (int) (68 / 0.75));

  static {

    /*
     * ================= MIME TYPES ====================
     */
    MIME_TYPES.put("asm", "text/x-asm");
    MIME_TYPES.put("json", "application/json");
    MIME_TYPES.put("js", "application/javascript");

    MIME_TYPES.put("def", "text/plain");
    MIME_TYPES.put("in", "text/plain");
    MIME_TYPES.put("rc", "text/plain");
    MIME_TYPES.put("list", "text/plain");
    MIME_TYPES.put("log", "text/plain");
    MIME_TYPES.put("pl", "text/plain");
    MIME_TYPES.put("prop", "text/plain");
    MIME_TYPES.put("properties", "text/plain");
    MIME_TYPES.put("ini", "text/plain");
    MIME_TYPES.put("md", "text/markdown");

    MIME_TYPES.put("epub", "application/epub+zip");
    MIME_TYPES.put("ibooks", "application/x-ibooks+zip");

    MIME_TYPES.put("ifb", "text/calendar");
    MIME_TYPES.put("eml", "message/rfc822");
    MIME_TYPES.put("msg", "application/vnd.ms-outlook");

    MIME_TYPES.put("ace", "application/x-ace-compressed");
    MIME_TYPES.put("bz", "application/x-bzip");
    MIME_TYPES.put("bz2", "application/x-bzip2");
    MIME_TYPES.put("cab", "application/vnd.ms-cab-compressed");
    MIME_TYPES.put("gz", "application/x-gzip");
    MIME_TYPES.put("7z", "application/x-7z-compressed");
    MIME_TYPES.put("lrf", "application/octet-stream");
    MIME_TYPES.put("jar", "application/java-archive");
    MIME_TYPES.put("xz", "application/x-xz");
    MIME_TYPES.put("lzma", "application/x-lzma");
    MIME_TYPES.put("Z", "application/x-compress");

    MIME_TYPES.put("bat", "application/x-msdownload");
    MIME_TYPES.put("ksh", "text/plain");
    MIME_TYPES.put("sh", "application/x-sh");

    MIME_TYPES.put("db", "application/octet-stream");
    MIME_TYPES.put("db3", "application/octet-stream");

    MIME_TYPES.put("otf", "application/x-font-otf");
    MIME_TYPES.put("ttf", "application/x-font-ttf");
    MIME_TYPES.put("psf", "application/x-font-linux-psf");

    MIME_TYPES.put("cgm", "image/cgm");
    MIME_TYPES.put("btif", "image/prs.btif");
    MIME_TYPES.put("dwg", "image/vnd.dwg");
    MIME_TYPES.put("dxf", "image/vnd.dxf");
    MIME_TYPES.put("fbs", "image/vnd.fastbidsheet");
    MIME_TYPES.put("fpx", "image/vnd.fpx");
    MIME_TYPES.put("fst", "image/vnd.fst");
    MIME_TYPES.put("mdi", "image/vnd.ms-mdi");
    MIME_TYPES.put("npx", "image/vnd.net-fpx");
    MIME_TYPES.put("xif", "image/vnd.xiff");
    MIME_TYPES.put("pct", "image/x-pict");
    MIME_TYPES.put("pic", "image/x-pict");
    MIME_TYPES.put("gif", "image/gif");

    MIME_TYPES.put("adp", "audio/adpcm");
    MIME_TYPES.put("au", "audio/basic");
    MIME_TYPES.put("snd", "audio/basic");
    MIME_TYPES.put("m2a", "audio/mpeg");
    MIME_TYPES.put("m3a", "audio/mpeg");
    MIME_TYPES.put("oga", "audio/ogg");
    MIME_TYPES.put("spx", "audio/ogg");
    MIME_TYPES.put("aac", "audio/x-aac");
    MIME_TYPES.put("mka", "audio/x-matroska");
    MIME_TYPES.put("opus", "audio/ogg");

    MIME_TYPES.put("jpgv", "video/jpeg");
    MIME_TYPES.put("jpgm", "video/jpm");
    MIME_TYPES.put("jpm", "video/jpm");
    MIME_TYPES.put("mj2", "video/mj2");
    MIME_TYPES.put("mjp2", "video/mj2");
    MIME_TYPES.put("mpa", "video/mpeg");
    MIME_TYPES.put("ogv", "video/ogg");
    MIME_TYPES.put("flv", "video/x-flv");
    MIME_TYPES.put("mkv", "video/x-matroska");
    MIME_TYPES.put("mts", "video/mp2t");

    MIME_TYPES.put(CryptUtil.CRYPT_EXTENSION.replace(".", ""), "crypt/aze");
  }

  /**
   * Get Mime Type of a file
   *
   * @param path the file of which mime type to get
   * @return Mime type in form of String
   */
  public static String getMimeType(String path, boolean isDirectory) {
    if (isDirectory) {
      return null;
    }

    String type = ALL_MIME_TYPES;
    final String extension = getExtension(path);

    // mapping extension to system mime types
    if (extension != null && !extension.isEmpty()) {
      final String extensionLowerCase = extension.toLowerCase(Locale.getDefault());
      final MimeTypeMap mime = MimeTypeMap.getSingleton();
      type = mime.getMimeTypeFromExtension(extensionLowerCase);
      if (type == null) {
        type = MIME_TYPES.get(extensionLowerCase);
      }
    }
    if (type == null) type = ALL_MIME_TYPES;
    return type;
  }

  public static boolean mimeTypeMatch(String mime, String input) {
    return Pattern.matches(mime.replace("*", ".*"), input);
  }

  /**
   * Helper method for {@link #getMimeType(String, boolean)} to calculate the last '.' extension of
   * files
   *
   * @param path the path of file
   * @return extension extracted from name in lowercase
   */
  public static String getExtension(@Nullable String path) {
    if (path != null && path.contains("."))
      return path.substring(path.lastIndexOf(".") + 1).toLowerCase();
    else return "";
  }
}
