package com.amaze.filemanager.filesystem;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.RootUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Utility class for helping parsing file systems.
 * <p>
 * Created by Arpit on 04-06-2015.
 */
public abstract class FileUtil {
    /**
     * Determine the camera folder. There seems to be no Android API to work for real devices, so this is a best guess.
     *
     * @return the default camera folder.
     **/
    //TODO the function?

    /**
     * Copy a file. The target file may even be on external SD card for Kitkat.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    @SuppressWarnings("null")
    private static boolean copyFile(final File source, final File target, Context context) {
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
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(target, false, context);
                    outStream =
                            context.getContentResolver().openOutputStream(targetDocument.getUri());
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    Uri uri = MediaStoreHack.getUriFromFile(target.getAbsolutePath(), context);
                    outStream = context.getContentResolver().openOutputStream(uri);
                } else {
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
        } catch (Exception e) {
            Log.e("AmazeFileUtils",
                    "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
            return false;
        } finally {
            try {
                inStream.close();
            } catch (Exception e) {
                // ignore exception
            }

            try {
                outStream.close();
            } catch (Exception e) {
                // ignore exception
            }

            try {
                inChannel.close();
            } catch (Exception e) {
                // ignore exception
            }

            try {
                outChannel.close();
            } catch (Exception e) {
                // ignore exception
            }
        }
        return true;
    }

    public static OutputStream getOutputStream(final File target, Context context) throws Exception {
        return getOutputStream(target, context, 0);
    }

    public static OutputStream getOutputStream(final File target, Context context, long s) throws Exception {
        OutputStream outStream = null;
        try {
            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(target, false, context);
                    outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    return MediaStoreHack.getOutputStream(context, target.getPath());
                }
            }
        } catch (Exception e) {
            Log.e("AmazeFileUtils",
                    "Error when copying file from " + target.getAbsolutePath(), e);
            throw new Exception();
        }
        return outStream;
    }

    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    static boolean deleteFile(@NonNull final File file, Context context) {
        // First try the normal deletion.
        if (file == null) return true;
        boolean fileDelete = deleteFilesInFolder(file, context);
        if (file.delete() || fileDelete)
            return true;

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(file, context)) {

            DocumentFile document = getDocumentFile(file, false, context);
            return document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();

            try {
                Uri uri = MediaStoreHack.getUriFromFile(file.getAbsolutePath(), context);
                resolver.delete(uri, null, null);
                return !file.exists();
            } catch (Exception e) {
                Log.e("AmazeFileUtils", "Error when deleting file " + file.getAbsolutePath(), e);
                return false;
            }
        }

        return !file.exists();
    }

    private static boolean rename(File f, String name, boolean root) throws RootNotPermittedException {
        String newPath = f.getParent() + "/" + name;
        if (f.getParentFile().canWrite()) {
            return f.renameTo(new File(newPath));
        } else if (root) {
            RootUtils.rename(f.getPath(), newPath);
            return true;
        }
        return false;
    }

    /**
     * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are moved.
     *
     * @param source The source folder.
     * @param target The target folder.
     * @return true if the renaming was successful.
     */
    static boolean renameFolder(@NonNull final File source, @NonNull final File target,
                                Context context) throws RootNotPermittedException {
        // First try the normal rename.
        if (rename(source, target.getName(), false)) {
            return true;
        }
        if (target.exists()) {
            return false;
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && source.getParent().equals(target.getParent()) && FileUtil.isOnExtSdCard(source, context)) {
            DocumentFile document = getDocumentFile(source, true, context);
            if (document.renameTo(target.getName())) {
                return true;
            }
        }

        // Try the manual way, moving files individually.
        if (!mkdir(target, context)) {
            return false;
        }

        File[] sourceFiles = source.listFiles();

        if (sourceFiles == null) {
            return true;
        }

        for (File sourceFile : sourceFiles) {
            String fileName = sourceFile.getName();
            File targetFile = new File(target, fileName);
            if (!copyFile(sourceFile, targetFile, context)) {
                // stop on first error
                return false;
            }
        }
        // Only after successfully copying all files, delete files on source folder.
        for (File sourceFile : sourceFiles) {
            if (!deleteFile(sourceFile, context)) {
                // stop on first error
                return false;
            }
        }
        return true;
    }

    /**
     * Get a temp file.
     *
     * @param file The base file for which to create a temp file.
     * @return The temp file.
     */
    public static File getTempFile(@NonNull final File file, Context context) {
        File extDir = context.getExternalFilesDir(null);
        return new File(extDir, file.getName());
    }

