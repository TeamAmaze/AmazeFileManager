package com.amaze.filemanager.database.models;

import com.amaze.filemanager.database.UtilsHandler;

import static com.amaze.filemanager.database.UtilsHandler.OPERATION_BOOKMARKS;
import static com.amaze.filemanager.database.UtilsHandler.OPERATION_GRID;
import static com.amaze.filemanager.database.UtilsHandler.OPERATION_HIDDEN;
import static com.amaze.filemanager.database.UtilsHandler.OPERATION_HISTORY;
import static com.amaze.filemanager.database.UtilsHandler.OPERATION_LIST;
import static com.amaze.filemanager.database.UtilsHandler.OPERATION_SFTP;
import static com.amaze.filemanager.database.UtilsHandler.OPERATION_SMB;

public class OperationData {
    public final int type;
    public final String path;
    public final String name;
    public final String hostKey;
    public final String sshKeyName;
    public final String sshKey;

    /**
     * Constructor for types {@link UtilsHandler#OPERATION_HIDDEN}, {@link UtilsHandler#OPERATION_HISTORY},
     * {@link UtilsHandler#OPERATION_LIST} or {@link UtilsHandler#OPERATION_GRID}
     */
    public OperationData(int type, String path) {
        if(type != OPERATION_HIDDEN && type != OPERATION_HISTORY && type != OPERATION_LIST && type != OPERATION_GRID) {
            throw new IllegalArgumentException("Wrong constructor for object type");
        }

        this.type = type;
        this.path = path;

        name = null;
        hostKey = null;
        sshKeyName = null;
        sshKey = null;
    }

    /**
     * Constructor for types {@link UtilsHandler#OPERATION_BOOKMARKS} or {@link UtilsHandler#OPERATION_SMB}
     */
    public OperationData(int type, String path, String name) {
        if(type != OPERATION_BOOKMARKS && type != OPERATION_SMB) {
            throw new IllegalArgumentException("Wrong constructor for object type");
        }

        this.type = type;
        this.path = path;
        this.name = name;

        hostKey = null;
        sshKeyName = null;
        sshKey = null;
    }

    /**
     * Constructor for {@link UtilsHandler#OPERATION_SFTP}
     */
    public OperationData(int type, String path, String name, String hostKey, String sshKeyName,
                         String sshKey) {
        if(type != OPERATION_SFTP) throw new IllegalArgumentException("Wrong constructor for object type");

        this.type = type;
        this.path = path;
        this.name = name;
        this.hostKey = hostKey;
        this.sshKeyName = sshKeyName;
        this.sshKey = sshKey;
    }

}
