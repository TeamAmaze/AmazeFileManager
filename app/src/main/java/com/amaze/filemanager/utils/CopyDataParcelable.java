package com.amaze.filemanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.fragments.ProcessViewerFragment;

/**
 * Created by Arpit on 01-08-2015.
 *
 * Class stores the {@link CopyService} progress variables.
 * This class also acts as a middle layer to communicate with
 * {@link ProcessViewerFragment}
 */
public class CopyDataParcelable implements Parcelable {

    // which file is being copied from total number of files
    public final int sourceProgress;

    // current byte position in total bytes pool
    public final long byteProgress;

    // total number of source files to be copied
    public final int sourceFiles;

    // total size of all source files combined
    public final long totalSize;

    // bytes being copied per sec
    public final int speedRaw;

    public final boolean completed, move;

    // name of source file being copied
    public final String name;

    public CopyDataParcelable(String name, int amountOfSourceFiles, long totalSize, boolean move) {
        this.name = name;
        sourceFiles = amountOfSourceFiles;
        this.totalSize = totalSize;
        this.move = move;

        speedRaw = 0;
        sourceProgress = 0;
        byteProgress = 0;
        completed = false;
    }

    public CopyDataParcelable(String name, int amountOfSourceFiles, int sourceProgress,
                              long totalSize, long byteProgress, int speedRaw, boolean move,
                              boolean completed) {
        this.name = name;
        sourceFiles = amountOfSourceFiles;
        this.sourceProgress = sourceProgress;
        this.totalSize = totalSize;
        this.byteProgress = byteProgress;
        this.speedRaw = speedRaw;
        this.move = move;
        this.completed = completed;
    }

    protected CopyDataParcelable(Parcel in) {
        sourceProgress = in.readInt();
        byteProgress = in.readLong();
        sourceFiles = in.readInt();
        totalSize = in.readLong();
        completed = in.readByte() != 0;
        move = in.readByte() != 0;
        name = in.readString();
        speedRaw = in.readInt();
    }

    public static final Creator<CopyDataParcelable> CREATOR = new Creator<CopyDataParcelable>() {
        @Override
        public CopyDataParcelable createFromParcel(Parcel in) {
            return new CopyDataParcelable(in);
        }

        @Override
        public CopyDataParcelable[] newArray(int size) {
            return new CopyDataParcelable[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(sourceProgress);
        dest.writeLong(byteProgress);
        dest.writeInt(sourceFiles);
        dest.writeLong(totalSize);
        dest.writeByte((byte) (completed ? 1 : 0));
        dest.writeByte((byte) (move ? 1 : 0));
        dest.writeString(name);
        dest.writeInt(speedRaw);
    }
}
