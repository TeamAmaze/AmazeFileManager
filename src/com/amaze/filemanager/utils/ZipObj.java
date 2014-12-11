package com.amaze.filemanager.utils;

import java.util.zip.ZipEntry;

/**
 * Created by Arpit on 11-12-2014.
 */
public class ZipObj {
    boolean directory;
    ZipEntry entry;
    public ZipObj(ZipEntry entry,boolean directory){
        this.directory=directory;
        this.entry=entry;
    }
    public ZipEntry getEntry(){return entry;}
    public boolean isDirectory(){return directory;}

    public String getName(){return entry.getName();}

    public long getSize(){return entry.getSize();}

    public long getTime(){return entry.getTime();}
}
