package com.amaze.filemanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by arpitkh996 on 16-01-2016.
 */
public class ComputerParcelable implements Parcelable {

    public final String addr;
    public final String name;

    public ComputerParcelable(String str, String str2) {
        this.name = str;
        this.addr = str2;
    }

    public static final Creator<ComputerParcelable> CREATOR = new Creator<ComputerParcelable>() {
        @Override
        public ComputerParcelable createFromParcel(Parcel in) {
            return new ComputerParcelable(in);
        }

        @Override
        public ComputerParcelable[] newArray(int size) {
            return new ComputerParcelable[size];
        }
    };

    public String toString() {
        return String.format("%s [%s]", this.name, this.addr);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeString(this.addr);
    }

    public boolean equals(Object obj) {
        return obj instanceof ComputerParcelable
                && (this == obj || (this.name.equals(((ComputerParcelable) obj).name)
                && this.addr.equals(((ComputerParcelable) obj).addr)));
    }

    public int hashCode() {
        return this.name.hashCode() + this.addr.hashCode();
    }

    private ComputerParcelable(Parcel parcel) {
        this.name = parcel.readString();
        this.addr = parcel.readString();
    }

}
