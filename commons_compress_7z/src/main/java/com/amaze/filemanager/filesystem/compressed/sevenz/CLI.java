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

package com.amaze.filemanager.filesystem.compressed.sevenz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CLI {

  private enum Mode {
    LIST("Analysing") {
      @Override
      public void takeAction(final SevenZFile archive, final SevenZArchiveEntry entry) {
        System.out.print(entry.getName());
        if (entry.isDirectory()) {
          System.out.print(" dir");
        } else {
          System.out.print(" " + entry.getCompressedSize() + "/" + entry.getSize());
        }
        if (entry.getHasLastModifiedDate()) {
          System.out.print(" " + entry.getLastModifiedDate());
        } else {
          System.out.print(" no last modified date");
        }
        if (!entry.isDirectory()) {
          System.out.println(" " + getContentMethods(entry));
        } else {
          System.out.println("");
        }
      }

      private String getContentMethods(final SevenZArchiveEntry entry) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final SevenZMethodConfiguration m : entry.getContentMethods()) {
          if (!first) {
            sb.append(", ");
          }
          first = false;
          sb.append(m.getMethod());
          if (m.getOptions() != null) {
            sb.append("(").append(m.getOptions()).append(")");
          }
        }
        return sb.toString();
      }
    },
    EXTRACT("Extracting") {
      private final byte[] buf = new byte[8192];

      @Override
      public void takeAction(final SevenZFile archive, final SevenZArchiveEntry entry)
          throws IOException {
        final File outFile = new File(entry.getName());
        if (entry.isDirectory()) {
          if (!outFile.isDirectory() && !outFile.mkdirs()) {
            throw new IOException("Cannot create directory " + outFile);
          }
          System.out.println("created directory " + outFile);
          return;
        }

        System.out.println("extracting to " + outFile);
        final File parent = outFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
          throw new IOException("Cannot create " + parent);
        }
        try (final FileOutputStream fos = new FileOutputStream(outFile)) {
          final long total = entry.getSize();
          long off = 0;
          while (off < total) {
            final int toRead = (int) Math.min(total - off, buf.length);
            final int bytesRead = archive.read(buf, 0, toRead);
            if (bytesRead < 1) {
              throw new IOException(
                  "reached end of entry "
                      + entry.getName()
                      + " after "
                      + off
                      + " bytes, expected "
                      + total);
            }
            off += bytesRead;
            fos.write(buf, 0, bytesRead);
          }
        }
      }
    };

    private final String message;

    Mode(final String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }

    public abstract void takeAction(SevenZFile archive, SevenZArchiveEntry entry)
        throws IOException;
  }

  public static void main(final String[] args) throws Exception {
    if (args.length == 0) {
      usage();
      return;
    }
    final Mode mode = grabMode(args);
    System.out.println(mode.getMessage() + " " + args[0]);
    final File f = new File(args[0]);
    if (!f.isFile()) {
      System.err.println(f + " doesn't exist or is a directory");
    }
    try (final SevenZFile archive = new SevenZFile(f)) {
      SevenZArchiveEntry ae;
      while ((ae = archive.getNextEntry()) != null) {
        mode.takeAction(archive, ae);
      }
    }
  }

  private static void usage() {
    System.out.println("Parameters: archive-name [list|extract]");
  }

  private static Mode grabMode(final String[] args) {
    if (args.length < 2) {
      return Mode.LIST;
    }
    return Enum.valueOf(Mode.class, args[1].toUpperCase());
  }
}
