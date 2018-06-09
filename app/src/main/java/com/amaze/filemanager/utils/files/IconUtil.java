package com.amaze.filemanager.utils.files;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.amaze.filemanager.ui.icons.Icons;

public class IconUtil {

    private Context context;
    private String path;
    private final String where;
    private Uri baseUri;
    private String[] projection;
    private int mimeType;

    public IconUtil(Context context, String path, boolean isDirectory, String volume) {
        this.context = context;
        this.path = path;
        where = MediaStore.MediaColumns.DATA + " = ?";
        mimeType = Icons.getTypeOfFile(path, isDirectory);

        IconFactory iconFactory = new IconFactory();
        baseUri = iconFactory.getUriInstance(mimeType, volume);
        projection = iconFactory.getProjectionInstance(mimeType);

    }

    public Uri findURIByusingIcon() {
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(baseUri, projection, where, new String[]{path}, null);
        try {
            if (c != null && c.moveToNext()) {
                boolean isValid = false;
                if (mimeType == Icons.IMAGE || mimeType == Icons.VIDEO || mimeType == Icons.AUDIO) {
                    isValid = true;
                } else {
                    int type = c.getInt(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
                    isValid = type != 0;
                }

                if (isValid) {
                    // Do not force to use content uri for no media files
                    long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
                    return Uri.withAppendedPath(baseUri, String.valueOf(id));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

}
