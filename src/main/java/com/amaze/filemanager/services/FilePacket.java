package com.amaze.filemanager.services;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;

/**
 * Created by arpitkh96 on 19/8/16.
 */
public class FilePacket {
    BaseFile source;
    HFile target;
    String id;
    boolean readDone=false,writeDone=false;
    public FilePacket(BaseFile source, HFile target, String id) {
        this.source = source;
        this.target = target;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilePacket that = (FilePacket) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isReadDone() {
        return readDone;
    }

    public void setReadDone(boolean readDone) {
        this.readDone = readDone;
    }

    public boolean isWriteDone() {
        return writeDone;
    }

    public void setWriteDone(boolean writeDone) {
        this.writeDone = writeDone;
    }
}
