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

package com.amaze.filemanager.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import jcifs.smb.SmbFile;

public class Layoutelements implements Parcelable {
    public Layoutelements(Parcel im) {
        try {
            Bitmap bitmap = (Bitmap) im.readParcelable(getClass().getClassLoader());
            // Convert Bitmap to Drawable:
            imageId = new BitmapDrawable(bitmap);

            title = im.readString();
            desc = im.readString();
            permissions = im.readString();
            symlink = im.readString();
            int j = im.readInt();
            date = im.readLong();
            int i = im.readInt();
            if (i == 0) {
                header = false;
            } else {
                header = true;
            } if (j == 0) {
                isDirectory = false;
            } else {
                isDirectory= true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        date1 = im.readString();
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
        p1.writeParcelable(((BitmapDrawable) imageId).getBitmap(), p2);
        p1.writeString(date1);
        // TODO: Implement this method
    }

    private Drawable imageId;
    private String title;
    private String desc;
    private String permissions;
    private String symlink;
    private String size;
    private boolean isDirectory;
    private long date = 0;
    private String date1 = "";
    private boolean header;

    public Layoutelements(Drawable imageId, String title, String desc, String permissions, String symlink, String size,  boolean header, String date,boolean isDirectory) {
        this.imageId = imageId;
        this.title = title;
        this.desc = desc;
        this.permissions = permissions.trim();
        this.symlink = symlink.trim();
        this.size = size;
        this.header = header;
        this.isDirectory = isDirectory;
        if (!date.trim().equals("")) {
            this.date = Long.parseLong(date);
            this.date1 = new Futils().getdate(this.date, "MMM dd, yyyy", "15");
        }
    }


    public static final Parcelable.Creator<Layoutelements> CREATOR =
            new Parcelable.Creator<Layoutelements>() {
                public Layoutelements createFromParcel(Parcel in) {
                    return new Layoutelements(in);
                }

                public Layoutelements[] newArray(int size) {
                    return new Layoutelements[size];
                }
            };

    public Drawable getImageId() {
        return imageId;
    }


    public String getDesc() {
        return desc.toString();
    }


    public String getTitle() {
        return title.toString();
    }

    public boolean isDirectory() {
    return isDirectory;}

    public String getSize() {
        return size;
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
        if (getSymlink() != null && getSymlink().length() != 0) {
            return true;
        } else return false;
    }

    @Override
    public String toString() {
        return title + "\n" + desc;
    }
}
