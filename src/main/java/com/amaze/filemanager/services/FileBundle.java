package com.amaze.filemanager.services;

import com.amaze.filemanager.filesystem.HFile;

/**
 * Created by arpitkh996 on 25-01-2016.
 */
public class FileBundle {
    private HFile file,file2;
    private boolean move;
    public FileBundle(HFile file, HFile file2,boolean move) {
        this.file = file;
        this.file2 = file2;
        this.move=move;
    }

    public HFile getFile() {
        return file;
    }

    public HFile getFile2() {
        return file2;
    }

    public boolean isMove() {
        return move;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileBundle)) {
            return false;
        }
        if (this == obj || (this.file.equals(((FileBundle) obj).getFile()) && this.file2.equals(((FileBundle) obj).getFile2()))) {
            return true;
        }
        return false;    }
}
