package com.amaze.filemanager.adapters.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.amaze.filemanager.ui.icons.Icons;

import java.util.Comparator;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 20/11/2017, at 15:26.
 */
public class CompressedObjectParcelable implements Parcelable {
    public static final int TYPE_GOBACK = -1, TYPE_NORMAL = 0;

    public final boolean directory;
    public final int type;
    public final String name;
    public final long date, size;
    public final int filetype;
    public final IconDataParcelable iconData;

    public CompressedObjectParcelable(String name, long date, long size, boolean directory) {
        this.directory = directory;
        this.type = TYPE_NORMAL;
        this.name = name;
        this.date = date;
        this.size = size;
        this.filetype = Icons.getTypeOfFile(name, directory);
        this.iconData = new IconDataParcelable(IconDataParcelable.IMAGE_RES,
                Icons.loadMimeIcon(name, directory));
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
        this.filetype = -1;
        this.iconData = null;
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
            p1.writeInt(filetype);
            p1.writeParcelable(iconData, 0);
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
            filetype = -1;
            iconData = null;
        } else {
            directory = im.readInt() == 1;
            name = im.readString();
            size = im.readLong();
            date = im.readLong();
            filetype = im.readInt();
            iconData = im.readParcelable(IconDataParcelable.class.getClassLoader());
        }
    }

    public static class Sorter implements Comparator<CompressedObjectParcelable> {
        @Override
        public int compare(CompressedObjectParcelable file1, CompressedObjectParcelable file2) {
            if(file1.type == CompressedObjectParcelable.TYPE_GOBACK) return -1;
            else if(file2.type == CompressedObjectParcelable.TYPE_GOBACK) return 1;
            else if (file1.directory && !file2.directory) {
                return -1;
            } else if (file2.directory && !(file1).directory) {
                return 1;
            } else return file1.name.compareToIgnoreCase(file2.name);
        }

    }

}
