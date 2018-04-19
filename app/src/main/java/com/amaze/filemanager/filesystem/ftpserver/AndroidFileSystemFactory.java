/*
 * AndroidFileSystemFactory.java
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

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.User;

public class AndroidFileSystemFactory implements FileSystemFactory {

    private final Context context;

    private final String ftpServerRoot;

    public AndroidFileSystemFactory(@NonNull final Context context, @NonNull String ftpServerRoot){
        this.context = context;
        this.ftpServerRoot = ftpServerRoot;
    }

    @Override
    public FileSystemView createFileSystemView(User user) {
        return new AndroidFileSystemView(context, ftpServerRoot);
    }
}
