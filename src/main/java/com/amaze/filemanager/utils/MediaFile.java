package com.amaze.filemanager.utils;

/**
 * Created by Arpit on 23-10-2014.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.amaze.filemanager.R;

/**
 * Wrapper for manipulating files via the Android Media Content Provider. As of Android 4.4 KitKat, applications can no longer write
 * to the "secondary storage" of a device. Write operations using the java.io.File API will thus fail. This class restores access to
 * those write operations by way of the Media Content Provider.
 *
 * Note that this class relies on the internal operational characteristics of the media content provider API, and as such is not
 * guaranteed to be future-proof. Then again, we did all think the java.io.File API was going to be future-proof for media card
 * access, so all bets are off.
 *
 * If you're forced to use this class, it's because Google/AOSP made a very poor API decision in Android 4.4 KitKat.
 * Read more at https://plus.google.com/+TodLiebeck/posts/gjnmuaDM8sn
 *
 * Your application must declare the permission "android.permission.WRITE_EXTERNAL_STORAGE".
 */
public class MediaFile {

    private static final String NO_MEDIA = ".nomedia";
    private static final String ALBUM_ART_URI = "content://media/external/audio/albumart";
    private static final String[] ALBUM_PROJECTION = { BaseColumns._ID, MediaStore.Audio.AlbumColumns.ALBUM_ID, "media_type" };

    private static File getExternalFilesDir(Context context) {


        try {
            Method method = Context.class.getMethod("getExternalFilesDir", String.class);
            return (File) method.invoke(context, (String) null);
        } catch (SecurityException ex) {
         //   Log.d(Maui.LOG_TAG, "Unexpected reflection error.", ex);
            return null;
        } catch (NoSuchMethodException ex) {
       //     Log.d(Maui.LOG_TAG, "Unexpected reflection error.", ex);
            return null;
        } catch (IllegalArgumentException ex) {
           // Log.d(Maui.LOG_TAG, "Unexpected reflection error.", ex);
            return null;
        } catch (IllegalAccessException ex) {
            //Log.d(Maui.LOG_TAG, "Unexpected reflection error.", ex);
            return null;
        } catch (InvocationTargetException ex) {
            //Log.d(Maui.LOG_TAG, "Unexpected reflection error.", ex);
            return null;
        }
    }


    private final File file;
    private final Context context;
    private final ContentResolver contentResolver;
    Uri filesUri;
    public MediaFile(Context context, File file) {
        this.file = file;
        this.context = context;
        contentResolver = context.getContentResolver();
        filesUri = MediaStore.Files.getContentUri("external");
    }

    /**
     * Deletes the file. Returns true if the file has been successfully deleted or otherwise does not exist. This operation is not
     * recursive.
     */
    public boolean delete()
            throws IOException {

        if (!file.exists()) {
            return true;
        }

        boolean directory = file.isDirectory();
        if (directory) {
            // Verify directory does not contain any files/directories within it.
            String[] files = file.list();
            if (files != null && files.length > 0) {
                return false;
            }
        }

        String where = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = new String[] { file.getAbsolutePath() };

        // Delete the entry from the media database. This will actually delete media files (images, audio, and video).
        contentResolver.delete(filesUri, where, selectionArgs);

        if (file.exists()) {
            // If the file is not a media file, create a new entry suggesting that this location is an image, even
            // though it is not.
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Delete the created entry, such that content provider will delete the file.
            contentResolver.delete(filesUri, where, selectionArgs);
        }

        return !file.exists();
    }

    public File getFile() {
        return file;
    }

