/*
 * AndroidFtplet.java
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

import com.amaze.filemanager.utils.files.FileUtils;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;

import java.io.IOException;

/**
 * {@link Ftplet} implementation based on {@link DefaultFtplet}, overriding <code>onUploadEnd</code>
 * method to trigger media store refresh.
 *
 * {@see {@link DefaultFtplet#onUploadEnd(FtpSession, FtpRequest)}}
 * {@see {@link FileUtils#scanFile(String, Context)}}
 */
public class AndroidFtplet extends DefaultFtplet {

    private final Context context;

    private final String fileSystemRoot;

    public AndroidFtplet(@NonNull Context context, @NonNull String fileSystemRoot) {
        this.context = context;
        this.fileSystemRoot = fileSystemRoot;
    }

    /**
     * Overrides parent class with trigger MediaStore refresh by broadcast Intent.
     *
     * @return {@link DefaultFtplet#onUploadEnd(FtpSession, FtpRequest)}
     */
    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        FileUtils.scanFile(fileSystemRoot + request.getArgument(), context);
        return super.onUploadEnd(session, request);
    }
}
