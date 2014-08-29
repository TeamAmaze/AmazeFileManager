package com.amaze.filemanager.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public abstract class MediaScannerUtils {
    /**
     * Request a MediaScanner scan for a single file.
     */
    private static Uri getFileUri(Context context,String fullname) {
        // Note: check outside this class whether the OS version is >= 11
        Uri uri=null;
        Cursor cursor=null;
        ContentResolver contentResolver=null;
        try{
            contentResolver=context.getContentResolver();
            if (contentResolver==null)
                return null;
            uri=MediaStore.Files.getContentUri("external");
            String[] projection=new String[2];
            projection[0]="_id";
            projection[1]="_data";
            String selection="_data = ? ";    // this avoids SQL injection
            String[] selectionParams=new String[1];
            selectionParams[0]=fullname;
            String sortOrder="_id";
            cursor=contentResolver.query(uri, projection, selection, selectionParams, sortOrder);
            if (cursor!=null) {
                try {
                    if (cursor.getCount()>0) {  // file present!
                        cursor.moveToFirst();
                        int dataColumn=cursor.getColumnIndex("_data");
                        String s=cursor.getString(dataColumn);
                        if (!s.equals(fullname))
                            return null;
                        int idColumn=cursor.getColumnIndex("_id");
                        long id=cursor.getLong(idColumn);
                        uri=MediaStore.Files.getContentUri("external",id);
                    } else {  // file isn't in the media database!
                        ContentValues contentValues=new ContentValues();
                        contentValues.put("_data",fullname);
                        uri=MediaStore.Files.getContentUri("external");
                        uri=contentResolver.insert(uri,contentValues);
                    }
                } catch (Throwable e) {
                    uri=null;
                }
                cursor.close();
            }
        } catch (Throwable e) {
            uri=null;
        }
        return uri;
    }

    public static OutputStream getOutputStreamViaContentProvider(Context context,String fullname) {
        Uri uri=getFileUri(context,fullname);
        if (uri==null)
            return null;
        try {
            ContentResolver resolver=context.getContentResolver();
            return resolver.openOutputStream(uri);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean deleteViaContentProvider(Context context,String fullname) {
        Uri uri=getFileUri(context,fullname);
        if (uri==null)
            return false;
        try {
            ContentResolver resolver=context.getContentResolver();
            // change type to image, otherwise nothing will be deleted
            ContentValues contentValues=new ContentValues();
            int media_type=1;
            contentValues.put("media_type", media_type);
            resolver.update(uri,contentValues,null,null);
            return resolver.delete(uri,null,null)>0;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean mkDirViaContentProvider(Context context,String fullname) {
        String fileName;
        Uri uri;
        ContentResolver resolver;
        try {
            resolver=context.getContentResolver();
            fileName = strcatslash(fullname)+"ghisler_temp.jpg";
            ContentValues contentValues=new ContentValues();
            contentValues.put("_data", fileName);
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);
        } catch (Throwable e) {
            return false;
        }
        if (uri!=null) {
            try {
                resolver.delete(uri,null,null);
            } catch (Throwable e) {
            }
            return true;
        } else
            return false;
    }
    public static String strcatslash(String dir) {
        if (dir.length()>0 && !dir.endsWith("/"))
            return dir+"/";
        else
            return dir;
    }


    public static void informFileAdded(Context c, File f) {
        if(f == null)
            return;

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(f));
        c.sendBroadcast(intent);
    }

    /**
     * Request a MediaScanner scan for multiple files.
     */
    public static void informFilesAdded(Context c, File[] files) {
        // NOTE: it seemed like overkill having to create a Helper class to
        // avoid VerifyError on 1.6 so that we can use MediaScannerConnection.scanFile()
        // Therefore we just iterate through files and use the compatible-with-every-version broadcast.
        for(int i = 0; i < files.length; i++)
            informFileAdded(c, files[i]);
    }


    public static void informFileDeleted(Context c, File f) {
        // TODO implement
    }

    public static void informFilesDeleted(Context c, File[] files) {
        for(File f : files)
            informFileDeleted(c, f);


    }
}
