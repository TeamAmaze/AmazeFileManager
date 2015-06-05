package com.amaze.filemanager.utils;

/**
 * Created by Arpit on 04-06-2015.
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.R;

/**
 * Utility class for helping parsing file systems.
 */
public abstract class FileUtil {
    /**
     * Determine the camera folder. There seems to be no Android API to work for real devices, so this is a best guess.
     *
     * @return the default camera folder.
     */
    public static String getDefaultCameraFolder() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (path.exists()) {
            File test1 = new File(path, "Camera/");
            if (test1.exists()) {
                path = test1;
            }
            else {
                File test2 = new File(path, "100ANDRO/");
                if (test2.exists()) {
                    path = test2;
                }
                else {
                    File test3 = new File(path, "100MEDIA/");
                    path = test3;
                }
            }
        }
        else {
            File test3 = new File(path, "Camera/");
            path = test3;
        }
        return path.getAbsolutePath();
    }

    /**
     * Copy a file. The target file may even be on external SD card for Kitkat.
     *
     * @param source
     *            The source file
     * @param target
     *            The target file
     * @return true if the copying was successful.
     */
    @SuppressWarnings("null")
    public static boolean copyFile(final File source, final File target,Context context) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(source);

            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
                inChannel = inStream.getChannel();
                outChannel = ((FileOutputStream) outStream).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            }
            else {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(target, false,context);
                    outStream =
                            context.getContentResolver().openOutputStream(targetDocument.getUri());
                }
                else if (Build.VERSION.SDK_INT==Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    Uri uri = MediaStoreUtil.getUriFromFile(target.getAbsolutePath(),context);
                    outStream = context.getContentResolver().openOutputStream(uri);
                }
                else {
                    return false;
                }

                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    byte[] buffer = new byte[16384]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }

            }
        }
        catch (Exception e) {
            Log.e("AmazeFileUtils",
                    "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
            return false;
        }
        finally {
            try {
                inStream.close();
            }
            catch (Exception e) {
                // ignore exception
            }
            try {
                outStream.close();
            }
            catch (Exception e) {
                // ignore exception
            }
            try {
                inChannel.close();
            }
            catch (Exception e) {
                // ignore exception
            }
            try {
                outChannel.close();
            }
            catch (Exception e) {
                // ignore exception
            }
        }
        return true;
    }
    public static OutputStream getOutputStream(final File target,Context context) {

        OutputStream outStream = null;
        try {

            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
            }
            else {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(target, false,context);
                    outStream =
                            context.getContentResolver().openOutputStream(targetDocument.getUri());
                }
                else if (Build.VERSION.SDK_INT==Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    Uri uri = MediaStoreUtil.getUriFromFile(target.getAbsolutePath(),context);
                    outStream = context.getContentResolver().openOutputStream(uri);
                }



            }
        }
        catch (Exception e) {
            Log.e("AmazeFileUtils",
                    "Error when copying file from " +  target.getAbsolutePath(), e);
        }
      return outStream;
        }
    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file
     *            the file to be deleted.
     * @return True if successfully deleted.
     */
    public static final boolean deleteFile(final File file,Context context) {
        // First try the normal deletion.
        if(file.isDirectory())
            deleteFilesInFolder(file,context);
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(file, false,context);
            return document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT==Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();

            try {
                Uri uri = MediaStoreUtil.getUriFromFile(file.getAbsolutePath(),context);
                resolver.delete(uri, null, null);
                return !file.exists();
            }
            catch (Exception e) {
                Log.e("AmazeFileUtils", "Error when deleting file " + file.getAbsolutePath(), e);
                return false;
            }
        }

        return !file.exists();
    }

    /**
     * Move a file. The target file may even be on external SD card.
     *
     * @param source
     *            The source file
     * @param target
     *            The target file
     * @return true if the copying was successful.
     */
    public static final boolean moveFile(final File source, final File target,Context context) {
        // First try the normal rename.
        if (source.renameTo(target)) {
            return true;
        }

        boolean success = copyFile(source, target,context);
        if (success) {
            success = deleteFile(source,context);
        }
        return success;
    }

    /**
     * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are moved.
     *
     * @param source
     *            The source folder.
     * @param target
     *            The target folder.
     * @return true if the renaming was successful.
     */
    public static final boolean renameFolder(final File source, final File target,Context context) {
        // First try the normal rename.
        if (source.renameTo(target)) {
            return true;
        }
        if (target.exists()) {
            return false;
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP && source.getParent().equals(target.getParent())) {
            DocumentFile document = getDocumentFile(source, true,context);
            if (document.renameTo(target.getName())) {
                return true;
            }
        }

        // Try the manual way, moving files individually.
        if (!mkdir(target,context)) {
            return false;
        }

        File[] sourceFiles = source.listFiles();

        if (sourceFiles == null) {
            return true;
        }

        for (File sourceFile : sourceFiles) {
            String fileName = sourceFile.getName();
            File targetFile = new File(target, fileName);
            if (!copyFile(sourceFile, targetFile,context)) {
                // stop on first error
                return false;
            }
        }
        // Only after successfully copying all files, delete files on source folder.
        for (File sourceFile : sourceFiles) {
            if (!deleteFile(sourceFile,context)) {
                // stop on first error
                return false;
            }
        }
        return true;
    }

    /**
     * Get a temp file.
     *
     * @param file
     *            The base file for which to create a temp file.
     * @return The temp file.
     */
    public static final File getTempFile(final File file,Context context) {
        File extDir = context.getExternalFilesDir(null);
        File tempFile = new File(extDir, file.getName());
        return tempFile;
    }

    /**
     * Create a folder. The folder may even be on external SD card for Kitkat.
     *
     * @param file
     *            The folder to be created.
     * @return True if creation was successful.
     */
    public static boolean mkdir(final File file,Context context) {
        if (file.exists()) {
            // nothing to create.
            return file.isDirectory();
        }

        // Try the normal way
        if (file.mkdir()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(file, true,context);
            // getDocumentFile implicitly creates the directory.
            return document.exists();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT==Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();
            File tempFile = new File(file, "dummyImage.jpg");

            File dummySong = copyDummyFiles(context);
            int albumId = MediaStoreUtil.getAlbumIdFromAudioFile(dummySong,context);
            Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DATA, tempFile.getAbsolutePath());
            contentValues.put(MediaStore.Audio.AlbumColumns.ALBUM_ID, albumId);

            if (resolver.update(albumArtUri, contentValues, null, null) == 0) {
                resolver.insert(Uri.parse("content://media/external/audio/albumart"), contentValues);
            }
            try {
                ParcelFileDescriptor fd = resolver.openFileDescriptor(albumArtUri, "r");
                fd.close();
            }
            catch (Exception e) {
                Log.e("AmazeFileUtils", "Could not open file", e);
                return false;
            }
            finally {
                FileUtil.deleteFile(tempFile,context);
            }

            return true;
        }

        return false;
    }

    /**
     * Delete a folder.
     *
     * @param file
     *            The folder name.
     *
     * @return true if successful.
     */
    public static boolean rmdir1(final File file,Context context){
        boolean b = true;
        for (File file1 : file.listFiles()) {
            if (file1.isDirectory()) {
                if (!rmdir1(file1, context)) b = false;
            } else {
                if (!deleteFile(file1, context)) b = false;
            }
        }
        return b;
    }
    public static boolean rmdir(final File file,Context context) {
        if (!file.exists()) {
            return true;
        }
        if (!file.isDirectory()) {
            return false;
        }
        String[] fileList = file.list();
        if (fileList != null && fileList.length > 0) {
            //  empty the folder.
               rmdir1(file,context);
        }
        String[] fileList1 = file.list();
        if (fileList1 != null && fileList1.length > 0) {
            // Delete only empty folder.
            return false;
        }
        // Try the normal way
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(file, true,context);
            return document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT==Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Delete the created entry, such that content provider will delete the file.
            resolver.delete(MediaStore.Files.getContentUri("external"), MediaStore.MediaColumns.DATA + "=?",
                    new String[] { file.getAbsolutePath() });
        }

        return !file.exists();
    }

    /**
     * Delete all files in a folder.
     *
     * @param folder
     *            the folder
     * @return true if successful.
     */
    public static final boolean deleteFilesInFolder(final File folder,Context context) {
        boolean totalSuccess = true;

        String[] children = folder.list();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                File file = new File(folder, children[i]);
                if (!file.isDirectory()) {
                    boolean success = FileUtil.deleteFile(file,context);
                    if (!success) {
                        Log.w("AmazeFileUtils", "Failed to delete file" + children[i]);
                        totalSuccess = false;
                    }
                }else deleteFilesInFolder(file,context);
            }
        }
        return totalSuccess;
    }

    /**
     * Delete a directory asynchronously.
     *
     * @param activity
     *            The activity calling this method.
     * @param file
     *            The folder name.
     * @param postActions
     *            Commands to be executed after success.
     */
    public static void rmdirAsynchronously(final Activity activity, final File file, final Runnable postActions, final Context context) {
        new Thread() {
            @Override
            public void run() {
                int retryCounter = 5; // MAGIC_NUMBER
                while (!FileUtil.rmdir(file,context) && retryCounter > 0) {
                    try {
                        Thread.sleep(100); // MAGIC_NUMBER
                    }
                    catch (InterruptedException e) {
                        // do nothing
                    }
                    retryCounter--;
                }
                if (file.exists()) {
           /*         DialogUtil.displayError(activity, R.string.message_dialog_failed_to_delete_folder, false,
                            file.getAbsolutePath());
           */     }
                else {
                    activity.runOnUiThread(postActions);
                }

            }
        }.start();
    }

    /**
     * Check is a file is writable. Detects write issues on external SD card.
     *
     * @param file
     *            The file
     * @return true if the file is writable.
     */
    public static final boolean isWritable(final File file) {
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.close();
            }
            catch (IOException e) {
                // do nothing.
            }
        }
        catch (FileNotFoundException e) {
            return false;
        }
        boolean result = file.canWrite();

        // Ensure that file is not created during this process.
        if (!isExisting) {
            file.delete();
        }

        return result;
    }

    // Utility methods for Android 5

    /**
     * Check for a directory if it is possible to create files within this directory, either via normal writing or via
     * Storage Access Framework.
     *
     * @param folder
     *            The directory
     * @return true if it is possible to write in this directory.
     */
    public static final boolean isWritableNormalOrSaf(final File folder,Context c) {
        // Verify that this is a directory.
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }

        // Find a non-existing file in this directory.
        int i = 0;
        File file;
        do {
            String fileName = "AugendiagnoseDummyFile" + (++i);
            file = new File(folder, fileName);
        }
        while (file.exists());

        // First check regular writability
        if (isWritable(file)) {
            return true;
        }

        // Next check SAF writability.
        DocumentFile document = getDocumentFile(file, false,c);

        if (document == null) {
            return false;
        }

        // This should have created the file - otherwise something is wrong with access URL.
        boolean result = document.canWrite() && file.exists();

        // Ensure that the dummy file is not remaining.
        document.delete();

        return result;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<String>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w("AmazeFileUtils", "Unexpected external file dir: " + file.getAbsolutePath());
                }
                else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    }
                    catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths.toArray(new String[0]);
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file
     *            the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     *         null is returned.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getExtSdCardFolder(final File file,Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        }
        catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file
     *            The file.
     * @return true if on external sd card.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isOnExtSdCard(final File file,Context c) {
        return getExtSdCardFolder(file,c) != null;
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
     * existing, it is created.
     *
     * @param file
     *            The file.
     * @param isDirectory
     *            flag indicating if the file should be a directory.
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory,Context context) {
        String baseFolder = getExtSdCardFolder(file,context);

        if (baseFolder == null) {
            return null;
        }

        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            relativePath = fullPath.substring(baseFolder.length() + 1);
        }
        catch (IOException e) {
            return null;
        }
        String as=PreferenceManager.getDefaultSharedPreferences(context).getString("URI",null);

        Uri treeUri =null;
        if(as!=null)treeUri=Uri.parse(as);
        if (treeUri == null) {
            return null;
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);

        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                }
                else {
                    nextDocument = document.createFile("image", parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }

    // Utility methods for Kitkat

    /**
     * Copy a resource file into a private target directory, if the target does not yet exist. Required for the Kitkat
     * workaround.
     *
     * @param resource
     *            The resource file.
     * @param folderName
     *            The folder below app folder where the file is copied to.
     * @param targetName
     *            The name of the target file.
     * @return the dummy file.
     * @throws IOException
     */
    private static File copyDummyFile(final int resource, final String folderName, final String targetName,Context context)
            throws IOException {
        File externalFilesDir = context.getExternalFilesDir(folderName);
        if (externalFilesDir == null) {
            return null;
        }
        File targetFile = new File(externalFilesDir, targetName);

        if (!targetFile.exists()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = context.getResources().openRawResource(resource);
                out = new FileOutputStream(targetFile);
                byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    }
                    catch (IOException ex) {
                        // do nothing
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    }
                    catch (IOException ex) {
                        // do nothing
                    }
                }
            }
        }
        return targetFile;
    }

    /**
     * Copy the dummy image and dummy mp3 into the private folder, if not yet there. Required for the Kitkat workaround.
     *
     * @return the dummy mp3.
     */
    private static File copyDummyFiles(Context c) {
        try {
            copyDummyFile(R.mipmap.ic_launcher, "mkdirFiles", "albumart.jpg",c);
            return copyDummyFile(R.raw.temptrack, "mkdirFiles", "temptrack.mp3",c);

        }
        catch (IOException e) {
            Log.e("AmazeFileUtils", "Could not copy dummy files.", e);
            return null;
        }
    }

}
