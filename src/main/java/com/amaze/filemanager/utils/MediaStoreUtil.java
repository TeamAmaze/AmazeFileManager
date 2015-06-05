package com.amaze.filemanager.utils;

/**
 * Created by Arpit on 04-06-2015.
 */import java.io.File;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

/**
 * Utility class for handling the media store.
 */
public abstract class MediaStoreUtil {
    public static Uri getUriFromFile(final String path,Context context) {
        ContentResolver resolver = context.getContentResolver();

        Cursor filecursor = resolver.query(MediaStore.Files.getContentUri("external"),
                new String[] { BaseColumns._ID }, MediaColumns.DATA + " = ?",
                new String[] { path }, MediaColumns.DATE_ADDED + " desc");
        filecursor.moveToFirst();

        if (filecursor.isAfterLast()) {
            filecursor.close();
            ContentValues values = new ContentValues();
            values.put(MediaColumns.DATA, path);
            return resolver.insert(MediaStore.Files.getContentUri("external"), values);
        }
        else {
            int imageId = filecursor.getInt(filecursor.getColumnIndex(BaseColumns._ID));
            Uri uri = MediaStore.Files.getContentUri("external").buildUpon().appendPath(
                    Integer.toString(imageId)).build();
            filecursor.close();
            return uri;
        }
    }

    public static final void addFileToMediaStore(final String path,Context context) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(path);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * Retrieve a thumbnail of a bitmap from the mediastore.
     *
     * @param path
     *            The path of the image
     * @param maxSize
     *            The maximum size of this bitmap (used for selecting the sample size)
     * @return the thumbnail.
     */

    /**
     * Delete the thumbnail of a bitmap.
     *
     * @param path
     *            The path of the image
     */

}
