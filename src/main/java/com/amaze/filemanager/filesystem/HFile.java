package com.amaze.filemanager.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;

import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Logger;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;

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
    //public static final int ROOT_MODE=3,LOCAL_MODE=0,SMB_MODE=1,UNKNOWN=-1;
    OpenMode mode = OpenMode.FILE;

    public HFile(OpenMode mode, String path) {
        this.path = path;
        this.mode = mode;
    }

    public HFile(OpenMode mode, String path, String name, boolean isDirectory) {
        this.mode = mode;
        if (path.startsWith("smb://") || isSmb()) {
            if (!isDirectory) this.path = path + name;
            else if (!name.endsWith("/")) this.path = path + name + "/";
            else this.path = path + name;
        } else this.path = path + "/" + name;
    }

    public void generateMode(Context context) {
        if (path.startsWith("smb://")) {
            mode = OpenMode.SMB;
        } else if (path.startsWith("otg:/")) {
            mode = OpenMode.OTG;
        } else if (isCustomPath()) {
            mode = OpenMode.CUSTOM;
        } else {
            if (context == null) {
                mode = OpenMode.FILE;
                return;
            }
            boolean rootmode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("rootMode", false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mode = OpenMode.FILE;
                if (rootmode) {
                    if (!getFile().canRead()) mode = OpenMode.ROOT;
                }
                return;
            }
            if (FileUtil.isOnExtSdCard(getFile(), context)) mode = OpenMode.FILE;
            else if (rootmode) {
                if (!getFile().canRead()) mode = OpenMode.ROOT;
            }
            if (mode == OpenMode.UNKNOWN) mode = OpenMode.FILE;
        }

    }

    public void setMode(OpenMode mode) {
        this.mode = mode;
    }

    public OpenMode getMode() {
        return mode;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isLocal() {
        return mode == OpenMode.FILE;
    }

    public boolean isRoot() {
        return mode == OpenMode.ROOT;
    }

    public boolean isSmb() {
        return mode == OpenMode.SMB;
    }

    public boolean isOtgFile() {
        return mode == OpenMode.OTG;
    }

    File getFile() {
        return new File(path);
    }

    BaseFile generateBaseFileFromParent() {
        ArrayList<BaseFile> arrayList = null;
        try {
            arrayList = RootHelper.getFilesList(getFile().getParent(), true, true, null);
        } catch (RootNotPermittedException e) {
            e.printStackTrace();
            return null;
        }
        for (BaseFile baseFile : arrayList) {
            if (baseFile.getPath().equals(path))
                return baseFile;
        }
        return null;
    }

    public long lastModified() throws MalformedURLException, SmbException {
        switch (mode) {
            case SMB:
                SmbFile smbFile = getSmbFile();
                if (smbFile != null)
                    return smbFile.lastModified();
                break;
            case FILE:
                new File(path).lastModified();
                break;
            case ROOT:
                BaseFile baseFile = generateBaseFileFromParent();
                if (baseFile != null)
                    return baseFile.getDate();
        }
        return new File("/").lastModified();
    }

    /**
     * @deprecated use {@link #length(Context)} to handle content resolvers
     * @return
     */
    public long length() {
        long s = 0L;
        switch (mode) {
            case SMB:
                SmbFile smbFile = getSmbFile();
                if (smbFile != null)
                    try {
                        s = smbFile.length();
                    } catch (SmbException e) {
                    }
                return s;
            case FILE:
                s = new File(path).length();
                return s;
            case ROOT:
                BaseFile baseFile = generateBaseFileFromParent();
                if (baseFile != null) return baseFile.getSize();
                break;
        }
        return s;
    }

    /**
     * Helper method to find length
     * @param context
     * @return
     */
    public long length(Context context) {

        long s = 0l;
        switch (mode){
            case SMB:
                SmbFile smbFile=getSmbFile();
                if(smbFile!=null)
                    try {
                        s = smbFile.length();
                    } catch (SmbException e) {
                    }
                return s;
            case FILE:
                s = new File(path).length();
                return s;
            case ROOT:
                BaseFile baseFile=generateBaseFileFromParent();
                if(baseFile!=null) return baseFile.getSize();
                break;
            case OTG:
                s = RootHelper.getDocumentFile(path, context, false).length();
                break;
            default:
                break;
        }
        return s;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        String name = null;
        switch (mode) {
            case SMB:
                SmbFile smbFile = getSmbFile();
                if (smbFile != null)
                    return smbFile.getName();
                break;
            case FILE:
                return new File(path).getName();
            case ROOT:
                return new File(path).getName();
            default:
                StringBuilder builder = new StringBuilder(path);
                name = builder.substring(builder.lastIndexOf("/") + 1, builder.length());
        }
        return name;
    }

    public String getName(Context context) {
        String name = null;
        switch (mode){
            case SMB:
                SmbFile smbFile=getSmbFile();
                if(smbFile!=null)
                    return smbFile.getName();
                break;
            case FILE:
                return new File(path).getName();
            case ROOT:
                return new File(path).getName();
            case OTG:
                return RootHelper.getDocumentFile(path, context, false).getName();
            default:
                StringBuilder builder = new StringBuilder(path);
                name = builder.substring(builder.lastIndexOf("/")+1, builder.length());
        }
        return name;
    }

    public SmbFile getSmbFile(int timeout) {
        try {
            SmbFile smbFile = new SmbFile(path);
            smbFile.setConnectTimeout(timeout);
            return smbFile;
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public SmbFile getSmbFile() {
        try {
            return new SmbFile(path);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public boolean isCustomPath() {
        return path.equals("0") ||
                path.equals("1") ||
                path.equals("2") ||
                path.equals("3") ||
                path.equals("4") ||
                path.equals("5") ||
                path.equals("6");
    }

    /**
     * Returns a path to parent for various {@link #mode}
     * @deprecated use {@link #getParent(Context)} to handle content resolvers
     *
     * @return
     */
    public String getParent() {
        String parentPath = "";
        switch (mode) {
            case SMB:
                try {
                    parentPath = new SmbFile(path).getParent();
                } catch (MalformedURLException e) {
                    parentPath = "";
                    e.printStackTrace();
                }
                break;
            case FILE:
            case ROOT:
                parentPath = new File(path).getParent();
                break;
            default:
                StringBuilder builder = new StringBuilder(path);
                return builder.substring(0, builder.length() - (getName().length() + 1));
        }
        return parentPath;
    }

    /**
     * Helper method to get parent path
     *
     * @param context
     * @return
     */
    public String getParent(Context context) {

        String parentPath = "";
        switch (mode) {
            case SMB:
                try {
                    parentPath = new SmbFile(path).getParent();
                } catch (MalformedURLException e) {
                    parentPath = "";
                    e.printStackTrace();
                }
                break;
            case FILE:
            case ROOT:
                parentPath = new File(path).getParent();
                break;
            case OTG:
                DocumentFile documentSourceFile = RootHelper.getDocumentFile(path,
                        context, false);
                parentPath =  documentSourceFile.getParentFile().getName();
                break;
            default:
                StringBuilder builder = new StringBuilder(path);
                StringBuilder parentPathBuilder = new StringBuilder(builder.substring(0,
                        builder.length()-(getName().length()+1)));
                return parentPathBuilder.toString();
        }
        return parentPath;
    }

    public String getParentName() {
        StringBuilder builder = new StringBuilder(path);
        StringBuilder parentPath = new StringBuilder(builder.substring(0,
                builder.length() - (getName().length() + 1)));
        String parentName = parentPath.substring(parentPath.lastIndexOf("/") + 1,
                parentPath.length());
        return parentName;
    }

    /**
     * Whether this object refers to a directory or file, handles all types of files
     * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
     *
     * @return
     */
    public boolean isDirectory() {
        boolean isDirectory;
        switch (mode) {
            case SMB:
                try {
                    isDirectory = new SmbFile(path).isDirectory();
                } catch (SmbException e) {
                    isDirectory = false;
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    isDirectory = false;
                    e.printStackTrace();
                }
                break;
            case FILE:
                isDirectory = new File(path).isDirectory();
                break;
            case ROOT:
                try {
                    isDirectory = RootHelper.isDirectory(path, true, 5);
                } catch (RootNotPermittedException e) {
                    e.printStackTrace();
                    isDirectory = false;
                }
                break;
            case OTG:
                // TODO: support for this method in OTG on-the-fly
                // you need to manually call {@link RootHelper#getDocumentFile() method
                isDirectory = false;
                break;
            default:
                isDirectory = new File(path).isDirectory();
                break;

        }
        return isDirectory;
    }

    public boolean isDirectory(Context context) {

        boolean isDirectory;
        switch (mode) {
            case SMB:
                try {
                    isDirectory = new SmbFile(path).isDirectory();
                } catch (SmbException e) {
                    isDirectory = false;
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    isDirectory = false;
                    e.printStackTrace();
                }
                break;
            case FILE:
                isDirectory = new File(path).isDirectory();
                break;
            case ROOT:
                try {
                    isDirectory = RootHelper.isDirectory(path,true,5);
                } catch (RootNotPermittedException e) {
                    e.printStackTrace();
                    isDirectory = false;
                }
                break;
            case OTG:
                isDirectory = RootHelper.getDocumentFile(path, context, false).isDirectory();
                break;
            default:
                isDirectory = new File(path).isDirectory();
                break;

        }
        return isDirectory;
    }

    /**
     * @deprecated use {@link #folderSize(Context)}
     * @return
     */
    public long folderSize() {
        long size = 0L;

        switch (mode) {
            case SMB:
                try {
                    size = Futils.folderSize(new SmbFile(path));
                } catch (MalformedURLException e) {
                    size = 0L;
                    e.printStackTrace();
                }
                break;
            case FILE:
                size = Futils.folderSize(new File(path));
                break;
            case ROOT:
                BaseFile baseFile = generateBaseFileFromParent();
                if (baseFile != null) size = baseFile.getSize();
                break;
            default:
                return 0L;
        }
        return size;
    }

    /**
     * Helper method to get length of folder in an otg
     *
     * @param context
     * @return
     */
    public long folderSize(Context context) {

        long size = 0l;

        switch (mode){
            case SMB:
                try {
                    size = Futils.folderSize(new SmbFile(path));
                } catch (MalformedURLException e) {
                    size = 0l;
                    e.printStackTrace();
                }
                break;
            case FILE:
                size = Futils.folderSize(new File(path));
                break;
            case ROOT:
                BaseFile baseFile=generateBaseFileFromParent();
                if(baseFile!=null) size = baseFile.getSize();
                break;
            case OTG:
                size = Futils.folderSize(path, context);
                break;
            default:
                return 0l;
        }
        return size;
    }


    public long getUsableSpace() {
        long size = 0L;
        if (isSmb()) {
            try {
                size = (new SmbFile(path).getDiskFreeSpace());
            } catch (MalformedURLException e) {
                size = 0L;
                e.printStackTrace();
            } catch (SmbException e) {
                size = 0L;
                e.printStackTrace();
            }
        } else
            size = (new File(path).getUsableSpace());
        return size;
    }

    /**
     * @deprecated use {@link #listFiles(Context, boolean)}
     * @param rootmode
     * @return
     */
    public ArrayList<BaseFile> listFiles(boolean rootmode) {
        ArrayList<BaseFile> arrayList = new ArrayList<>();
        if (isSmb()) {
            try {
                SmbFile smbFile = new SmbFile(path);
                for (SmbFile smbFile1 : smbFile.listFiles()) {
                    BaseFile baseFile = new BaseFile(smbFile1.getPath());
                    baseFile.setName(smbFile1.getName());
                    baseFile.setMode(OpenMode.SMB);
                    baseFile.setDirectory(smbFile1.isDirectory());
                    baseFile.setDate(smbFile1.lastModified());
                    baseFile.setSize(baseFile.isDirectory() ? 0 : smbFile1.length());
                    arrayList.add(baseFile);
                }
            } catch (MalformedURLException e) {
                if (arrayList != null) arrayList.clear();
                e.printStackTrace();
            } catch (SmbException e) {
                if (arrayList != null) arrayList.clear();
                e.printStackTrace();
            }
        } else if (isOtgFile()) {

        } else {
            try {
                arrayList = RootHelper.getFilesList(path, rootmode, true, null);
            } catch (RootNotPermittedException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    /**
     * Helper method to list children of this file
     *
     * @param context
     * @return
     */
    public ArrayList<BaseFile> listFiles(Context context, boolean isRoot) {
        ArrayList<BaseFile> arrayList = new ArrayList<>();
        switch (mode) {
            case SMB:
                try {
                    SmbFile smbFile = new SmbFile(path);
                    for (SmbFile smbFile1 : smbFile.listFiles()) {
                        BaseFile baseFile=new BaseFile(smbFile1.getPath());
                        baseFile.setName(smbFile1.getName());
                        baseFile.setMode(OpenMode.SMB);
                        baseFile.setDirectory(smbFile1.isDirectory());
                        baseFile.setDate(smbFile1.lastModified());
                        baseFile.setSize(baseFile.isDirectory()?0:smbFile1.length());
                        arrayList.add(baseFile);
                    }
                } catch (MalformedURLException e) {
                    if (arrayList != null) arrayList.clear();
                    e.printStackTrace();
                } catch (SmbException e) {
                    if (arrayList != null) arrayList.clear();
                    e.printStackTrace();
                }
                break;
            case OTG:
                arrayList = RootHelper.getDocumentFilesList(path, context);
                break;
            default:
                try {
                    arrayList = RootHelper.getFilesList(path, isRoot, true, null);
                } catch (RootNotPermittedException e) {
                    e.printStackTrace();
                }
        }

        return arrayList;
    }

    public String getReadablePath(String path) {
        if (isSmb())
            return parseSmbPath(path);
        return path;
    }

    String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }

    /**
     * Handles getting input stream for various {@link OpenMode}
     * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
     * @return
     */
    public InputStream getInputStream() {
        InputStream inputStream;
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

    public InputStream getInputStream(Context context) {
        InputStream inputStream;

        switch (mode) {
            case SMB:
                try {
                    inputStream = new SmbFile(path).getInputStream();
                } catch (IOException e) {
                    inputStream = null;
                    e.printStackTrace();
                }
                break;
            case OTG:
                ContentResolver contentResolver = context.getContentResolver();
                DocumentFile documentSourceFile = RootHelper.getDocumentFile(path,
                        context, false);
                try {
                    inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    inputStream = null;
                }
                break;
            default:
                try {
                    inputStream = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    inputStream = null;
                    e.printStackTrace();
                }
                break;
        }
        return inputStream;
    }

    public OutputStream getOutputStream(Context context) {
        OutputStream outputStream;
        switch (mode) {
            case SMB:
                try {
                    outputStream = new SmbFile(path).getOutputStream();
                } catch (IOException e) {
                    outputStream = null;
                    e.printStackTrace();
                }
                break;
            case OTG:
                ContentResolver contentResolver = context.getContentResolver();
                DocumentFile documentSourceFile = RootHelper.getDocumentFile(path,
                        context, true);
                try {
                    outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    outputStream = null;
                }
                break;
            default:
                try {
                    outputStream = FileUtil.getOutputStream(new File(path), context, length());
                } catch (Exception e) {
                    outputStream=null;
                    e.printStackTrace();
                }

        }
        return outputStream;
    }

    public boolean exists() {
        boolean exists = false;
        if (isSmb()) {
            try {
                SmbFile smbFile = getSmbFile(2000);
                exists = smbFile != null && smbFile.exists();
            } catch (SmbException e) {
                exists = false;
            }
        } else if (isLocal()) {
            exists = new File(path).exists();
        } else if (isRoot()) {
            try {
                return RootHelper.fileExists(path);
            } catch (RootNotPermittedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return exists;
    }

    /**
     * Helper method to check file existence in otg
     *
     * @param context
     * @return
     */
    public boolean exists(Context context) {
        if (isOtgFile()) {
            DocumentFile fileToCheck = RootHelper.getDocumentFile(path, context, false);
            return fileToCheck != null;
        } else return (exists());
    }

    /**
     * Whether file is a simple file (i.e. not a directory/smb/otg/other)
     *
     * @return true if file; other wise false
     */
    public boolean isSimpleFile() {
        return !isSmb() && !isOtgFile() && !isCustomPath()
                && !android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches() &&
                !new File(path).isDirectory();
    }

    public boolean setLastModified(long date) {
        if (isSmb())
            try {
                new SmbFile(path).setLastModified(date);
                return true;
            } catch (SmbException e) {
                return false;
            } catch (MalformedURLException e) {
                return false;
            }
        File f = new File(path);
        return f.setLastModified(date);

    }

    public void mkdir(Context context) {
        if (isSmb()) {
            try {
                new SmbFile(path).mkdirs();
            } catch (SmbException | MalformedURLException e) {
                Logger.log(e, path, context);
            }
        } else if (isOtgFile()) {
            if (!exists(context)) {
                DocumentFile parentDirectory = RootHelper.getDocumentFile(getParent(), context, false);
                if (parentDirectory.isDirectory()) {
                    parentDirectory.createDirectory(getName());
                }
            }

        } else
            FileUtil.mkdir(new File(path), context);
    }

    public boolean delete(Context context, boolean rootmode) throws RootNotPermittedException {
        if (isSmb()) {
            try {
                new SmbFile(path).delete();
            } catch (SmbException | MalformedURLException e) {
                Logger.log(e, path, context);
            }
        } else {
            if (isRoot() && rootmode) {
                setMode(OpenMode.ROOT);

                RootUtils.delete(getPath());
            } else {

                FileUtil.deleteFile(new File(path), context);
            }
        }
        return !exists();
    }

}
