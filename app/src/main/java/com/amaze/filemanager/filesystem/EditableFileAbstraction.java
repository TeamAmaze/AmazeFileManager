package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.amaze.filemanager.utils.Utils;

import java.io.FileNotFoundException;

/**
 * This is a special representation of a file that is to be used so that uris can be loaded as
 * editable files.
 *
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 16/1/2018, at 17:06.
 */

public class EditableFileAbstraction {

    public static final int SCHEME_CONTENT = 0;
    public static final int SCHEME_FILE = 1;

    public final Uri uri;
    public final String name;
    public final int scheme;
    public final HybridFileParcelable hybridFileParcelable;

    public EditableFileAbstraction(Context context, Uri uri) throws FileNotFoundException {
        switch (uri.getScheme()) {
            case "content":
                this.uri = uri;
                this.scheme = SCHEME_CONTENT;

                String tempName = null;
                Cursor c = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                        null, null, null);

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
                    //At least we have something to show the user...
                    tempName = uri.getLastPathSegment();
                }

                this.name = tempName;

                this.hybridFileParcelable = null;
                break;
            case "file":
                this.scheme = SCHEME_FILE;

                String path = uri.getPath();
                if(path == null) throw new NullPointerException("Uri '" + uri.toString() + "' is not hierarchical!");
                path = Utils.sanitizeInput(path);
                this.hybridFileParcelable = new HybridFileParcelable(path);

                String tempN = hybridFileParcelable.getName(context);
                if(tempN == null) tempN = uri.getLastPathSegment();
                this.name = tempN;

                this.uri = null;
                break;
            default:
                throw new IllegalArgumentException("The scheme '" + uri.getScheme() + "' cannot be processed!");
        }
    }

}
