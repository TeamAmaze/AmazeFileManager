package com.amaze.filemanager.database.models;


public class SSHOperationData implements OperationData{
    private String name;
    private String path;
    private String hostKey;
    private String sshKeyName;
    private String sshKey;


    public SSHOperationData() {
    }

    public SSHOperationData(String name, String path, String hostKey, String sshKeyName, String sshKey) {
        this.name = name;
        this.path = path;
        this.hostKey = hostKey;
        this.sshKeyName = sshKeyName;
        this.sshKey = sshKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHostKey() {
        return hostKey;
    }

    public void setHostKey(String hostKey) {
        this.hostKey = hostKey;
    }

    public String getSshKeyName() {
        return sshKeyName;
    }

    public void setSshKeyName(String sshKeyName) {
        this.sshKeyName = sshKeyName;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }
}
