package com.amaze.filemanager.ui;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.zip.ZipEntry;

/**
 * Created by Arpit on 11-12-2014.
 */
public class ZipObjectParcelable implements Parcelable {

    private boolean directory;
    private ZipEntry entry;
    private String name;
    private long date, size;

    public ZipObjectParcelable(ZipEntry entry, long date, long size, boolean directory) {
        this.directory = directory;
        this.entry = entry;
        if (entry != null) {
            name = entry.getName();
            this.date = date;
            this.size = size;

        }
    }

    public ZipEntry getEntry() {
        return entry;
    }

    public boolean isDirectory() {
        return directory;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getTime() {
        return date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p1, int p2) {
        p1.writeString(name);
        p1.writeLong(size);
        p1.writeLong(date);
        p1.writeInt(isDirectory() ? 1 : 0);
    }

    public static final Parcelable.Creator<ZipObjectParcelable> CREATOR =
            new Parcelable.Creator<ZipObjectParcelable>() {
                public ZipObjectParcelable createFromParcel(Parcel in) {
                    return new ZipObjectParcelable(in);
                }

                public ZipObjectParcelable[] newArray(int size) {
                    return new ZipObjectParcelable[size];
                }
            };

    public ZipObjectParcelable(Parcel im) {
        name = im.readString();
        size = im.readLong();
        date = im.readLong();
        int i = im.readInt();
        directory = i != 0;
        entry = new ZipEntry(name);
    }

}
