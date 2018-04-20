/*
 * AndroidFileSystemView.java
 *
 * Copyright © 2018 Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */
package com.amaze.filemanager.filesystem.ftpserver;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.File;

public class AndroidFileSystemView implements FileSystemView {

    private final Context context;

    private final String fileSystemViewRoot;

    private String currentDir;

    public AndroidFileSystemView(@NonNull Context context, @NonNull String fileSystemViewRoot){
        this.context = context;
        this.fileSystemViewRoot = fileSystemViewRoot;
    }

    /**
     * Does the file system support random file access?
     * @return false
     * @throws FtpException
     */
    @Override
    public boolean isRandomAccessible() {
        return false;
    }

    @Override
    public FtpFile getFile(String file) {

        File fileObj = new File(file);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            return new AndroidSafFtpFile(context, DocumentFile.fromFile(fileObj));
        else
            return new AndroidLegacyFtpFile(context, fileObj);
    }

    @Override
    public FtpFile getWorkingDirectory() {
        return null;
    }

    @Override
    public boolean changeWorkingDirectory(String dir) {
        return false;
    }

    @Override
    public FtpFile getHomeDirectory() {
        return null;
    }

    @Override
    public void dispose() {

    }
}
