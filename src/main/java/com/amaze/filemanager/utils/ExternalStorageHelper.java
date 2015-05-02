/*
 * Copyright (C) 2015 JRummy Apps, Inc. - All Rights Reserved
 *
 * Unauthorized copying or redistribution of this file in source and binary forms via any medium
 * is strictly prohibited.
 *
 * Created by Jared Rummler <jared.rummler@gmail.com>, Mar 28, 2015
 */
package com.amaze.filemanager.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.stericson.RootTools.RootTools;

/**
 * Helper class to manage files on a removable storage device.
 *
 * @author Jared Rummler <jared.rummler@gmail.com>
 */
public class ExternalStorageHelper {

    private static final String TAG = "ExternalStorageHelper";

    private static final boolean IS_ROOTED = RootTools.isAccessGiven();

    public static DocumentFile getFor(final File file,MainActivity mainActivity) throws FileNotFoundException {
        return getFor(file.getAbsolutePath(),mainActivity);
    }

    public static DocumentFile getFor(final String path,MainActivity mainActivity) throws FileNotFoundException {
        final DocumentFile documentFile = ExternalStoragePermissionGrant.getInstance(mainActivity).findFile(
            path,mainActivity);
        if (documentFile == null) {
            throw new FileNotFoundException(path + " not found on external storage.");
        }
        return documentFile;
    }

    public static boolean delete(final File file,MainActivity c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                final DocumentFile document = getFor(file.getAbsolutePath(),c);
                if (document.delete()) {
                    return true;
                }
            } catch (final FileNotFoundException e) {
                Log.d(TAG, "Document not found for %s"+ e+ file.getAbsolutePath());
            }
        }
        try {
            if (MediaStoreHack.delete(c, file)) {
                return true;
            }
        } catch (final Exception e) {
            Log.d(TAG, "Failed deleting %s with media hack" +e+ file.getPath());
        }
        if (IS_ROOTED) {
            return RootTools.deleteFileOrDirectory(file.getPath(), true);
        }
        return false;
    }

    public static OutputStream getOutputStream(final File file,MainActivity c) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile documentFile;
            if (file.exists()) {
                documentFile = getFor(file,c);
            } else {
                final DocumentFile parentDocument = ExternalStoragePermissionGrant.getInstance(c)
                        .findFile(file.getParent(),c);
                if (parentDocument == null) {
                    throw new FileNotFoundException("parent doesn't exist.");
                }
                documentFile = parentDocument.createFile(MimeTypes.getMimeType(file),
                    file.getName());
                if (documentFile == null) {
                    throw new FileNotFoundException("Unable to create file.");
                }
            }
            final OutputStream outputStream = c.getContentResolver()
                    .openOutputStream(documentFile.getUri());
            if (outputStream != null) {
                return outputStream;
            }
        }
        return MediaStoreHack.getOutputStream(c, file, file.length());
    }

    public static InputStream getInputStream(final File file,MainActivity c) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile documentFile;
            if (file.exists()) {
                documentFile = getFor(file,c);
            } else {
                final DocumentFile parentDocument = ExternalStoragePermissionGrant.getInstance(c)
                        .findFile(file.getParent(),c);
                if (parentDocument == null) {
                    throw new FileNotFoundException("parent doesn't exist.");
                }
                documentFile = parentDocument.createFile(MimeTypes.getMimeType(file),
                    file.getName());
                if (documentFile == null) {
                    throw new FileNotFoundException("Unable to create file.");
                }
            }
            final InputStream stream = c.getContentResolver()
                    .openInputStream(documentFile.getUri());
            if (stream != null) {
                return stream;
            }
        }
        return MediaStoreHack.getInputStream(c, file, file.length());
    }

    public static boolean mkdir(final File file,MainActivity c) {
        try {
            if (getFor(file.getParentFile(),c).createDirectory(file.getName()) != null) {
                return true;
            }
        } catch (final Exception e) {
            Log.e(TAG, "Failed created DocumentFile: %s"+ e+ file.getPath());
        }
        try {
            if (MediaStoreHack.mkdir(c, file)) {
                return true;
            }
        } catch (final Exception e) {
            Log.d(TAG, "Failed creating %s with media hack"+ e+ file.getPath());
        }
        if (file.mkdirs()) {
            return true;
        }
        //TOdo
         /*if (IS_ROOTED && Commands.mkdir(file)) {
            return true;
        }*/
        return false;
    }

    public static boolean mkfile(final File file,MainActivity c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (getFor(file,c).createFile(MimeTypes.getMimeType(file), file.getName()) != null) {
                    return true;
                }
            } catch (final FileNotFoundException e) {
            }
        }
        try {
            MediaStoreHack.mkdir(c, file);
            if (file.isDirectory()) {
                return true;
            }
        } catch (final Exception e) {
        }
        /*try {
            FileUtils.touch(file);
            return file.exists();
        } catch (final IOException e) {
        }
        if (IS_ROOTED) {
            return Commands.touch(file);
        }*/
        return false;
    }
/*
    public static boolean moveTo(final File source, final File destination) {
        if (IS_ROOTED) {
            if (Commands.mv(source, destination)) {
                return true;
            }
        }
        return source.renameTo(destination);
    }

    public static boolean rename(final File source, final File destination) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (getFor(source).renameTo(destination.getName())) {
                    return true;
                }
            } catch (final FileNotFoundException e) {
            }
        }
        return moveTo(source, destination);
    }
*/
}
