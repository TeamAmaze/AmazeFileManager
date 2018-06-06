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
import android.util.Log;

import com.amaze.filemanager.filesystem.FileUtil;

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.File;

public class AndroidFileSystemView implements FileSystemView {

    private static final String FILESYSTEM_ROOT = "/";

    private static final String CURRENT_DIR = "./";

    private final Context context;

    private final String fileSystemViewRoot;

    private String currentDir;

    public AndroidFileSystemView(@NonNull Context context, @NonNull String fileSystemViewRoot){
        this.context = context;
        this.fileSystemViewRoot = fileSystemViewRoot;
        this.currentDir = FILESYSTEM_ROOT;
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
        switch(file){
            case FILESYSTEM_ROOT:
                return createFtpFileFrom(fileSystemViewRoot);
            case CURRENT_DIR:
                return createFtpFileFrom(fileSystemViewRoot + currentDir);
            default:
                return createFtpFileFrom(fileSystemViewRoot + file);
        }
    }

    @Override
    public FtpFile getWorkingDirectory() {
        return getFile(currentDir);
    }

    @Override
    public boolean changeWorkingDirectory(String dir) {
        currentDir = dir;
        return true;
    }

    @Override
    public FtpFile getHomeDirectory() {
        new Exception().printStackTrace();
        return createFtpFileFrom(fileSystemViewRoot);
    }

    @Override
    public void dispose() {

    }

    private FtpFile createFtpFileFrom(String fullPath){
        return new AndroidSafFtpFile(context, fileSystemViewRoot, DocumentFile.fromFile(new File(fullPath)));
    }
}
