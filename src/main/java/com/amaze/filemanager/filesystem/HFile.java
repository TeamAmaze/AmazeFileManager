package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Logger;
import com.amaze.filemanager.utils.RootUtils;
import com.stericson.RootTools.RootTools;

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
    public static final int ROOT_MODE=3,LOCAL_MODE=0,SMB_MODE=1,UNKNOWN=-1;
    int mode=0;
    public HFile(int mode, String path) {
        this.path = path;
        this.mode = mode;
    }

    public HFile(int mode,String path, String name,boolean isDirectory) {
        this.mode = mode;
        if (path.startsWith("smb://") || isSmb()){
            if(!isDirectory)this.path = path + name;
            else if(!name.endsWith("/")) this.path=path+name+"/";
            else this.path=path+name;
        }
        else this.path = path + "/" + name;
    }
    public void generateMode(Context context){
        if(path.startsWith("smb://"))mode=SMB_MODE;
        else {
            if(context==null){
                mode=LOCAL_MODE;
                return;
            }
            boolean rootmode=PreferenceManager.getDefaultSharedPreferences(context).getBoolean("rootMode",false);
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT)
            {   mode=LOCAL_MODE;
                if(rootmode){
                    if(!getFile().canRead())mode=ROOT_MODE;
                }
                return;
            }
            if(FileUtil.isOnExtSdCard(getFile(),context))mode=LOCAL_MODE;
            else if(rootmode){
                if(!getFile().canRead())mode=ROOT_MODE;
            }
            if(mode==UNKNOWN)mode=LOCAL_MODE;
        }

    }
    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isLocal(){
        return mode==LOCAL_MODE;
    }
    public boolean isRoot(){
        return mode==ROOT_MODE;
    }
    public boolean isSmb(){
        return mode==SMB_MODE;
    }
    File getFile(){return new File(path);}
    BaseFile generateBaseFileFromParent(){
        ArrayList<BaseFile> arrayList= RootHelper.getFilesList(getFile().getParent(),true,true,null);
        for(BaseFile baseFile:arrayList){
            if(baseFile.getPath().equals(path))
                return baseFile;
        }
        return null;
    }
    public long lastModified() throws MalformedURLException, SmbException {
        switch (mode){
            case SMB_MODE:
                SmbFile smbFile=getSmbFile();
                if(smbFile!=null)
                    return smbFile.lastModified();
                break;
            case LOCAL_MODE:
                new File(path).lastModified();
                break;
            case ROOT_MODE:
                BaseFile baseFile=generateBaseFileFromParent();
                if(baseFile!=null)
                return baseFile.getDate();
        }
        return new File("/").lastModified();
    }
    public long length() {
        long s = 0l;
        switch (mode){
            case SMB_MODE:
                SmbFile smbFile=getSmbFile();
                if(smbFile!=null)
                    try {
                        s = smbFile.length();
                    } catch (SmbException e) {
                    }
                    return s;
            case LOCAL_MODE:
                s = new File(path).length();
                return s;
            case ROOT_MODE:
                BaseFile baseFile=generateBaseFileFromParent();
                if(baseFile!=null)
                return baseFile.getSize();
        }
        return s;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        String name = null;
        switch (mode){
            case SMB_MODE:
                SmbFile smbFile=getSmbFile();
                if(smbFile!=null)
                    return smbFile.getName();
                break;
            case LOCAL_MODE:
                return new File(path).getName();
            case ROOT_MODE:
                return new File(path).getName();
        }
        return name;
    }
    public SmbFile getSmbFile(int timeout){
        try {
            SmbFile smbFile=new SmbFile(path);
            smbFile.setConnectTimeout(timeout);
            return smbFile;
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public SmbFile getSmbFile(){
        try {
            return new SmbFile(path);
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public boolean isCustomPath(){
        if(path.equals("0") ||
                path.equals("1") ||
                path.equals("2") ||
                path.equals("3") ||
                path.equals("5") ||
                path.equals("6") ||
                path.equals("4"))
            return true;
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
        } else if(isLocal())isDirectory = new File(path).isDirectory();
        else if(isRoot())isDirectory=RootHelper.isDirectory(path,true,5);
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

    public ArrayList<BaseFile> listFiles(boolean rootmode) {
        ArrayList<BaseFile> arrayList = new ArrayList<>();
        if (isSmb()) {
            try {
                SmbFile smbFile = new SmbFile(path);
                for (SmbFile smbFile1 : smbFile.listFiles()) {
                    BaseFile baseFile=new BaseFile(smbFile1.getPath());
                    baseFile.setName(smbFile1.getName());
                    baseFile.setMode(HFile.SMB_MODE);
                    baseFile.setDirectory(smbFile1.isDirectory());
                    baseFile.setDate(smbFile1.lastModified());
                    baseFile.setSize(baseFile.isDirectory()?0:smbFile1.length());
                    arrayList.add(baseFile);
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
            arrayList = RootHelper.getFilesList(path, rootmode, true,null);
        }
        if (arrayList == null) arrayList = new ArrayList<>();
        return arrayList;
    }
    public String getReadablePath(String path){
        if(isSmb())
            return parseSmbPath(path);
            return path;
    }
    String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
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
            try {
                inputStream = FileUtil.getOutputStream(new File(path), context, length());
            } catch (Exception e) {
                inputStream=null;
            }

        }
        return inputStream;
    }

    public boolean exists() {
        boolean exists = false;
        if (isSmb()) {
            try {
                SmbFile smbFile=getSmbFile(2000);
                exists =smbFile!=null?smbFile .exists():false;
            } catch (SmbException e) {
                exists = false;
            }
        }
        else if(isLocal())exists = new File(path).exists();
        else if(isRoot())return RootHelper.fileExists(path);
        return exists;
    }
    public boolean isSimpleFile(){
        if(!isSmb() && !isCustomPath() && !android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches()){
            if(!new File(path).isDirectory())return true;
        }
        return false;
    }
    public boolean setLastModified(long date){
        if(isSmb())
            try {
                new SmbFile(path).setLastModified(date);
                return true;
        } catch (SmbException e) {
                 return false;
        } catch (MalformedURLException e) {
                return false;
        }
        File f=new File(path);
        return f.setLastModified(date);

    }
    public void mkdir(Context context) {
        if (isSmb()) {
            try {
                new SmbFile(path).mkdirs();
            } catch (SmbException e) {
                Logger.log(e,path,context);
            } catch (MalformedURLException e) {
                Logger.log(e,path,context);
            }
        } else
            FileUtil.mkdir(new File(path), context);
    }
    public boolean delete(Context context,boolean rootmode){
        if (isSmb()) {
            try {
                new SmbFile(path).delete();
            } catch (SmbException e) {
                Logger.log(e,path,context);
            } catch (MalformedURLException e) {
                Logger.log(e,path,context);
            }
        } else {
            boolean b= FileUtil.deleteFile(new File(path), context);
            if(!b && rootmode){
                setMode(ROOT_MODE);
                RootTools.remount(getParent(),"rw");
                String s=RootHelper.runAndWait("rm -r \""+getPath()+"\"",true);
                RootTools.remount(getParent(),"ro");
            }

        }
        return !exists();
    }
}
