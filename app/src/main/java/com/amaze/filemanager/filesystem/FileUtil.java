/*
 * FileUtil.java
 *
 * Copyright (C) 2015-2018 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.annotation.TargetApi;
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
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.regex.Pattern;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Utility class for helping parsing file systems.
 */
public abstract class FileUtil {

    private static final String LOG = "AmazeFileUtils";

    private static final Pattern FILENAME_REGEX = Pattern.compile("[\\\\\\/:\\*\\?\"<>\\|\\x01-\\x1F\\x7F]", Pattern.CASE_INSENSITIVE);

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
            Log.e(LOG,
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

    public static OutputStream getOutputStream(final File target, Context context) throws FileNotFoundException {
        OutputStream outStream = null;
        // First try the normal way
        if (isWritable(target)) {
            // standard way
            outStream = new FileOutputStream(target);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Storage Access Framework
                DocumentFile targetDocument = getDocumentFile(target, false, context);
                if (targetDocument == null) return null;
                outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                // Workaround for Kitkat ext SD card
                return MediaStoreHack.getOutputStream(context, target.getPath());
            }
        }
        return outStream;
    }

    /**
     * Writes uri stream from external application to the specified path
     */
    public static final void writeUriToStorage(@NonNull final MainActivity mainActivity, @NonNull final ArrayList<Uri> uris,
                                               @NonNull final ContentResolver contentResolver, @NonNull final String currentPath) {

        AppConfig.runInParallel(new AppConfig.CustomAsyncCallbacks<Void, List<String>>(null) {

            @Override
            public List<String> doInBackground() {

                List<String> retval = new ArrayList<>();

                for (Uri uri : uris) {

                    BufferedInputStream bufferedInputStream = null;
                    try {
                        bufferedInputStream = new BufferedInputStream(contentResolver.openInputStream(uri));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    BufferedOutputStream bufferedOutputStream = null;

                    try {
                        DocumentFile documentFile = DocumentFile.fromSingleUri(mainActivity, uri);
                        String filename = documentFile.getName();
                        if(filename == null) {
                            filename = uri.getLastPathSegment();

                            //For cleaning up slashes. Back in #1217 there is a case of Uri.getLastPathSegment() end up with a full file path
                            if (filename.contains("/"))
                                filename = filename.substring(filename.lastIndexOf('/') + 1);
                        }

                        String finalFilePath = currentPath + "/" + filename;
                        DataUtils dataUtils = DataUtils.getInstance();

                        HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, currentPath);
                        hFile.generateMode(mainActivity);

                        switch (hFile.getMode()) {
                            case FILE:
                            case ROOT:
                                File targetFile = new File(finalFilePath);
                                if (!FileUtil.isWritableNormalOrSaf(targetFile.getParentFile(), mainActivity.getApplicationContext())) {
                                    AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.not_allowed));
                                    return null;
                                }

                                DocumentFile targetDocumentFile = getDocumentFile(targetFile, false, mainActivity.getApplicationContext());

                                //Fallback, in case getDocumentFile() didn't properly return a DocumentFile instance
                                if(targetDocumentFile == null)
                                    targetDocumentFile = DocumentFile.fromFile(targetFile);

                                //Lazy check... and in fact, different apps may pass in URI in different formats, so we could only check filename matches
                                //FIXME?: Prompt overwrite instead of simply blocking
                                if (targetDocumentFile.exists() && targetDocumentFile.length() > 0) {
                                    AppConfig.toast(mainActivity, mainActivity.getString(R.string.cannot_overwrite));
                                    return null;
                                }

                                bufferedOutputStream = new BufferedOutputStream(contentResolver.openOutputStream(targetDocumentFile.getUri()));
                                retval.add(targetFile.getPath());
                                break;
                            case SMB:
                                SmbFile targetSmbFile = new SmbFile(finalFilePath);
                                if (targetSmbFile.exists()) {
                                    AppConfig.toast(mainActivity, mainActivity.getString(R.string.cannot_overwrite));
                                    return null;
                                } else {
                                    OutputStream outputStream = targetSmbFile.getOutputStream();
                                    bufferedOutputStream = new BufferedOutputStream(outputStream);
                                    retval.add(mainActivity.mainActivityHelper.parseSmbPath(targetSmbFile.getPath()));
                                }
                                break;
                            case SFTP:
                                //FIXME: implement support
                                AppConfig.toast(mainActivity, mainActivity.getString(R.string.not_allowed));
                                return null;
                            case DROPBOX:
                                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
                                String path = CloudUtil.stripPath(OpenMode.DROPBOX, finalFilePath);
                                cloudStorageDropbox.upload(path,
                                        bufferedInputStream, documentFile.length(), true);
                                retval.add(path);
                                break;
                            case BOX:
                                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
                                path = CloudUtil.stripPath(OpenMode.BOX, finalFilePath);
                                cloudStorageBox.upload(path,
                                        bufferedInputStream, documentFile.length(), true);
                                retval.add(path);
                                break;
                            case ONEDRIVE:
                                CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
                                path = CloudUtil.stripPath(OpenMode.ONEDRIVE, finalFilePath);
                                cloudStorageOneDrive.upload(path,
                                        bufferedInputStream, documentFile.length(), true);
                                retval.add(path);
                                break;
                            case GDRIVE:
                                CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
                                path = CloudUtil.stripPath(OpenMode.GDRIVE, finalFilePath);
                                cloudStorageGDrive.upload(path,
                                        bufferedInputStream, documentFile.length(), true);
                                retval.add(path);
                                break;
                            case OTG:
                                DocumentFile documentTargetFile = OTGUtil.getDocumentFile(finalFilePath,
                                        mainActivity, true);

                                if(documentTargetFile.exists()) {
                                    AppConfig.toast(mainActivity, mainActivity.getString(R.string.cannot_overwrite));
                                    return null;
                                }

                                bufferedOutputStream = new BufferedOutputStream(contentResolver
                                        .openOutputStream(documentTargetFile.getUri()),
                                        GenericCopyUtil.DEFAULT_BUFFER_SIZE);

                                retval.add(documentTargetFile.getUri().getPath());
                                break;
                            default:
                                return null;
                        }

                        int count = 0;
                        byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];

                        while (count != -1) {
                            count = bufferedInputStream.read(buffer);
                            if (count != -1) {

                                bufferedOutputStream.write(buffer, 0, count);
                            }
                        }
                        bufferedOutputStream.flush();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {

                        try {

                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                            if (bufferedOutputStream != null) {
                                bufferedOutputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return (retval.size() > 0) ? retval : null;
            }
            @Override
            public void onPostExecute(List<String> result) {
                if(result !=  null) {
                    List<String> paths = (List<String>) result;
                    if (paths.size() == 1) {
                        Toast.makeText(mainActivity, mainActivity.getString(R.string.saved_single_file, paths.get(0)), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(mainActivity, mainActivity.getString(R.string.saved_multi_files, paths.size()), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
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
        boolean fileDelete = rmdir(file, context);
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
                Log.e(LOG, "Error when deleting file " + file.getAbsolutePath(), e);
                return false;
            }
        }

        return !file.exists();
    }

    private static boolean rename(File f, String name, boolean root) throws ShellNotRunningException {
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
                                Context context) throws ShellNotRunningException {
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
     * @deprecated use {@link #mkdirs(Context, HybridFile)}
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

    public static boolean mkdirs(Context context, HybridFile file) {
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
                DocumentFile documentFile = OTGUtil.getDocumentFile(file.getPath(), context, true);
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

    public static boolean mkfile(final File file,Context context) {
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
                return document.createFile(MimeTypes.getMimeType(file.getPath(), file.isDirectory()), file.getName()) != null;
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
    private static boolean rmdir(@NonNull final File file, Context context) {
        if (!file.exists()) return true;

        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for(File child : files) {
                rmdir(child, context);
            }
        }

        // Try the normal way
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(file, true, context);
            if(document != null && document.delete()) {
                return true;
            }
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
                e.printStackTrace();
                // do nothing.
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
                    Log.w(LOG, "Unexpected external file dir: " + file.getAbsolutePath());
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
                    Log.w(LOG, "Unexpected external file dir: " + file.getAbsolutePath());
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

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
            return DocumentFile.fromFile(file);

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
        String as = PreferenceManager.getDefaultSharedPreferences(context).getString(PreferencesConstants.PREFERENCE_URI,
                null);

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
     * @return 1 if exists or writable, 0 if not writable
     */
    public static int checkFolder(final String f,Context context) {
        if(f==null)return 0;
        if(f.startsWith("smb://")
                || f.startsWith("ssh://")
                || f.startsWith(OTGUtil.PREFIX_OTG)
                || f.startsWith(CloudHandler.CLOUD_PREFIX_BOX)
                || f.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)
                || f.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)
                || f.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)
                )
            return 1;

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
            Log.e(LOG, "Could not copy dummy files.", e);
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
        public boolean delete() {

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


    /**
     * Validate given text is a valid filename.
     *
     * @param text
     * @return true if given text is a valid filename
     */
    public static boolean isValidFilename(String text) {
        //It's not easy to use regex to detect single/double dot while leaving valid values (filename.zip) behind...
        //So we simply use equality to check them
        return (!FILENAME_REGEX.matcher(text).find())
                && !".".equals(text) && !"..".equals(text);
    }
}
