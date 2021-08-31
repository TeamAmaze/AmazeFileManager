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

package com.amaze.filemanager.file_operations.filesystem.cloud;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import android.util.Log;

import jcifs.smb.SmbFile;

public class CloudStreamer extends CloudStreamServer {

  private static final String TAG = CloudStreamer.class.getSimpleName();

  public static final int PORT = 7871;
  public static final String URL = "http://127.0.0.1:" + PORT;
  private InputStream inputStream;
  private String fileName;
  long length = 0;
  private static CloudStreamer instance;
  private static Pattern pattern =
      Pattern.compile(
          "^.*\\.(?i)(mp3|wma|wav|aac|ogg|m4a|flac|mp4|avi|mpg|mpeg|3gp|3gpp|mkv|flv|rmvb)$");

  // private CBItem source;
  // private String mime;

  protected CloudStreamer(int port) throws IOException {
    super(port, new File("."));
  }

  public static CloudStreamer getInstance() {
    if (instance == null)
      try {
        instance = new CloudStreamer(PORT);
      } catch (IOException e) {
        Log.e(TAG, "Error initializing CloudStreamer", e);
      }
    return instance;
  }

  public static boolean isStreamMedia(SmbFile file) {
    return pattern.matcher(file.getName()).matches();
  }

  public void setStreamSrc(InputStream inputStream, String fileName, long length) {
    this.inputStream = inputStream;
    this.fileName = fileName;
    this.length = length;
  }

  @Override
  public void stop() {
    super.stop();
    instance = null;
  }

  @Override
  public CloudStreamServer.Response serve(
      String uri, String method, Properties header, Properties parms, Properties files) {
    CloudStreamServer.Response res = null;

    if (inputStream == null)
      res = new CloudStreamServer.Response(HTTP_NOTFOUND, MIME_PLAINTEXT, null);
    else {

      long startFrom = 0;
      long endAt = -1;
      String range = header.getProperty("range");
      if (range != null) {
        if (range.startsWith("bytes=")) {
          range = range.substring("bytes=".length());
          int minus = range.indexOf('-');
          try {
            if (minus > 0) {
              startFrom = Long.parseLong(range.substring(0, minus));
              endAt = Long.parseLong(range.substring(minus + 1));
            }
          } catch (NumberFormatException nfe) {
          }
        }
      }
      Log.d(TAG, "Request: " + range + " from: " + startFrom + ", to: " + endAt);

      // Change return code and add Content-Range header when skipping
      // is requested
      // source.open();
      final CloudStreamSource source = new CloudStreamSource(fileName, length, inputStream);
      long fileLen = source.length();
      if (range != null && startFrom > 0) {
        if (startFrom >= fileLen) {
          res = new CloudStreamServer.Response(HTTP_RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, null);
          res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
        } else {
          if (endAt < 0) endAt = fileLen - 1;
          long newLen = fileLen - startFrom;
          if (newLen < 0) newLen = 0;
          Log.d(TAG, "start=" + startFrom + ", endAt=" + endAt + ", newLen=" + newLen);
          final long dataLen = newLen;
          source.moveTo(startFrom);
          Log.d(TAG, "Skipped " + startFrom + " bytes");

          res = new CloudStreamServer.Response(HTTP_PARTIALCONTENT, null, source);
          res.addHeader("Content-length", "" + dataLen);
        }
      } else {
        source.reset();
        res = new CloudStreamServer.Response(HTTP_OK, null, source);
        res.addHeader("Content-Length", "" + fileLen);
      }
    }

    res.addHeader("Accept-Ranges", "bytes"); // Announce that the file
    // server accepts partial
    // content requestes
    return res;
  }
}
