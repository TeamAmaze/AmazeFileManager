package com.amaze.filemanager.adapters.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 7/12/2017, at 17:24.
 */

public final class AppDataParcelable implements Parcelable {

    public final String label, path, packageName, data, fileSize;
    public final long size, lastModification;

    public AppDataParcelable(String label, String path, String packageName, String data,
                             String fileSize, long size, long lastModification) {
        this.label = label;
        this.path = path;
        this.packageName = packageName;
        this.data = data;
        this.fileSize = fileSize;
        this.size = size;
        this.lastModification = lastModification;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(label);
        parcel.writeString(path);
        parcel.writeString(packageName);
        parcel.writeString(data);
        parcel.writeString(fileSize);
        parcel.writeLong(size);
        parcel.writeLong(lastModification);
    }

    private AppDataParcelable(Parcel in) {
        this.label = in.readString();
        this.path = in.readString();
        this.packageName = in.readString();
        this.data = in.readString();
        this.fileSize = in.readString();
        this.size = in.readLong();
        this.lastModification = in.readLong();
    }

    public static final Creator<AppDataParcelable> CREATOR = new Creator<AppDataParcelable>() {
        @Override
        public AppDataParcelable createFromParcel(Parcel in) {
            return new AppDataParcelable(in);
        }

        @Override
        public AppDataParcelable[] newArray(int size) {
            return new AppDataParcelable[size];
        }
    };

    public static final class AppDataSorter implements Comparator<AppDataParcelable> {
        public static final int SORT_NAME = 0, SORT_MODIF = 1, SORT_SIZE = 2;

        private int asc = 1;
        private int sort = 0;

        public AppDataSorter(int sort, int asc) {
            this.asc = asc;
            this.sort = sort;
        }

        /**
         * Compares two elements and return negative, zero and positive integer if first argument is
         * less than, equal to or greater than second
         */
        @Override
        public int compare(AppDataParcelable file1, AppDataParcelable file2) {
            if (sort == SORT_NAME) {
                // sort by name
                return asc * file1.label.compareToIgnoreCase(file2.label);
            } else if (sort == SORT_MODIF) {
                // sort by last modified
                return asc * Long.valueOf(file1.lastModification).compareTo(file2.lastModification);
            } else if (sort == SORT_SIZE) {
                // sort by size
                return asc * Long.valueOf(file1.size).compareTo(file2.size);
            }
            return 0;
        }
    }

}
