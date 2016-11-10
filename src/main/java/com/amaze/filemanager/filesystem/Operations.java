package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;

import com.amaze.filemanager.utils.Logger;
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
    }

    public static void mkdir(final HFile file,final Context context,final boolean rootMode,@NonNull final ErrorCallBack errorCallBack){
        if(file==null || errorCallBack==null)return;
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                if(file.exists())errorCallBack.exists(file);
                if(file.isSmb()){
                    try {
                        file.getSmbFile(2000).mkdirs();
                    } catch (SmbException e) {
                        Logger.log(e,file.getPath(),context);
                        errorCallBack.done(file,false);
                        return null;
                    }
                    errorCallBack.done(file,file.exists());
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
                            file.setMode(OpenMode.DRIVE);
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

                if(file.exists())errorCallBack.exists(file);
                if(file.isSmb()){
                    try {
                        file.getSmbFile(2000).createNewFile();
                    } catch (SmbException e) {
                        Logger.log(e,file.getPath(),context);
                        errorCallBack.done(file,false);
                        return null;
                    }
                    errorCallBack.done(file,file.exists());
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
                            file.setMode(OpenMode.DRIVE);
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
    public static void rename(final HFile f, final HFile f1, final boolean rootMode, final Context context, final ErrorCallBack errorCallBack){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (f.isSmb()) {
                    try {
                        SmbFile smbFile = new SmbFile(f.getPath());
                        SmbFile smbFile1=new SmbFile(f1.getPath());
                        if(smbFile1.exists()){
                            errorCallBack.exists(f1);
                            return null;
                        }
                        smbFile.renameTo(smbFile1);
                        if(!smbFile.exists() && smbFile1.exists())
                            errorCallBack.done(f1,true);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (SmbException e) {
                        e.printStackTrace();
                    }
                    return null;
                } else {
                        if(f1.exists()){
                            errorCallBack.launchSAF(f,f1);
                            return null;
                        }

                    File file = new File(f.getPath());
                    File file1 = new File(f1.getPath());
                        switch (f.getMode()){
                            case FILE:
                                int mode = checkFolder(file.getParentFile(), context);
                                if (mode == 2) {
                                    errorCallBack.launchSAF(f,f1);
                                } else if (mode == 1 || mode==0) {
                                    boolean b = FileUtil.renameFolder(file, file1, context);
                                    boolean a = !file.exists() && file1.exists();
                                    if (!a && rootMode){
                                        try {
                                            renameRoot(file, file1.getName());
                                        } catch (Exception e) {
                                            Logger.log(e,f.getPath()+"\n"+f1.getPath(),context);
                                        }
                                       f.setMode(OpenMode.DRIVE);
                                      f1.setMode(OpenMode.DRIVE);
                                      a=  !file.exists() && file1.exists();
                                    }
                                    errorCallBack.done(f1,a);
                                    return null;
                                }
                                break;
                            case DRIVE:
                                try {
                                    renameRoot(file, file1.getName());
                                } catch (Exception e) {
                                    Logger.log(e,f.getPath()+"\n"+f1.getPath(),context);
                                }
                                f.setMode(OpenMode.DRIVE);
                                f1.setMode(OpenMode.DRIVE);
                                boolean a=  !file.exists() && file1.exists();
                                errorCallBack.done(f1,a);
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
