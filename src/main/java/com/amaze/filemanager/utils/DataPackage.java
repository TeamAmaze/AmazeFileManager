package com.amaze.filemanager.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Arpit on 01-08-2015.
 *
 * Class stores the {@link com.amaze.filemanager.services.CopyService} progress variables.
 * This class also acts as a middle layer to communicate with
 * {@link com.amaze.filemanager.fragments.ProcessViewer}
 */
public class DataPackage implements Parcelable {

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

    public DataPackage(){}

    protected DataPackage(Parcel in) {
        sourceProgress = in.readInt();
        byteProgress = in.readLong();
        sourceFiles = in.readInt();
        totalSize = in.readLong();
        completed = in.readByte() != 0;
        move = in.readByte() != 0;
        name = in.readString();
        speedRaw = in.readInt();
    }

    public static final Creator<DataPackage> CREATOR = new Creator<DataPackage>() {
        @Override
        public DataPackage createFromParcel(Parcel in) {
            return new DataPackage(in);
        }

        @Override
        public DataPackage[] newArray(int size) {
            return new DataPackage[size];
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
