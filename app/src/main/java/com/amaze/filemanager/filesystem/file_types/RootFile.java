package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;

import java.io.File;
import java.util.ArrayList;

import jcifs.smb.SmbException;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class RootFile extends HybridFile {
    private String path;
    final private OpenMode mode = OpenMode.ROOT;

    public RootFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public RootFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long lastModified() throws SmbException {
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null)
            return baseFile.getDate();
        return super.lastModified();
    }

    @Override
    public long length() {
        return length(AppConfig.getInstance());
    }

    @Override
    public long length(Context context) {
        HybridFileParcelable baseFile=generateBaseFileFromParent();
        if(baseFile!=null)
            return baseFile.getSize();
        return super.length(context);
    }

    HybridFileParcelable generateBaseFileFromParent() {
        ArrayList<HybridFileParcelable> arrayList = RootHelper.getFilesList(getFile().getParent(), true, true, null);
        for (HybridFileParcelable baseFile : arrayList) {
            if (baseFile.getPath().equals(path))
                return baseFile;
        }
        return null;
    }

    @Override
    public String getName() {
        return getName(AppConfig.getInstance());
    }

    @Override
    public String getName(Context context) {
        return new File(path).getName();
    }

    @Override
    public String getParent() {
        return getParent(AppConfig.getInstance());
    }

    @Override
    public String getParent(Context context) {
        return new File(path).getParent();
    }

    @Override
    public boolean isDirectory() {
        return isDirectory(AppConfig.getInstance());
    }

    @Override
    public boolean isDirectory(Context context) {
        try {
            return RootHelper.isDirectory(path, true, 5);
        } catch (ShellNotRunningException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long folderSize() {
        return folderSize(AppConfig.getInstance());
    }

    @Override
    public long folderSize(Context context) {
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) return baseFile.getSize();
        return super.folderSize(context);
    }

    @Override
    public long getUsableSpace() {
        return new File(path).getUsableSpace();
    }

    @Override
    public long getTotal(Context context) {
        return new File(path).getTotalSpace();
    }

    @Override
    public boolean exists() {
        try {
            return RootHelper.fileExists(path);
        } catch (ShellNotRunningException e) {
            e.printStackTrace();
            return false;
        }
    }
}