    private int getTemporaryAlbumId() {
        final File temporaryTrack;
        try {
            temporaryTrack = installTemporaryTrack();
        } catch (IOException ex) {
            return 0;
        }

        final String[] selectionArgs = { temporaryTrack.getAbsolutePath() };
        Cursor cursor = contentResolver.query(filesUri, ALBUM_PROJECTION, MediaStore.MediaColumns.DATA + "=?",
                selectionArgs, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, temporaryTrack.getAbsolutePath());
            values.put(MediaStore.MediaColumns.TITLE, "{MediaWrite Workaround}");
            values.put(MediaStore.MediaColumns.SIZE, temporaryTrack.length());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, true);
            contentResolver.insert(filesUri, values);
        }
        cursor = contentResolver.query(filesUri, ALBUM_PROJECTION, MediaStore.MediaColumns.DATA + "=?",
                selectionArgs, null);
        if (cursor == null) {
            return 0;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return 0;
        }
        int id = cursor.getInt(0);
        int albumId = cursor.getInt(1);
        int mediaType = cursor.getInt(2);
        cursor.close();

        ContentValues values = new ContentValues();
        boolean updateRequired = false;
        if (albumId == 0) {
            values.put(MediaStore.Audio.AlbumColumns.ALBUM_ID, 13371337);
            updateRequired = true;
        }
        if (mediaType != 2) {
            values.put("media_type", 2);
            updateRequired = true;
        }
        if (updateRequired) {
            contentResolver.update(filesUri, values, BaseColumns._ID + "=" + id, null);
        }
        cursor = contentResolver.query(filesUri, ALBUM_PROJECTION, MediaStore.MediaColumns.DATA + "=?",
                selectionArgs, null);
        if (cursor == null) {
            return 0;
        }

        try {
            if (!cursor.moveToFirst()) {
                return 0;
            }
            return cursor.getInt(1);
        } finally {
            cursor.close();
        }
    }

    private File installTemporaryTrack()
            throws IOException {
        File externalFilesDir = getExternalFilesDir(context);
        if (externalFilesDir == null) {
            return null;
        }
        File temporaryTrack = new File(externalFilesDir, "temptrack.mp3");
        if (!temporaryTrack.exists()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = context.getResources().openRawResource(R.raw.temptrack);
                out = new FileOutputStream(temporaryTrack);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        return null;
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        return null;
                    }
                }
            }
        }
        return temporaryTrack;
    }
    public boolean mkdir()
            throws IOException {
        if (file.exists()) {
            return file.isDirectory();
        }

        File tmpFile = new File(file, ".MediaWriteTemp");
        int albumId = getTemporaryAlbumId();

        if (albumId == 0) {
            throw new IOException("Fail");
        }

        Uri albumUri = Uri.parse(ALBUM_ART_URI + '/' + albumId);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, tmpFile.getAbsolutePath());

        if (contentResolver.update(albumUri, values, null, null) == 0) {
            values.put(MediaStore.Audio.AlbumColumns.ALBUM_ID, albumId);
            contentResolver.insert(Uri.parse(ALBUM_ART_URI), values);
        }

        try {
            ParcelFileDescriptor fd = contentResolver.openFileDescriptor(albumUri, "r");
            fd.close();
        } finally {
            MediaFile tmpMediaFile = new MediaFile(context, tmpFile);
            tmpMediaFile.delete();
        }

        return file.exists();
    }

    /**
     * Returns an OutputStream to write to the file. The file will be truncated immediately.
     */
    public OutputStream write(long size)
            throws IOException {

        if (NO_MEDIA.equals(file.getName().trim())) {
            throw new IOException("Unable to create .nomedia file via media content provider API.");
        }

        if (file.exists() && file.isDirectory()) {
            throw new IOException("File exists and is a directory.");
        }

        // Delete any existing entry from the media database.
        // This may also delete the file (for media types), but that is irrelevant as it will be truncated momentarily in any case.
        String where = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = new String[] { file.getAbsolutePath() };
        contentResolver.delete(filesUri, where, selectionArgs);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.MediaColumns.SIZE, size);
        Uri uri = contentResolver.insert(filesUri, values);

        if (uri == null) {
            // Should not occur.
            throw new IOException("Internal error.");
        }

        return contentResolver.openOutputStream(uri);
    }
}