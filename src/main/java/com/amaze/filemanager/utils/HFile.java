package com.amaze.filemanager.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by Arpit on 07-07-2015.
 */
//Hybrid file for handeling all types of files
public class HFile {
    String path;

    public HFile(String path) {
        this.path = path;
    }

    public HFile(String path, String name,boolean isDirectory) {
        if (path.startsWith("smb://"))
            if(!isDirectory)this.path = path + name;
            else this.path=path+name+"/";
        else this.path = path + "/" + name;
    }

    public boolean isSmb() {
        return path.startsWith("smb:/");
    }
    public long lastModified() throws MalformedURLException, SmbException {
        if(isSmb())return new SmbFile(path).lastModified();
        else return new File(path).lastModified();
    }
    public long length() {
        long s = 0l;
        if (isSmb()) {
            try {
                s = new SmbFile(path).length();
            } catch (SmbException e) {
                s = 0l;
                e.printStackTrace();
            } catch (MalformedURLException e) {
                s = 0l;
                e.printStackTrace();
            }
        } else s = new File(path).length();
        return s;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        String name = "";
        if (isSmb()) {
            try {
                name = new SmbFile(path).getName();
            } catch (MalformedURLException e) {
                name = "";
                e.printStackTrace();
            }
        } else
            name = new File(path).getName();
        return name;
    }
    public SmbFile getSmbFile(){
        try {
            return new SmbFile(path);
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public boolean isCustomPath(){
        if(path.equals("0") || path.equals("1") || path.equals("2") || path.equals("3") || path
                .equals("4"))return true;
        return false;
    }
    public String getParent() {
        String name = "";
        if (isSmb()) {
            try {
                name = new SmbFile(path).getParent();
            } catch (MalformedURLException e) {
                name = "";
                e.printStackTrace();
            }
        } else
            name = new File(path).getParent();
        return name;
    }
    public boolean isDirectory() {
        boolean isDirectory = false;
        if (isSmb()) {
            try {
                isDirectory = new SmbFile(path).isDirectory();
            } catch (SmbException e) {
                isDirectory = false;
                e.printStackTrace();
            } catch (MalformedURLException e) {
                isDirectory = false;
                e.printStackTrace();
            }
        } else isDirectory = new File(path).isDirectory();
        return isDirectory;

    }

    public long folderSize() {
        long size = 0l;
        if (isSmb()) {
            try {
                size = new Futils().folderSize(new SmbFile(path));
            } catch (MalformedURLException e) {
                size = 0l;
                e.printStackTrace();
            }
        } else
            size = new Futils().folderSize(new File(path));
        return size;
    }

    public long getUsableSpace() {
        long size = 0l;
        if (isSmb()) {
            try {
                size = (new SmbFile(path).getDiskFreeSpace());
            } catch (MalformedURLException e) {
                size = 0l;
                e.printStackTrace();
            } catch (SmbException e) {
                size = 0l;
                e.printStackTrace();
            }
        } else
            size = (new File(path).getUsableSpace());
        return size;
    }

    public ArrayList<String[]> listFiles(boolean rootmode) {
        ArrayList<String[]> arrayList = new ArrayList<>();
        if (isSmb()) {
            try {
                SmbFile smbFile = new SmbFile(path);
                for (SmbFile smbFile1 : smbFile.listFiles()) {
                    arrayList.add(new String[]{smbFile1.getPath()});
                }
            } catch (MalformedURLException e) {
                if (arrayList != null) arrayList.clear();
                else arrayList = new ArrayList<>();
                e.printStackTrace();
            } catch (SmbException e) {
                if (arrayList != null) arrayList.clear();
                else arrayList = new ArrayList<>();
                e.printStackTrace();
            }
        } else {
            arrayList = RootHelper.getFilesList(path, rootmode, true, false);
        }
        if (arrayList == null) arrayList = new ArrayList<>();
        return arrayList;
    }

    public InputStream getInputStream() {
        InputStream inputStream = null;
        if (isSmb()) {
            try {
                inputStream = new SmbFile(path).getInputStream();
            } catch (IOException e) {
                inputStream = null;
                e.printStackTrace();
            }
        } else {
            try {
                inputStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                inputStream = null;
                e.printStackTrace();
            }
        }
        return inputStream;
    }

    public OutputStream getOutputStream(Context context) {
        OutputStream inputStream = null;
        if (isSmb()) {
            try {
                inputStream = new SmbFile(path).getOutputStream();
            } catch (IOException e) {
                inputStream = null;
                e.printStackTrace();
            }
        } else {
            inputStream = FileUtil.getOutputStream(new File(path), context, length());

        }
        return inputStream;
    }

    public boolean exists() {
        boolean exists = false;
        if (isSmb()) {
            try {
                exists = new SmbFile(path).exists();
            } catch (SmbException e) {
                exists = false;
                e.printStackTrace();
            } catch (MalformedURLException e) {
                exists = false;
                e.printStackTrace();
            }
        } else exists = new File(path).exists();
        return exists;
    }

    public void mkdir(Context context) {
        if (isSmb()) {
            try {
                new SmbFile(path).mkdirs();
            } catch (SmbException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else FileUtil.mkdir(new File(path), context);
    }
    public void delete(Context context){
        if(isSmb()){ try {
            new SmbFile(path).delete();
        } catch (SmbException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }}
        else{
            FileUtil.deleteFile(new File(path),context);
        }
    }
}
