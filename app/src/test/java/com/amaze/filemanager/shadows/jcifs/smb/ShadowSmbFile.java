package com.amaze.filemanager.shadows.jcifs.smb;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

@Implements(SmbFile.class)
public class ShadowSmbFile {

    private File file = null;

    @Implementation
    public void __constructor__(URL url, NtlmPasswordAuthentication auth) {
        //intentionally empty
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Implementation
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Implementation
    public long length() throws SmbException {
        return file.length();
    }
}