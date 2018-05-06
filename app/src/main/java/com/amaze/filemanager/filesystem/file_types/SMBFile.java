package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OpenMode;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class SMBFile extends HybridFile {
    public SMBFile(OpenMode mode, String path) {
        super(mode, path);
    }

    public SMBFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
    }

    @Override
    public long length() {
        SmbFile smbFile = getSmbFile();
        if (smbFile != null)
            try {
                return smbFile.length();
            } catch (SmbException ignored) {
            }
        return super.length();
    }

    @Override
    public long length(Context context) {
        SmbFile smbFile=getSmbFile();
        if(smbFile!=null)
            try {
                return smbFile.length();
            } catch (SmbException ignored) {
            }
        return super.length(context);
    }
}
