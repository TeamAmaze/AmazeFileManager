package com.amaze.filemanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by arpitkh996 on 11-01-2016.
 */
public class BaseFile implements Parcelable{
    long date,size;
    boolean isDirectory;
    String path;
    String permisson;


    String link="";

    public BaseFile(String path) {
        this.path = path;
    }

    public BaseFile(String path, String permisson, long date, long size, boolean isDirectory) {
        this.date = date;
        this.size = size;
        this.isDirectory = isDirectory;
        this.path = path;
        this.permisson = permisson;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPermisson() {
        return permisson;
    }

    public void setPermisson(String permisson) {
        this.permisson = permisson;
    }
    protected BaseFile(Parcel in) {
        date = in.readLong();
        size = in.readLong();
        isDirectory = in.readByte() != 0;
        path = in.readString();
        permisson = in.readString();
    }

    public static final Creator<BaseFile> CREATOR = new Creator<BaseFile>() {
        @Override
        public BaseFile createFromParcel(Parcel in) {
            return new BaseFile(in);
        }

        @Override
        public BaseFile[] newArray(int size) {
            return new BaseFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date);
        dest.writeLong(size);
        dest.writeByte((byte) (isDirectory ? 1 : 0));
        dest.writeString(path);
        dest.writeString(permisson);
    }
}
