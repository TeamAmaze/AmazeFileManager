package com.amaze.filemanager.services;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.Futils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.util.ArrayList;

import jcifs.smb.SmbException;

/**
 * Created by arpitkh996 on 25-01-2016.
 */
public class FileVerifier extends Thread {
    ArrayList<FileBundle> arrayList = new ArrayList<>();
    Futils utils;
    Context c;
    boolean rootmode;
    FileVerifierInterface fileVerifierInterface;
    boolean running=true;
    public FileVerifier(Context context,boolean rootmode, FileVerifierInterface fileVerifierInterface) {
        utils = new Futils();
        c = context;
        this.rootmode=rootmode;
        this.fileVerifierInterface = fileVerifierInterface;
    }

    @Override
    public void run() {
        super.run();
        while (arrayList.size() > 0 && !isInterrupted()) {
            running=true;
            if (arrayList.get(arrayList.size() - 1) != null) {
                FileBundle fileBundle = arrayList.get(arrayList.size() - 1);
                processFile(fileBundle);
                if(arrayList.contains(fileBundle))
                arrayList.remove(fileBundle);
            }
        }
        running=false;
    }

    public void add(FileBundle fileBundle) {
        arrayList.add(0, fileBundle);
        if (!isAlive()) {
            start();
        }
    }

    public boolean isRunning() {
        return running;
    }
    void stopTask(){
        arrayList.clear();
        interrupt();
    }
    public interface FileVerifierInterface {
        void addFailedFile(HFile a);

        boolean contains(String a);
        boolean containsDirectory(String a);
        void setCopySuccessful(boolean b);
    }

    void processFile(FileBundle fileBundle) {
        HFile sourceFile = fileBundle.getFile(), targetFile = fileBundle.getFile2();
        boolean move = fileBundle.isMove();
        if(sourceFile.isDirectory()){
            if(move){
                if(!fileVerifierInterface.containsDirectory(sourceFile.getPath())){
                    sourceFile.delete(c,rootmode);
                }
            }
            return;
        }
        if (!targetFile.isSmb())
            utils.scanFile(targetFile.getPath(), c);
        if (!checkNonRootFiles(sourceFile, targetFile)) {
            fileVerifierInterface.addFailedFile(sourceFile);
            fileVerifierInterface.setCopySuccessful(false);
        }
        try {
            targetFile.setLastModified(sourceFile.lastModified());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
        if (move) {
            if (!fileVerifierInterface.contains(sourceFile.getPath())) {
                sourceFile.delete(c,rootmode);
                if (sourceFile.isLocal())
                    delete(c, sourceFile.getPath());
            }
        }
    }

    void delete(final Context context, final String file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{
                file
        };
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        // Delete the entry from the media database. This will actually delete media files.
        contentResolver.delete(filesUri, where, selectionArgs);

    }

    boolean checkNonRootFiles(HFile hFile1, HFile hFile2) {
        long l1 = hFile1.length(), l2 = hFile2.length();
        if (hFile2.exists() && ((l1 != -1 && l2 != -1) ? l1 == l2 : true)) {
            //after basic checks try checksum if possible
            InputStream inputStream = hFile1.getInputStream();
            InputStream inputStream1 = hFile2.getInputStream();
            if (inputStream == null || inputStream1 == null) return true;
            String md5, md5_1;
            try {
                md5 = getMD5Checksum(inputStream);
                md5_1 = getMD5Checksum(inputStream1);
                if (md5 != null && md5_1 != null && md5.length() > 0 && md5_1.length() > 0) {
                    if (md5.equals(md5_1)) return true;
                    else return false;
                } else return true;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    public String getMD5Checksum(InputStream filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public byte[] createChecksum(InputStream fis) throws Exception {
        byte[] buffer = new byte[8192];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }
}
