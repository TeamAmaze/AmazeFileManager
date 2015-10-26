package com.amaze.filemanager.utils;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.Toast;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.ui.icons.Icons;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Property;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Arpit on 10/24/2015.
 */
public class DriveUtil {
    ArrayList<File> arrayList;
    public DriveUtil(){
        arrayList=new ArrayList<>();
    }
    private static final String mFolderFields = "items(downloadUrl,fileSize,thumbnailLink,id,mimeType,modifiedDate,parents(id,isRoot),thumbnail/image,title),nextPageToken";

    public ArrayList<File> listFolder(com.google.api.services.drive.Drive mService, String mFolderId) throws IOException,UserRecoverableAuthIOException {
        ArrayList<com.google.api.services.drive.model.File> files = new ArrayList<>();
        String pg = "";
        for (; ; ) {
            Drive.Files.List lst = mService
                    .files()
                    .list()
                    .setQ("'" + mFolderId + "' in parents")
                    .setFields(mFolderFields)
                    .setMaxResults(100);
            if (!pg.equals(""))
                lst.setPageToken(pg);
            final FileList fl = lst.execute();

            if (fl.size() == 0)
                break;
            for (com.google.api.services.drive.model.File file : fl.getItems()) {
                if (files.contains(file))
                    continue;
                else  {
                    files.add(file);
                }
            }
            if (pg.equals(fl.getNextPageToken()))
                break;
            pg = fl.getNextPageToken();
            if (pg == null || pg.equals(""))
                break;
        }
        return files;
    }
    public interface FileReturn{
         void getFile(File f);
    };
    public void getFile(final String id, final com.google.api.services.drive.Drive mService, final FileReturn fileReturn) throws UserRecoverableAuthIOException{
        new AsyncTask<Void,File,Void>() {
            @Override
            public void onProgressUpdate(File... f) {
                if (f != null)
                    fileReturn.getFile(f[0]);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    publishProgress(mService.files().get(id).execute());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }   .execute();

    }
    public   ArrayList<File> listRoot(com.google.api.services.drive.Drive mService) throws IOException,UserRecoverableAuthIOException {
        ArrayList<com.google.api.services.drive.model.File> files = new ArrayList<>();
        String pg = "";
        for (; ; ) {
            Drive.Files.List lst = mService
                    .files()
                    .list()
                    .setFields(mFolderFields)
                    .setMaxResults(100);
            if (!pg.equals(""))
                lst.setPageToken(pg);
            final FileList fl = lst.execute();

            if (fl.size() == 0)
                break;
            for (com.google.api.services.drive.model.File file : fl.getItems()) {
                if (files.contains(file))
                    continue;
                else if (isRoot(file)) {
                    files.add(file);
                }
            }
            if (pg.equals(fl.getNextPageToken()))
                break;
            pg = fl.getNextPageToken();
            if (pg == null || pg.equals(""))
                break;
        }
        return files;
        // Get a list of up to 10 files.

    }
    public   File getParent(String folderid, Drive mSee) throws IOException ,UserRecoverableAuthIOException{
        File f = mSee.files().get(folderid).execute();
        String id = null;
        for (ParentReference p : f.getParents()) {
            if (p.getId() != null) id = p.getId();
        }if(id==null)return null;
        return mSee.files().get(id).execute();
    }


    public   boolean isRoot(File folderId) {
        for (ParentReference p : folderId.getParents()) {
            if (p.getIsRoot())
                return true;
        }
        return false;
    }
    public interface GoBackCallback{
        void load(String id);
    }
    public   void goBack(final String id, final GoBackCallback goBackCallback, final Drive mService){
        new AsyncTask<Void,String,Void>(){
            @Override
            public void onProgressUpdate(String... f){
                if(f==null)goBackCallback.load(null);
                else
                goBackCallback.load(f[0]);
            }
            @Override
            protected Void doInBackground(Void... voids) {
                if(mService==null ||id==null) {
                    publishProgress(null);
                    return null;
                }
                if(android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches())
                    publishProgress(id);
                try {
                    File f=getParent(id, mService);
                    publishProgress(f != null ? f.getId() : null);

                } catch (Exception e) {
                    e.printStackTrace();
                    publishProgress(null);
                }    return null;
            }
        } .execute();
    }


}
