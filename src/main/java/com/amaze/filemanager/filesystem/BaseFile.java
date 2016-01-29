package com.amaze.filemanager.filesystem;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by arpitkh996 on 11-01-2016.
 */
public class BaseFile extends HFile implements Parcelable{
    long date,size;
    boolean isDirectory;
    String permisson;
    String name;

    String link="";
    public BaseFile(String path) {
        super(0,path);
        this.path = path;
    }

    public BaseFile(String path, String permisson, long date, long size, boolean isDirectory) {
        super(0,path);
        this.date = date;
        this.size = size;
        this.isDirectory = isDirectory;
        this.path = path;
        this.permisson = permisson;

    }

    @Override
    public String getName() {
        if(name!=null && name.length()>0)
        return name;
        else return super.getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMode() {
        return mode;
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


    public String getPermisson() {
        return permisson;
    }

    public void setPermisson(String permisson) {
        this.permisson = permisson;
    }
    protected BaseFile(Parcel in) {
        super(in.readInt(),in.readString());
        permisson = in.readString();
        name=in.readString();
        date = in.readLong();
        size = in.readLong();
        isDirectory = in.readByte() != 0;

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
        dest.writeInt(mode);
        dest.writeString(path);
        dest.writeString(permisson);
        dest.writeString(name);
        dest.writeLong(date);
        dest.writeLong(size);
        dest.writeByte((byte) (isDirectory ? 1 : 0));

    }
}
