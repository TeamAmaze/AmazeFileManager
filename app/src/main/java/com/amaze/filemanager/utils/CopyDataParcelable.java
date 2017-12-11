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
    int sourceProgress;

    // current byte position in total bytes pool
    long byteProgress;

    // total number of source files to be copied
    int sourceFiles;

    // total size of all source files combined
    long totalSize;

    // bytes being copied per sec
    int speedRaw;

    boolean completed=false,move=false;

    // name of source file being copied
    String name;

    public CopyDataParcelable(){}

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getByteProgress() {
        return byteProgress;
    }

    public void setByteProgress(long byteProgress) {
        this.byteProgress = byteProgress;
    }

    public boolean isMove() {
        return move;
    }

    public void setMove(boolean move) {
        this.move = move;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getTotal() {
        return totalSize;
    }

    public void setTotal(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getSourceProgress() {
        return sourceProgress;
    }

    public void setSourceProgress(int progress) {
        this.sourceProgress = progress;
    }

    public void setSourceFiles(int sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public int getSourceFiles() {
        return this.sourceFiles;
    }

    public void setSpeedRaw(int speedRaw) {
        this.speedRaw = speedRaw;
    }

    public int getSpeedRaw() {
        return this.speedRaw;
    }

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
