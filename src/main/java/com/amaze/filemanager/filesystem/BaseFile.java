package com.amaze.filemanager.filesystem;

import android.os.Parcel;
import android.os.Parcelable;

import com.amaze.filemanager.utils.OpenMode;

/**
 * Created by arpitkh996 on 11-01-2016.
 */
public class BaseFile extends HFile implements Parcelable {

    private long date, size;
    private boolean isDirectory;
    private String permission;
    private String name;
    private String link = "";

    public BaseFile(String path) {
        super(OpenMode.FILE, path);
        this.path = path;
    }

    public BaseFile(String path, String permission, long date, long size, boolean isDirectory) {
        super(OpenMode.FILE, path);
        this.date = date;
        this.size = size;
        this.isDirectory = isDirectory;
        this.path = path;
        this.permission = permission;

    }

    @Override
    public String getName() {
        if (name != null && name.length() > 0)
            return name;
        else return super.getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public OpenMode getMode() {
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

    /**
     * @return
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getPath() {
        return path;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    protected BaseFile(Parcel in) {
        super(OpenMode.getOpenMode(in.readInt()), in.readString());
        permission = in.readString();
        name = in.readString();
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
        dest.writeInt(mode.ordinal());
        dest.writeString(path);
        dest.writeString(permission);
        dest.writeString(name);
        dest.writeLong(date);
        dest.writeLong(size);
        dest.writeByte((byte) (isDirectory ? 1 : 0));

    }

}
