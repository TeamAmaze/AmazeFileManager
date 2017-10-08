package com.amaze.filemanager.filesystem;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Special immutable class for handling cut/copy operations.
 *
 * @author Emmanuel
 *         on 5/9/2017, at 09:59.
 */

public final class PasteHelper implements Parcelable {

    public static final int OPERATION_COPY = 0, OPERATION_CUT = 1;

    public final int operation;
    public final HybridFileParcelable[] paths;

    public PasteHelper(int op, HybridFileParcelable[] paths) {
        if(paths == null || paths.length == 0) throw new IllegalArgumentException();
        operation = op;
        this.paths = paths;
    }

    private PasteHelper(Parcel in) {
        operation = in.readInt();
        paths = (HybridFileParcelable[]) in.readParcelableArray(HybridFileParcelable.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(operation);
        dest.writeParcelableArray(paths, 0);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public PasteHelper createFromParcel(Parcel in) {
            return new PasteHelper(in);
        }

        public PasteHelper[] newArray(int size) {
            return new PasteHelper[size];
        }
    };

}
