package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.utils.Logger;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by arpitkh996 on 13-01-2016, modified by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class Operations {

    // reserved characters by OS, shall not be allowed in file names
    private static final String FOREWARD_SLASH = "/";
    private static final String BACKWARD_SLASH = "\\";
    private static final String COLON = ":";
    private static final String ASTERISK = "*";
    private static final String QUESTION_MARK = "?";
    private static final String QUOTE = "\"";
    private static final String GREATER_THAN = ">";
    private static final String LESS_THAN = "<";

    private static final String FAT = "FAT";

    public interface ErrorCallBack {

        /**
         * Callback fired when file being created in process already exists
         *
         * @param file
         */
        void exists(HFile file);

        /**
         * Callback fired when creating new file/directory and required storage access framework permission
         * to access SD Card is not available
         *
         * @param file
         */
        void launchSAF(HFile file);

        /**
         * Callback fired when renaming file and required storage access framework permission to access
         * SD Card is not available
         *
         * @param file
         * @param file1
         */
        void launchSAF(HFile file, HFile file1);

        /**
         * Callback fired when we're done processing the operation
         *
         * @param hFile
         * @param b     defines whether operation was successful
         */
        void done(HFile hFile, boolean b);

        /**
         * Callback fired when an invalid file name is found.
         *
         * @param file
         */
        void invalidName(HFile file);
    }

    public static void mkdir(@NonNull final HFile file, final Context context, final boolean rootMode,
                             @NonNull final ErrorCallBack errorCallBack) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // checking whether filename is valid or a recursive call possible
                if (MainActivityHelper.isNewDirectoryRecursive(file) ||
                        !Operations.isFileNameValid(file.getName())) {
                    errorCallBack.invalidName(file);
                    return null;
                }

                if (file.exists()) {
                    errorCallBack.exists(file);
                    return null;
                }
                if (file.isSmb()) {
                    try {
                        file.getSmbFile(2000).mkdirs();
                    } catch (SmbException e) {
                        Logger.log(e, file.getPath(), context);
                        errorCallBack.done(file, false);
                        return null;
                    }
                    errorCallBack.done(file, file.exists());
                    return null;
                } else if (file.isOtgFile()) {

                    // first check whether new directory already exists
                    DocumentFile directoryToCreate = RootHelper.getDocumentFile(file.getPath(), context, false);
                    if (directoryToCreate != null) errorCallBack.exists(file);

                    DocumentFile parentDirectory = RootHelper.getDocumentFile(file.getParent(), context, false);
                    if (parentDirectory.isDirectory()) {
                        parentDirectory.createDirectory(file.getName());
                        errorCallBack.done(file, true);
                    } else errorCallBack.done(file, false);
                    return null;
                } else {
                    if (file.isLocal() || file.isRoot()) {
                        int mode = checkFolder(new File(file.getParent()), context);
                        if (mode == 2) {
                            errorCallBack.launchSAF(file);
                            return null;
                        }
                        if (mode == 1 || mode == 0)
                            FileUtil.mkdir(file.getFile(), context);
                        if (!file.exists() && rootMode) {
                            file.setMode(OpenMode.ROOT);
                            if (file.exists()) errorCallBack.exists(file);
                            try {

                                RootUtils.mkDir(file.getParent(), file.getName());
                            } catch (RootNotPermittedException e) {
                                Logger.log(e, file.getPath(), context);
                            }
                            errorCallBack.done(file, file.exists());
                            return null;
                        }
                        errorCallBack.done(file, file.exists());
                        return null;
                    }

                    errorCallBack.done(file, file.exists());
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public static void mkfile(@NonNull final HFile file, final Context context, final boolean rootMode,
                              @NonNull final ErrorCallBack errorCallBack) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // check whether filename is valid or not
                if (!Operations.isFileNameValid(file.getName())) {
                    errorCallBack.invalidName(file);
                    return null;
                }

                if (file.exists()) {
                    errorCallBack.exists(file);
                    return null;
                }
                if (file.isSmb()) {
                    try {
                        file.getSmbFile(2000).createNewFile();
                    } catch (SmbException e) {
                        Logger.log(e, file.getPath(), context);
                        errorCallBack.done(file, false);
                        return null;
                    }
                    errorCallBack.done(file, file.exists());
                    return null;
                } else if (file.isOtgFile()) {

                    // first check whether new file already exists
                    DocumentFile fileToCreate = RootHelper.getDocumentFile(file.getPath(), context, false);
                    if (fileToCreate != null) errorCallBack.exists(file);

                    DocumentFile parentDirectory = RootHelper.getDocumentFile(file.getParent(), context, false);
                    if (parentDirectory.isDirectory()) {
                        parentDirectory.createFile(file.getName().substring(file.getName().lastIndexOf(".")),
                                file.getName());
                        errorCallBack.done(file, true);
                    } else errorCallBack.done(file, false);
                    return null;
                } else {
                    if (file.isLocal() || file.isRoot()) {
                        int mode = checkFolder(new File(file.getParent()), context);
                        if (mode == 2) {
                            errorCallBack.launchSAF(file);
                            return null;
                        }
                        if (mode == 1 || mode == 0)
                            try {
                                FileUtil.mkfile(file.getFile(), context);
                            } catch (IOException e) {
                            }
                        if (!file.exists() && rootMode) {
                            file.setMode(OpenMode.ROOT);
                            if (file.exists()) errorCallBack.exists(file);
                            try {

                                RootUtils.mkFile(file.getPath());
                            } catch (RootNotPermittedException e) {
                                Logger.log(e, file.getPath(), context);
                            }
                            errorCallBack.done(file, file.exists());
                            return null;
                        }
                        errorCallBack.done(file, file.exists());
                        return null;
                    }
                    errorCallBack.done(file, file.exists());


                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void rename(final HFile oldFile, final HFile newFile, final boolean rootMode,
                              final Context context, final ErrorCallBack errorCallBack) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // check whether file names for new file are valid or recursion occurs
                if (MainActivityHelper.isNewDirectoryRecursive(newFile) ||
                        !Operations.isFileNameValid(newFile.getName())) {
                    errorCallBack.invalidName(newFile);
                    return null;
                }

                if (newFile.exists()) {
                    errorCallBack.exists(newFile);
                    return null;
                }

                if (oldFile.isSmb()) {
                    try {
                        SmbFile smbFile = new SmbFile(oldFile.getPath());
                        SmbFile smbFile1 = new SmbFile(newFile.getPath());
                        if (smbFile1.exists()) {
                            errorCallBack.exists(newFile);
                            return null;
                        }
                        smbFile.renameTo(smbFile1);
                        if (!smbFile.exists() && smbFile1.exists())
                            errorCallBack.done(newFile, true);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (SmbException e) {
                        e.printStackTrace();
                    }
                    return null;
                } else if (oldFile.isOtgFile()) {
                    DocumentFile oldDocumentFile = RootHelper.getDocumentFile(oldFile.getPath(), context, false);
                    DocumentFile newDocumentFile = RootHelper.getDocumentFile(newFile.getPath(), context, false);
                    if (newDocumentFile != null) {
                        errorCallBack.exists(newFile);
                        return null;
                    }
                    errorCallBack.done(newFile, oldDocumentFile.renameTo(newFile.getName()));
                    return null;
                } else {

                    File file = new File(oldFile.getPath());
                    File file1 = new File(newFile.getPath());
                    switch (oldFile.getMode()) {
                        case FILE:
                            int mode = checkFolder(file.getParentFile(), context);
                            if (mode == 2) {
                                errorCallBack.launchSAF(oldFile, newFile);
                            } else if (mode == 1 || mode == 0) {
                                try {
                                    FileUtil.renameFolder(file, file1, context);
                                } catch (RootNotPermittedException e) {
                                    e.printStackTrace();
                                }
                                boolean a = !file.exists() && file1.exists();
                                if (!a && rootMode) {
                                    try {
                                        RootUtils.rename(file.getPath(), file1.getPath());
                                    } catch (Exception e) {
                                        Logger.log(e, oldFile.getPath() + "\n" + newFile.getPath(), context);
                                    }
                                    oldFile.setMode(OpenMode.ROOT);
                                    newFile.setMode(OpenMode.ROOT);
                                    a = !file.exists() && file1.exists();
                                }
                                errorCallBack.done(newFile, a);
                                return null;
                            }
                            break;
                        case ROOT:
                            try {

                                RootUtils.rename(file.getPath(), file1.getPath());
                            } catch (Exception e) {
                                Logger.log(e, oldFile.getPath() + "\n" + newFile.getPath(), context);
                            }

                            newFile.setMode(OpenMode.ROOT);
                            errorCallBack.done(newFile, true);
                            break;

                    }
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private static int checkFolder(final File folder, Context context) {
        boolean lol = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        if (lol) {

            boolean ext = FileUtil.isOnExtSdCard(folder, context);
            if (ext) {

                if (!folder.exists() || !folder.isDirectory()) {
                    return 0;
                }

                // On Android 5, trigger storage access framework.
                if (!FileUtil.isWritableNormalOrSaf(folder, context)) {
                    return 2;
                }
                return 1;
            }
        } else if (Build.VERSION.SDK_INT == 19) {
            // Assume that Kitkat workaround works
            if (FileUtil.isOnExtSdCard(folder, context)) return 1;

        }

        // file not on external sd card
        if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Well, we wouldn't want to copy when the target is inside the source
     * otherwise it'll end into a loop
     *
     * @param sourceFile
     * @param targetFile
     * @return true when copy loop is possible
     */
    public static boolean isCopyLoopPossible(BaseFile sourceFile, HFile targetFile) {
        return targetFile.getPath().contains(sourceFile.getPath());
    }

    /**
     * Validates file name
     * special reserved characters shall not be allowed in the file names on FAT filesystems
     *
     * @param fileName the filename, not the full path!
     * @return boolean if the file name is valid or invalid
     */
    public static boolean isFileNameValid(String fileName) {
        //String fileName = builder.substring(builder.lastIndexOf("/")+1, builder.length());

        // TODO: check file name validation only for FAT filesystems
        return !(fileName.contains(ASTERISK) || fileName.contains(BACKWARD_SLASH) ||
                fileName.contains(COLON) || fileName.contains(FOREWARD_SLASH) ||
                fileName.contains(GREATER_THAN) || fileName.contains(LESS_THAN) ||
                fileName.contains(QUESTION_MARK) || fileName.contains(QUOTE));
    }

    private static boolean isFileSystemFAT(String mountPoint) {
        String[] args = new String[]{"/bin/bash", "-c", "df -DO_NOT_REPLACE | awk '{print $1,$2,$NF}' | grep \"^"
                + mountPoint + "\""};
        try {
            Process proc = new ProcessBuilder(args).start();
            OutputStream outputStream = proc.getOutputStream();
            String buffer = null;
            outputStream.write(buffer.getBytes());
            return buffer != null && buffer.contains(FAT);
        } catch (IOException e) {
            e.printStackTrace();
            // process interrupted, returning true, as a word of cation
            return true;
        }
    }
}