    /**
     * Create a folder. The folder may even be on external SD card for Kitkat.
     *
     * @deprecated use {@link #mkdirs(Context, HFile)}
     * @param file  The folder to be created.
     * @return True if creation was successful.
     */
    public static boolean mkdir(final File file, Context context) {
        if(file==null)
            return false;
        if (file.exists()) {
            // nothing to create.
            return file.isDirectory();
        }

        // Try the normal way
        if (file.mkdirs()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(file, context)) {
            DocumentFile document = getDocumentFile(file, true, context);
            // getDocumentFile implicitly creates the directory.
            return document.exists();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            try {
                return MediaStoreHack.mkdir(context, file);
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }

    public static boolean mkdirs(Context context, HFile file) {
        boolean isSuccessful = true;
        switch (file.mode) {
            case SMB:
                try {
                    SmbFile smbFile = new SmbFile(file.getPath());
                    smbFile.mkdirs();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    isSuccessful =  false;
                } catch (SmbException e) {
                    e.printStackTrace();
                    isSuccessful = false;
                }
                break;
            case OTG:
                DocumentFile documentFile = RootHelper.getDocumentFile(file.getPath(), context, true);
                isSuccessful = documentFile != null;
                break;
            case FILE:
                isSuccessful = mkdir(new File(file.getPath()), context);
                break;
            default:
                isSuccessful = true;
                break;
        }

        return isSuccessful;
    }

    public static boolean mkfile(final File file,Context context) throws IOException {
        if(file==null)
            return false;
        if (file.exists()) {
            // nothing to create.
            return !file.isDirectory();
        }

        // Try the normal way
        try {
            if (file.createNewFile()) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(file, context)) {
            DocumentFile document = getDocumentFile(file.getParentFile(), true, context);
            // getDocumentFile implicitly creates the directory.
            try {
                return document.createFile(MimeTypes.getMimeType(file), file.getName()) != null;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            try {
                return MediaStoreHack.mkfile(context, file);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Delete a folder.
     *
     * @param file The folder name.
     * @return true if successful.
     */
    private static boolean rmdir1(final File file, Context context) {
        if (file == null)
            return false;
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

    private static boolean rmdir(final File file, Context context) {
        if (file == null)
            return false;
        if (!file.exists()) {
            return true;
        }
        if (!file.isDirectory()) {
            return false;
        }
        String[] fileList = file.list();
        if (fileList != null && fileList.length > 0) {
            //  empty the folder.
            rmdir1(file, context);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(file, true, context);
            return document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Delete the created entry, such that content provider will delete the file.
            resolver.delete(MediaStore.Files.getContentUri("external"), MediaStore.MediaColumns.DATA + "=?",
                    new String[]{file.getAbsolutePath()});
        }

        return !file.exists();
    }

    /**
     * Delete all files in a folder.
     *
     * @param folder the folder
     * @return true if successful.
     */
    private static final boolean deleteFilesInFolder(final File folder, Context context) {
        boolean totalSuccess = true;
        if (folder == null)
            return false;
        if (folder.isDirectory()) {
            for (File child : folder.listFiles()) {
                deleteFilesInFolder(child, context);
            }

            if (!folder.delete())
                totalSuccess = false;
        } else {

            if (!folder.delete())
                totalSuccess = false;
        }
        return totalSuccess;
    }

    /**
     * Delete a directory asynchronously.
     *
     * @param activity    The activity calling this method.
     * @param file        The folder name.
     * @param postActions Commands to be executed after success.
     */
    public static void rmdirAsynchronously(final Activity activity, final File file, final Runnable postActions, final Context context) {
        if (file == null)
            return;
        new Thread() {
            @Override
            public void run() {
                int retryCounter = 5; // MAGIC_NUMBER
                while (!FileUtil.rmdir(file, context) && retryCounter > 0) {
                    try {
                        Thread.sleep(100); // MAGIC_NUMBER
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                    retryCounter--;
                }
                if (file.exists()) {
           /*         DialogUtil.displayError(activity, R.string.message_dialog_failed_to_delete_folder, false,
                            file.getAbsolutePath());
           */
                } else {
                    activity.runOnUiThread(postActions);
                }

            }
        }.start();
    }

    /**
     * Check if a file is readable.
     *
     * @param file The file
     * @return true if the file is reabable.
     */
    public static boolean isReadable(final File file) {
        if (file == null)
            return false;
        if (!file.exists()) return false;

        boolean result;
        try {
            result = file.canRead();
        } catch (SecurityException e) {
            return false;
        }

        return result;
    }

    /**
     * Check if a file is writable. Detects write issues on external SD card.
     *
     * @param file The file
     * @return true if the file is writable.
     */
    public static boolean isWritable(final File file) {
        if (file == null)
            return false;
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.close();
            } catch (IOException e) {
                // do nothing.
            }
        } catch (FileNotFoundException e) {
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
     * @param folder The directory
     * @return true if it is possible to write in this directory.
     */
    public static boolean isWritableNormalOrSaf(final File folder, Context c) {
        // Verify that this is a directory.
        if (folder == null)
            return false;
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }

        // Find a non-existing file in this directory.
        int i = 0;
        File file;
        do {
            String fileName = "AugendiagnoseDummyFile" + (++i);
            file = new File(folder, fileName);
        } while (file.exists());

        // First check regular writability
        if (isWritable(file)) {
            return true;
        }

        // Next check SAF writability.
        DocumentFile document = getDocumentFile(file, false, c);

        if (document == null) {
            return false;
        }

        // This should have created the file - otherwise something is wrong with access URL.
        boolean result = document.canWrite() && file.exists();

        // Ensure that the dummy file is not remaining.
        deleteFile(file, c);
        return result;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w("AmazeFileUtils", "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String[] getExtSdCardPathsForActivity(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w("AmazeFileUtils", "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     * null is returned.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file The file.
     * @return true if on external sd card.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isOnExtSdCard(final File file, Context c) {
        return getExtSdCardFolder(file, c) != null;
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
     * existing, it is created.
     *
     * @param file        The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory, Context context) {
        String baseFolder = getExtSdCardFolder(file, context);
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return null;
        }

        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath))
                relativePath = fullPath.substring(baseFolder.length() + 1);
            else originalDirectory = true;
        } catch (IOException e) {
            return null;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }
        String as = PreferenceManager.getDefaultSharedPreferences(context).getString("URI", null);

        Uri treeUri = null;
        if (as != null) treeUri = Uri.parse(as);
        if (treeUri == null) {
            return null;
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) return document;
        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
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
     * @param resource   The resource file.
     * @param folderName The folder below app folder where the file is copied to.
     * @param targetName The name of the target file.
     * @return the dummy file.
     * @throws IOException
     */
    private static File copyDummyFile(final int resource, final String folderName, final String targetName, Context context)
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
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // do nothing
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        // do nothing
                    }
                }
            }
        }
        return targetFile;
    }

    /**
     * Checks whether the target path exists or is writable
     * @param f the target path
     * @param context
     * @return 1 if exists or writable, 0 if not writable
     */
    public static int checkFolder(final String f,Context context) {
        if(f==null)return 0;
        if(f.startsWith("smb://") || f.startsWith("otg:")) return 1;
        File folder=new File(f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(folder, context)) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            // On Android 5, trigger storage access framework.
            if (FileUtil.isWritableNormalOrSaf(folder, context)) {
                return 1;

            }
        } else if (Build.VERSION.SDK_INT == 19 && FileUtil.isOnExtSdCard(folder, context)) {
            // Assume that Kitkat workaround works
            return 1;
        } else if (folder.canWrite()) {
            return 1;
        } else {
            return 0;
        }
        return 0;
    }

    /**
     * Copy the dummy image and dummy mp3 into the private folder, if not yet there. Required for the Kitkat workaround.
     *
     * @return the dummy mp3.
     */
    private static File copyDummyFiles(Context c) {
        try {
            copyDummyFile(R.mipmap.ic_launcher, "mkdirFiles", "albumart.jpg", c);
            return copyDummyFile(R.raw.temptrack, "mkdirFiles", "temptrack.mp3", c);

        } catch (IOException e) {
            Log.e("AmazeFileUtils", "Could not copy dummy files.", e);
            return null;
        }
    }

    static class MediaFile {
        private static final String NO_MEDIA = ".nomedia";
        private static final String ALBUM_ART_URI = "content://media/external/audio/albumart";
        private static final String[] ALBUM_PROJECTION = {BaseColumns._ID, MediaStore.Audio.AlbumColumns.ALBUM_ID, "media_type"};

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

        MediaFile(Context context, File file) {
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
            String[] selectionArgs = new String[]{file.getAbsolutePath()};

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

            final String[] selectionArgs = {temporaryTrack.getAbsolutePath()};
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
            String[] selectionArgs = new String[]{file.getAbsolutePath()};
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

}
