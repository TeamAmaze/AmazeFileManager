/*
 * AndroidFtpFile.java
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AndroidFtpFile implements FtpFile {

    private final Context context;

    private final DocumentFile backingDocumentFile;

    public AndroidFtpFile(@NonNull Context context, @NonNull DocumentFile backingDocumentFile) {
        this.context = context;
        this.backingDocumentFile = backingDocumentFile;
        System.err.println(backingDocumentFile);
        System.err.println(this.backingDocumentFile);
    }

    @Override
    public InputStream createInputStream(long offset) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(backingDocumentFile.getUri());
    }

    @Override
    public OutputStream createOutputStream(long offset) throws FileNotFoundException {
        return context.getContentResolver().openOutputStream(backingDocumentFile.getUri());
    }

    @Override
    public boolean doesExist() {
        return backingDocumentFile != null && backingDocumentFile.exists();
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
        return backingDocumentFile.getUri();
    }

    @Override
    public String getAbsolutePath() {
        System.err.println(backingDocumentFile);
        System.err.println(backingDocumentFile.getUri());
        return backingDocumentFile.getUri().getPath();
    }

    @Override
    public String getName() {
        return backingDocumentFile.getName();
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
        return backingDocumentFile.lastModified();
    }

    @Override
    public long getSize() {
        return backingDocumentFile.length();
    }

    @Override
    public boolean isDirectory() {
        return backingDocumentFile.isDirectory();
    }

    @Override
    public boolean isFile() {
        return backingDocumentFile.isFile();
    }

    @Override
    public boolean isReadable() {
        return backingDocumentFile.canRead();
    }

    @Override
    public boolean isWritable() {
        return backingDocumentFile.canWrite();
    }

    //FIXME: dot sign
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * If user can write to file, user should also be able to delete as well.
     * @return
     */
    @Override
    public boolean isRemovable() {
        return backingDocumentFile.canWrite();
    }

    @Override
    public boolean mkdir() {
        return backingDocumentFile.getParentFile().createDirectory(getName()) != null;
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
        return backingDocumentFile.delete();
    }

    @Override
    public List<AndroidFtpFile> listFiles() {
        if(isDirectory()) {
            List<AndroidFtpFile> retval = new ArrayList<>();
            for (DocumentFile documentFile : backingDocumentFile.listFiles()) {
                retval.add(new AndroidFtpFile(context, documentFile));
            }
            return retval;
        } else {
            return null;
        }
    }
}
