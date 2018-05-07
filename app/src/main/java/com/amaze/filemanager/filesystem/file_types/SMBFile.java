package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.files.FileUtils;

import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class SMBFile extends HybridFile {
    private String path;
    final private OpenMode mode = OpenMode.SMB;

    public SMBFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public SMBFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long lastModified() throws SmbException {
        SmbFile smbFile = getSmbFile();
        if (smbFile != null)
            return smbFile.lastModified();
        return super.lastModified();
    }

    @Override
    public long length() {
        return length(AppConfig.getInstance());
    }

    @Override
    public long length(Context context) {
        SmbFile smbFile = getSmbFile();
        if (smbFile != null)
            try {
                return smbFile.length();
            } catch (SmbException ignored) {
            }
        return super.length(context);
    }

    @Override
    public String getName() {
        return  getName(AppConfig.getInstance());
    }

    @Override
    public String getName(Context context) {
        SmbFile smbFile = getSmbFile();
        if(smbFile!=null)
            return smbFile.getName();
        return super.getName(context);
    }

    @Override
    public String getParent() {
        return getParent(AppConfig.getInstance());
    }

    @Override
    public String getParent(Context context) {
        try {
            return new SmbFile(path).getParent();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public boolean isDirectory() {
        return isDirectory(AppConfig.getInstance());
    }

    @Override
    public boolean isDirectory(Context context) {
        try {
            return new SmbFile(path).isDirectory();
        } catch (SmbException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long folderSize() {
        return folderSize(AppConfig.getInstance());
    }

    @Override
    public long folderSize(Context context) {
        try {
            return FileUtils.folderSize(new SmbFile(path));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    @Override
    public long getUsableSpace() {
        try {
            return (new SmbFile(path).getDiskFreeSpace());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    public long getTotal(Context context) {
        // TODO: Find total storage space of SMB when JCIFS adds support
        try {
            return new SmbFile(path).getDiskFreeSpace();
        } catch (SmbException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    public boolean exists() {
        try {
            SmbFile smbFile = getSmbFile(2000);
            return smbFile != null && smbFile.exists();
        } catch (SmbException e) {
            return false;
        }
    }

    private SmbFile getSmbFile() {
        try {
            return new SmbFile(path);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private SmbFile getSmbFile(int timeout) {
        try {
            SmbFile smbFile = new SmbFile(path);
            smbFile.setConnectTimeout(timeout);
            return smbFile;
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
