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

    /* Line arrangement - equals method : arrange condition statements
    I tied the related conditional statements together.(improved readability)
    */

    /* extract method - equals method : change condition statement
    I extracted the function for each conditional statement. (Improved understandability)
     */
    public boolean equals(Object obj) {
        return obj instanceof ComputerParcelable
                && (objectEqual(obj)
                || (computerNameEqual(obj) && computerAddressEqual(obj)));
    }

    private boolean objectEqual(Object obj) {
        return this == obj;
    }

    private boolean computerNameEqual(Object obj) {
        return this.name.equals(((ComputerParcelable) obj).name);
    }

    private boolean computerAddressEqual(Object obj) {
        return this.addr.equals(((ComputerParcelable) obj).addr);
    }

    public int hashCode() {
        return this.name.hashCode() + this.addr.hashCode();
    }

    private ComputerParcelable(Parcel parcel) {
        this.name = parcel.readString();
        this.addr = parcel.readString();
    }

}
