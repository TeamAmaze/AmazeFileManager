package com.amaze.filemanager.adapters.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.io.File;

/**
 * Identifies a mounted volume
 */
public class StorageDirectoryParcelable implements Parcelable {
    public final String path;
    public final String name;
    public final @DrawableRes int iconRes;

    public StorageDirectoryParcelable(String path, String name, int iconRes) {
        this.path = path;
        this.name = name;
        this.iconRes = iconRes;
    }

    public StorageDirectoryParcelable(Parcel im) {
        path = im.readString();
        name = im.readString();
        iconRes = im.readInt();
    }

    @NonNull
    @Override
    public String toString() {
        return "StorageDirectory(path=" + path + ", name=" + name + ", icon=" + iconRes + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(path);
        parcel.writeString(name);
        parcel.writeInt(iconRes);
    }

    public static final Creator<StorageDirectoryParcelable> CREATOR =
            new Creator<StorageDirectoryParcelable>() {
                public StorageDirectoryParcelable createFromParcel(Parcel in) {
                    return new StorageDirectoryParcelable(in);
                }

                public StorageDirectoryParcelable[] newArray(int size) {
                    return new StorageDirectoryParcelable[size];
                }
            };

}
