package com.amaze.filemanager.database.models;

import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.UtilsHandler.Operation;

import static com.amaze.filemanager.database.UtilsHandler.Operation.BOOKMARKS;
import static com.amaze.filemanager.database.UtilsHandler.Operation.GRID;
import static com.amaze.filemanager.database.UtilsHandler.Operation.HIDDEN;
import static com.amaze.filemanager.database.UtilsHandler.Operation.HISTORY;
import static com.amaze.filemanager.database.UtilsHandler.Operation.LIST;
import static com.amaze.filemanager.database.UtilsHandler.Operation.SFTP;
import static com.amaze.filemanager.database.UtilsHandler.Operation.SMB;

public class OperationData {
    public final Operation type;
    public final String path;
    public final String name;
    public final String hostKey;
    public final String sshKeyName;
    public final String sshKey;

    /**
     * Constructor for types {@link Operation#HIDDEN}, {@link Operation#HISTORY},
     * {@link Operation#LIST} or {@link Operation#GRID}
     */
    public OperationData(Operation type, String path) {
        if(type != HIDDEN && type != HISTORY && type != LIST && type != GRID) {
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
     * Constructor for types {@link Operation#BOOKMARKS} or {@link Operation#SMB}
     */
    public OperationData(Operation type, String name, String path) {
        if(type != BOOKMARKS && type != SMB) throw new IllegalArgumentException("Wrong constructor for object type");

        this.type = type;
        this.path = path;
        this.name = name;

        hostKey = null;
        sshKeyName = null;
        sshKey = null;
    }

    /**
     * Constructor for {@link Operation#SFTP}
     * {@param hostKey}, {@param sshKeyName} and {@param sshKey} may be null for when
     * {@link OperationData} is used for {@link UtilsHandler#removeFromDatabase(OperationData)}
     */
    public OperationData(Operation type, String path, String name, String hostKey, String sshKeyName,
                         String sshKey) {
        if(type != SFTP) throw new IllegalArgumentException("Wrong constructor for object type");

        this.type = type;
        this.path = path;
        this.name = name;
        this.hostKey = hostKey;
        this.sshKeyName = sshKeyName;
        this.sshKey = sshKey;
    }

}
