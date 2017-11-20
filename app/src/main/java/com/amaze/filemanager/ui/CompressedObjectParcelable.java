package com.amaze.filemanager.ui;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 20/11/2017, at 15:26.
 */
public class CompressedObjectParcelable implements Parcelable {
    public static final int TYPE_GOBACK = -1, TYPE_NORMAL = 0;

    private final boolean directory;
    private final int type;
    private final String name;
    private final long date, size;

    public CompressedObjectParcelable(String name, long date, long size, boolean directory) {
        this.directory = directory;
        this.type = TYPE_NORMAL;
        this.name = name;
        this.date = date;
        this.size = size;
    }

    /**
     * TYPE_GOBACK instance
     */
    public CompressedObjectParcelable() {
        this.directory = true;
        this.type = TYPE_GOBACK;
        this.name = null;
        this.date = 0;
        this.size = 0;
    }

    public int getType() {
        return type;
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
        p1.writeInt(type);
        if(type != TYPE_GOBACK) {
            p1.writeInt(directory? 1:0);
            p1.writeString(name);
            p1.writeLong(size);
            p1.writeLong(date);
        }
    }

    public static final Parcelable.Creator<CompressedObjectParcelable> CREATOR =
            new Parcelable.Creator<CompressedObjectParcelable>() {
                public CompressedObjectParcelable createFromParcel(Parcel in) {
                    return new CompressedObjectParcelable(in);
                }

                public CompressedObjectParcelable[] newArray(int size) {
                    return new CompressedObjectParcelable[size];
                }
            };

    private CompressedObjectParcelable(Parcel im) {
        type = im.readInt();
        if(type == TYPE_GOBACK) {
            directory = true;
            name = null;
            date = 0;
            size = 0;
        } else {
            directory = im.readInt() == 1;
            name = im.readString();
            size = im.readLong();
            date = im.readLong();
        }
    }

}
