package com.amaze.filemanager.database.models;


public class SSHOperationData implements OperationData{
    public final String name;
    public final String path;
    public final String hostKey;
    public final String sshKeyName;
    public final String sshKey;

    public SSHOperationData(String name, String path, String hostKey, String sshKeyName, String sshKey) {
        this.name = name;
        this.path = path;
        this.hostKey = hostKey;
        this.sshKeyName = sshKeyName;
        this.sshKey = sshKey;
    }
}
