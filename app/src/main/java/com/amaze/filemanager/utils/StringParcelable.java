package com.amaze.filemanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Vishal on 12-10-2017.
 */

public class StringParcelable implements Parcelable {

    private String value;

    public StringParcelable(String string) {
        value = string;
    }

    protected StringParcelable(Parcel in) {
        value = in.readString();
    }

    public static final Creator<StringParcelable> CREATOR = new Creator<StringParcelable>() {
        @Override
        public StringParcelable createFromParcel(Parcel in) {
            return new StringParcelable(in);
        }

        @Override
        public StringParcelable[] newArray(int size) {
            return new StringParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
