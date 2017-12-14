/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *                      Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.adapters.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;

import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.Utils;

import java.io.File;
import java.util.Calendar;

public class LayoutElementParcelable implements Parcelable {

    private static final String CURRENT_YEAR = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    private int filetype;
    private IconDataParcelable iconData;
    private String title;
    private String desc;
    private String permissions;
    private String symlink;
    private String size;
    private boolean isDirectory;
    private long date;
    private long longSize;
    private String date1;
    private boolean header;

    //same as hfile.modes but different than openmode in Main.java
    private OpenMode mode = OpenMode.FILE;

    public LayoutElementParcelable(String title, String path, String permissions,
                                   String symlink, String size, long longSize, boolean header,
                                   String date, boolean isDirectory, boolean isGrid, boolean useThumbs) {
        filetype = Icons.getTypeOfFile(path);
        @DrawableRes int fallbackIcon = Icons.loadMimeIcon(path, isGrid);

        if(useThumbs) {
            if (filetype == Icons.PICTURE || filetype == Icons.VIDEO || filetype == Icons.APK) {
                this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_FROMFILE, path, fallbackIcon);
            } else {
                this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_RES, fallbackIcon);
            }
        } else {
            this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_RES, fallbackIcon);
        }

        this.title = title;
        this.desc = path;
        this.permissions = permissions.trim();
        this.symlink = symlink.trim();
        this.size = size;
        this.header = header;
        this.longSize=longSize;
        this.isDirectory = isDirectory;
        if (!date.trim().equals("")) {
            this.date = Long.parseLong(date);
            this.date1 = Utils.getDate(this.date, CURRENT_YEAR);
        } else {
            this.date = 0;
            this.date1 = "";
        }
    }

    public LayoutElementParcelable(String path, String permissions, String symlink,
                                   String size, long longSize, boolean isDirectory, boolean header,
                                   String date, boolean isGrid, boolean useThumbs) {
        this(new File(path).getName(), path, permissions, symlink, size, longSize, header, date, isDirectory, isGrid, useThumbs);

    }

    public HybridFileParcelable generateBaseFile() {
        HybridFileParcelable baseFile=new HybridFileParcelable(getDesc(), getPermissions(), getDate1(), getlongSize(), isDirectory());
        baseFile.setMode(mode);
        baseFile.setName(title);
        return baseFile;
    }

    public int getFiletype() {
        return filetype;
    }

    public IconDataParcelable getIconData() {
        return iconData;
    }

    public String getDesc() {
        return desc;
    }

    public String getTitle() {
        return title;
    }

    public OpenMode getMode() {
        return mode;
    }

    public void setMode(OpenMode mode) {
        this.mode = mode;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getSize() {
        return size;
    }

    public long getlongSize() {
        return longSize;
    }

    public String getDate() {
        return date1;
    }

    public long getDate1() {
        return date;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getSymlink() {
        return symlink;
    }

    public boolean hasSymlink() {
        return getSymlink() != null && getSymlink().length() != 0;
    }

    @Override
    public String toString() {
        return title + "\n" + desc;
    }

    public LayoutElementParcelable(Parcel im) {
        filetype = im.readInt();
        iconData = im.readParcelable(IconDataParcelable.class.getClassLoader());
        title = im.readString();
        desc = im.readString();
        permissions = im.readString();
        symlink = im.readString();
        int j = im.readInt();
        date = im.readLong();
        int i = im.readInt();
        header = i != 0;
        isDirectory = j != 0;
        date1 = im.readString();
        size = im.readString();
        longSize=im.readLong();
    }

    @Override
    public int describeContents() {
        // TODO: Implement this method
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p1, int p2) {
        p1.writeInt(filetype);
        p1.writeParcelable(iconData, 0);
        p1.writeString(title);
        p1.writeString(desc);
        p1.writeString(permissions);
        p1.writeString(symlink);
        p1.writeInt(isDirectory?1:0);
        p1.writeLong(date);
        p1.writeInt(header ? 1 : 0);
        p1.writeString(date1);
        p1.writeString(size);
        p1.writeLong(longSize);
    }

    public static final Parcelable.Creator<LayoutElementParcelable> CREATOR =
            new Parcelable.Creator<LayoutElementParcelable>() {
                public LayoutElementParcelable createFromParcel(Parcel in) {
                    return new LayoutElementParcelable(in);
                }

                public LayoutElementParcelable[] newArray(int size) {
                    return new LayoutElementParcelable[size];
                }
            };

}
