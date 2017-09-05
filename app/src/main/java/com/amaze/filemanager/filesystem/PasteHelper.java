package com.amaze.filemanager.filesystem;

/**
 * Special immutable class for handling cut/copy operations.
 *
 * @author Emmanuel
 *         on 5/9/2017, at 09:59.
 */

public final class PasteHelper {

    public static final int OPERATION_COPY = 0, OPERATION_CUT = 1;

    public final int operation;
    public final HybridFileParcelable[] paths;

    public PasteHelper(int op, HybridFileParcelable[] paths) {
        if(paths == null || paths.length == 0) throw new IllegalArgumentException();
        operation = op;
        this.paths = paths;
    }

}
