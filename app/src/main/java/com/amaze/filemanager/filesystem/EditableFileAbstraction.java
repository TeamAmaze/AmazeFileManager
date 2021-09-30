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

import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.CONTENT;
import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.FILE;

import com.amaze.filemanager.utils.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

/**
 * This is a special representation of a file that is to be used so that uris can be loaded as
 * editable files.
 *
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 16/1/2018, at 17:06.
 */
public class EditableFileAbstraction {

  public enum Scheme {
    CONTENT,
    FILE
  }

  public final Uri uri;
  public final String name;
  public final Scheme scheme;
  public final HybridFileParcelable hybridFileParcelable;

  public EditableFileAbstraction(@NonNull Context context, @NonNull Uri uri) {
    switch (uri.getScheme()) {
      case ContentResolver.SCHEME_CONTENT:
        this.uri = uri;
        this.scheme = CONTENT;

        String tempName = null;
        Cursor c =
            context
                .getContentResolver()
                .query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null);

        if (c != null) {
          c.moveToFirst();
          try {
            /*
            The result and whether [Cursor.getString()] throws an exception when the column
            value is null or the column type is not a string type is implementation-defined.
            */
            tempName = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
          } catch (Exception e) {
            tempName = null;
          }
          c.close();
        }

        if (tempName == null) {
          // At least we have something to show the user...
          tempName = uri.getLastPathSegment();
        }

        this.name = tempName;

        this.hybridFileParcelable = null;
        break;
      case ContentResolver.SCHEME_FILE:
        this.scheme = FILE;

        String path = uri.getPath();
        if (path == null)
          throw new NullPointerException("Uri '" + uri.toString() + "' is not hierarchical!");
        path = Utils.sanitizeInput(path);
        this.hybridFileParcelable = new HybridFileParcelable(path);

        String tempN = hybridFileParcelable.getName(context);
        if (tempN == null) tempN = uri.getLastPathSegment();
        this.name = tempN;

        this.uri = null;
        break;
      default:
        throw new IllegalArgumentException(
            "The scheme '" + uri.getScheme() + "' cannot be processed!");
    }
  }
}
