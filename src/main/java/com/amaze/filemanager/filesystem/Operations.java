package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import com.amaze.filemanager.utils.Logger;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by arpitkh996 on 13-01-2016.
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

    public interface ErrorCallBack{
        void exists(HFile file);
        void launchSAF(HFile file);
        void launchSAF(HFile file,HFile file1);
        void done(HFile hFile,boolean b);
        void invalidName(HFile file);
    }

    public static void mkdir(final HFile file,final Context context,final boolean rootMode,@NonNull final ErrorCallBack errorCallBack){
        if(file==null || errorCallBack==null)return;
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                // checking whether filename is valid or a recursive call possible
                if (MainActivityHelper.isNewDirectoryRecursive(file) ||
                        !Operations.isFileNameValid(file.getName())) {
                    errorCallBack.invalidName(file);
                    return null;
                }

                if(file.exists()) {
                    errorCallBack.exists(file);
                    return null;
                }
                if(file.isSmb()){
                    try {
                        file.getSmbFile(2000).mkdirs();
                    } catch (SmbException e) {
                        Logger.log(e,file.getPath(),context);
                        errorCallBack.done(file,false);
                        return null;
                    }
                    errorCallBack.done(file,file.exists());
                    return null;
                } else if (file.isOtgFile()) {

                    // first check whether new directory already exists
                    DocumentFile directoryToCreate = RootHelper.getDocumentFile(file.getPath(), context);
                    if (directoryToCreate.exists()) errorCallBack.exists(file);

                    DocumentFile parentDirectory = RootHelper.getDocumentFile(file.getParent(), context);
                    if (parentDirectory.isDirectory())  {
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
                            boolean remount = false;
                            try {
                                String res;
                                if (!("rw".equals(res = RootTools.getMountedAs(file.getParent()))))
                                    remount = true;
                                if (remount)
                                    RootTools.remount(file.getParent(), "rw");
                                RootHelper.runAndWait("mkdir \"" + file.getPath()+"\"", true);
                                if (remount) {
                                    if (res == null || res.length() == 0) res = "ro";
                                    RootTools.remount(file.getParent(), res);
                                }
                            } catch (Exception e) {
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
    public static void mkfile(final HFile file,final Context context,final boolean rootMode,@NonNull final ErrorCallBack errorCallBack){
        if(file==null || errorCallBack==null)return;
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                // check whether filename is valid or not
                if (!Operations.isFileNameValid(file.getName())) {
                    errorCallBack.invalidName(file);
                    return null;
                }

                if(file.exists()) {
                    errorCallBack.exists(file);
                    return null;
                }
                if(file.isSmb()){
                    try {
                        file.getSmbFile(2000).createNewFile();
                    } catch (SmbException e) {
                        Logger.log(e,file.getPath(),context);
                        errorCallBack.done(file,false);
                        return null;
                    }
                    errorCallBack.done(file,file.exists());
                    return null;
                } else if (file.isOtgFile()) {

                    // first check whether new file already exists
                    DocumentFile fileToCreate = RootHelper.getDocumentFile(file.getPath(), context);
                    if (fileToCreate.exists()) errorCallBack.exists(file);

                    DocumentFile parentDirectory = RootHelper.getDocumentFile(file.getParent(), context);
                    if (parentDirectory.isDirectory())  {
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
                            boolean remount = false;
                            try {
                                String res;
                                if (!("rw".equals(res = RootTools.getMountedAs(file.getParent()))))
                                    remount = true;
                                if (remount)
                                    RootTools.remount(file.getParent(), "rw");
                                RootHelper.runAndWait("touch \"" + file.getPath()+"\"", true);
                                if (remount) {
                                    if (res == null || res.length() == 0) res = "ro";
                                    RootTools.remount(file.getParent(), res);
                                }
                            } catch (Exception e) {
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
                              final Context context, final ErrorCallBack errorCallBack){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                // check whether file names for new file are valid or recursion occurs
                if (MainActivityHelper.isNewDirectoryRecursive(newFile) ||
                        !Operations.isFileNameValid(newFile.getName())) {
                    errorCallBack.invalidName(newFile);
                    return null;
                }

                if (oldFile.isSmb()) {
                    try {
                        SmbFile smbFile = new SmbFile(oldFile.getPath());
                        SmbFile smbFile1=new SmbFile(newFile.getPath());
                        if(smbFile1.exists()){
                            errorCallBack.exists(newFile);
                            return null;
                        }
                        smbFile.renameTo(smbFile1);
                        if(!smbFile.exists() && smbFile1.exists())
                            errorCallBack.done(newFile,true);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (SmbException e) {
                        e.printStackTrace();
                    }
                    return null;
                } else if (oldFile.isOtgFile()) {
                    DocumentFile oldDocumentFile = RootHelper.getDocumentFile(oldFile.getPath(), context);
                    DocumentFile newDocumentFile = RootHelper.getDocumentFile(newFile.getPath(), context);
                    if (newDocumentFile.exists()) {
                        errorCallBack.exists(newFile);
                        return null;
                    }
                    errorCallBack.done(newFile, oldDocumentFile.renameTo(newFile.getName()));
                    return null;
                } else {
                    if(newFile.exists()){
                        errorCallBack.exists(newFile);
                        return null;
                    }

                    File file = new File(oldFile.getPath());
                    File file1 = new File(newFile.getPath());
                    switch (oldFile.getMode()){
                        case FILE:
                            int mode = checkFolder(file.getParentFile(), context);
                            if (mode == 2) {
                                errorCallBack.launchSAF(oldFile,newFile);
                            } else if (mode == 1 || mode==0) {
                                boolean b = FileUtil.renameFolder(file, file1, context);
                                boolean a = !file.exists() && file1.exists();
                                if (!a && rootMode){
                                    try {
                                        renameRoot(file, file1.getName());
                                    } catch (Exception e) {
                                        Logger.log(e,oldFile.getPath()+"\n"+newFile.getPath(),context);
                                    }
                                    oldFile.setMode(OpenMode.ROOT);
                                    newFile.setMode(OpenMode.ROOT);
                                    a=  !file.exists() && file1.exists();
                                }
                                errorCallBack.done(newFile,a);
                                return null;
                            }
                            break;
                        case ROOT:
                            try {
                                renameRoot(file, file1.getName());
                            } catch (Exception e) {
                                Logger.log(e,oldFile.getPath()+"\n"+newFile.getPath(),context);
                            }
                            oldFile.setMode(OpenMode.ROOT);
                            newFile.setMode(OpenMode.ROOT);
                            boolean a=  !file.exists() && file1.exists();
                            errorCallBack.done(newFile,a);
                            break;

                    }
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }
    static void renameRoot(File a,String v) throws Exception {
        boolean remount=false;
        String newname = a.getParent() + "/" + v;
        String res;
        if (!("rw".equals(res = RootTools.getMountedAs(a.getParent()))))
            remount = true;
        if (remount)
            RootTools.remount(a.getParent(), "rw");
        RootHelper.runAndWait("mv \"" + a.getPath()+ "\" \"" +newname+"\"" , true);
        if (remount) {
            if (res == null || res.length() == 0) res = "ro";
            RootTools.remount(a.getParent(), res);
        }
    }
    public static int checkFolder(final File folder, Context context) {
        boolean lol= Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,ext=FileUtil.isOnExtSdCard(folder, context);
        if (lol && ext) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            // On Android 5, trigger storage access framework.
            if (!FileUtil.isWritableNormalOrSaf(folder, context)) {
                return 2;
            }
            return 1;
        } else if (Build.VERSION.SDK_INT == 19 && FileUtil.isOnExtSdCard(folder, context)) {
            // Assume that Kitkat workaround works
            return 1;
        } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Well, we wouldn't want to copy when the target is inside the source
     * otherwise it'll end into a loop
     * @param sourceFile
     * @param targetFile
     * @return true when copy loop is possible
     */
    public static boolean isCopyLoopPossible(BaseFile sourceFile, HFile targetFile) {
        if (targetFile.getPath().contains(sourceFile.getPath())) return true;
        else return false;
    }

    /**
     * Validates file name
     * special reserved characters shall not be allowed in the file names on FAT filesystems
     * @param fileName the filename, not the full path!
     * @return boolean if the file name is valid or invalid
     */
    public static boolean isFileNameValid(String fileName) {

        //String fileName = builder.substring(builder.lastIndexOf("/")+1, builder.length());


        // TODO: check file name validation only for FAT filesystems
        if ((fileName.contains(ASTERISK) || fileName.contains(BACKWARD_SLASH) ||
                fileName.contains(COLON) || fileName.contains(FOREWARD_SLASH) ||
                fileName.contains(GREATER_THAN) || fileName.contains(LESS_THAN) ||
                fileName.contains(QUESTION_MARK) || fileName.contains(QUOTE))) {
            return false;
        } else return true;
    }

    private static boolean isFileSystemFAT(String mountPoint) {
        String[] args = new String[] {"/bin/bash", "-c", "df -T | awk '{print $1,$2,$NF}' | grep \"^"
                + mountPoint + "\""};
        try {
            Process proc = new ProcessBuilder(args).start();
            OutputStream outputStream = proc.getOutputStream();
            String buffer = null;
            outputStream.write(buffer.getBytes());
            if (buffer!=null && buffer.contains(FAT)) return true;
            else return false;
        } catch (IOException e) {
            e.printStackTrace();
            // process interrupted, returning true, as a word of cation
            return true;
        }
    }
}
