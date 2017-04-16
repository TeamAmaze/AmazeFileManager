/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.ui;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.OpenMode;

import java.util.Calendar;

public class LayoutElements implements Parcelable {

    private static final String CURRENT_YEAR = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    public LayoutElements(Parcel im) {
        title = im.readString();
        desc = im.readString();
        permissions = im.readString();
        symlink = im.readString();
        int j = im.readInt();
        date = im.readLong();
        int i = im.readInt();
        header = i != 0;
        isDirectory = j != 0;
        // don't save bitmaps in parcel, it might exceed the allowed transaction threshold
        //Bitmap bitmap = (Bitmap) im.readParcelable(getClass().getClassLoader());
        // Convert Bitmap to Drawable:
        //imageId = new BitmapDrawable(bitmap);
        date1 = im.readString();
        size = im.readString();
        longSize=im.readLong();
    }


    public int describeContents() {
        // TODO: Implement this method
        return 0;
    }

    public void writeToParcel(Parcel p1, int p2) {
        p1.writeString(title);
        p1.writeString(desc);
        p1.writeString(permissions);
        p1.writeString(symlink);
        p1.writeInt(isDirectory?1:0);
        p1.writeLong(date);
        p1.writeInt(header ? 1 : 0);
        //p1.writeParcelable(imageId.getBitmap(), p2);
        p1.writeString(date1);
        p1.writeString(size);
        p1.writeLong(longSize);
        // TODO: Implement this method
    }

    private BitmapDrawable imageId;
    private String title;
    private String desc;
    private String permissions;
    private String symlink;
    private String size;
    private boolean isDirectory;
    private long date = 0,longSize=0;
    private String date1 = "";
    private boolean header;
    //same as hfile.modes but different than openmode in Main.java
    private OpenMode mode = OpenMode.FILE;

    public LayoutElements(BitmapDrawable imageId, String title, String desc, String permissions,
                          String symlink, String size, long longSize, boolean header, String date, boolean isDirectory) {
        this.imageId = imageId;
        this.title = title;
        this.desc = desc;
        this.permissions = permissions.trim();
        this.symlink = symlink.trim();
        this.size = size;
        this.header = header;
        this.longSize=longSize;
        this.isDirectory = isDirectory;
        if (!date.trim().equals("")) {
            this.date = Long.parseLong(date);
            this.date1 = Futils.getdate(this.date, CURRENT_YEAR);
        }
    }

    public static final Parcelable.Creator<LayoutElements> CREATOR =
            new Parcelable.Creator<LayoutElements>() {
                public LayoutElements createFromParcel(Parcel in) {
                    return new LayoutElements(in);
                }

                public LayoutElements[] newArray(int size) {
                    return new LayoutElements[size];
                }
            };

    public Drawable getImageId() {
        return imageId;
    }

    public void setImageId(BitmapDrawable imageId){this.imageId=imageId;}
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

    public BaseFile generateBaseFile() {
        BaseFile baseFile=new BaseFile(getDesc(), getPermissions(), getDate1(), getlongSize(), isDirectory());
        baseFile.setMode(mode);
        baseFile.setName(title);
        return baseFile;
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
}
