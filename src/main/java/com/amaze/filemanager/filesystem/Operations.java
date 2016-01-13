package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.amaze.filemanager.utils.Logger;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;

import jcifs.smb.SmbException;

/**
 * Created by arpitkh996 on 13-01-2016.
 */
public class Operations {
    public interface ErrorCallBack{
        void exists(HFile file);
        void launchSAF(HFile file);
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
                }
                else {
                    if (file.isLocal() || file.isRoot()) {
                        int mode = checkFolder(new File(file.getParent()), context);
                        if (mode == 2) {
                            errorCallBack.launchSAF(file);
                            return null;
                        }
                        if (mode == 1 || mode == 0)
                            FileUtil.mkdir(file.getFile(), context);
                        if (!file.exists() && rootMode) {
                            file.setMode(HFile.ROOT_MODE);
                            if (file.exists()) errorCallBack.exists(file);
                            boolean remount = false;
                            try {
                                String res;
                                if (!("rw".equals(res = RootTools.getMountedAs(file.getParent()))))
                                    remount = true;
                                if (remount)
                                    RootTools.remount(file.getParent(), "rw");
                                RootHelper.runAndWait("mkdir " + file.getPath(), true);
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
                }
                else {
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
                            file.setMode(HFile.ROOT_MODE);
                            if (file.exists()) errorCallBack.exists(file);
                            boolean remount = false;
                            try {
                                String res;
                                if (!("rw".equals(res = RootTools.getMountedAs(file.getParent()))))
                                    remount = true;
                                if (remount)
                                    RootTools.remount(file.getParent(), "rw");
                                RootHelper.runAndWait("touch " + file.getPath(), true);
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
}
