/*
 * AndroidLegacyFtpFile.java
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
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AndroidLegacyFtpFile implements FtpFile {

    private final Context context;

    private final File backingFile;

    public AndroidLegacyFtpFile(@NonNull Context context, @NonNull File backingFile) {
        this.context = context;
        this.backingFile = backingFile;
    }

    /**
     * Not wrapped with {@link java.io.BufferedInputStream} since callers should have done this already.
     *
     * @param offset ignored
     * @return {@link FileInputStream}
     * @throws IOException
     */
    @Override
    public InputStream createInputStream(long offset) throws IOException {
        return new FileInputStream(backingFile);
    }

    /**
     * Not wrapped with {@link java.io.BufferedOutputStream} since callers should have done this already.
     *
     * @param offset ignored
     * @return {@link FileOutputStream}
     * @throws IOException
     */
    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        return new FileOutputStream(backingFile);
    }

    @Override
    public boolean doesExist() {
        return backingFile.exists();
    }

    /**
     * @see NativeFtpFile#getLinkCount()
     */
    @Override
    public int getLinkCount() {
        return isDirectory() ? 3 : 1;
    }

    /**
     * Returns backing {@link DocumentFile}'s {@link android.net.Uri}
     * @return {@link android.net.Uri}
     */
    @Override
    public Object getPhysicalFile() {
        return backingFile;
    }

    @Override
    public String getAbsolutePath() {
        return backingFile.getAbsolutePath();
    }

    @Override
    public String getName() {
        return backingFile.getName();
    }

    /**
     * @see NativeFtpFile#getGroupName()
     * @return group name "group"
     */
    @Override
    public String getGroupName() {
        return "group";
    }

    /**
     * @see NativeFtpFile#getOwnerName()
     * @return owner name "user"
     */
    @Override
    public String getOwnerName() {
        return "user";
    }

    @Override
    public long getLastModified() {
        return backingFile.lastModified();
    }

    @Override
    public long getSize() {
        return backingFile.length();
    }

    @Override
    public boolean isDirectory() {
        return backingFile.isDirectory();
    }

    @Override
    public boolean isFile() {
        return backingFile.isFile();
    }

    @Override
    public boolean isReadable() {
        return backingFile.canRead();
    }

    @Override
    public boolean isWritable() {
        return backingFile.canWrite();
    }

    @Override
    public boolean isHidden() {
        return backingFile.isHidden();
    }

    /**
     * If user can write to file, user should also be able to delete as well.
     * @return
     */
    @Override
    public boolean isRemovable() {
        return backingFile.canWrite();
    }

    @Override
    public boolean mkdir() {
        return backingFile.mkdir();
    }

    /**
     * Unsupported.
     * @param time
     * @return
     */
    @Override
    public boolean setLastModified(long time) {
        return false;
    }

    @Override
    public boolean move(FtpFile destination) {
        return false;
    }

    @Override
    public boolean delete() {
        return backingFile.delete();
    }

    @Override
    public List<AndroidLegacyFtpFile> listFiles() {
        if(isDirectory()) {
            List<AndroidLegacyFtpFile> retval = new ArrayList<>();
            for (File file : backingFile.listFiles()) {
                retval.add(new AndroidLegacyFtpFile(context, file));
            }
            return retval;
        } else {
            return null;
        }
    }
}
