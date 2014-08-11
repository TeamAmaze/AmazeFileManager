package com.amaze.filemanager.utils;

import java.io.File;
import java.util.Comparator;

public class FileListSorter implements Comparator<Layoutelements> {


    private int dirsOnTop = 0;

    private int asc = 1;
    int sort = 0;

    public FileListSorter(int dir, int sort, int asc) {
        this.dirsOnTop = dir;
        this.asc = asc;
        this.sort = sort;

    }

    @Override
    public int compare(Layoutelements file1, Layoutelements file2) {
File f1=new File(file1.getDesc());
        File f2=new File(file2.getDesc());
        if (dirsOnTop == 0) {
            if (f1.isDirectory() && f2.isFile()) {
                return -1;


            } else if (f2.isDirectory() && (f1).isFile()) {
                return 1;
            }
        } else if (dirsOnTop == 1) {
            if (f1.isDirectory() && f2.isFile()) {
                return 1;


            } else if (f2.isDirectory() && (f1).isFile()) {
                return -1;
            }
        } else {
        }

        if (sort == 0) {
            return asc * f1.getName().compareToIgnoreCase(f2.getName());
        } else if (sort == 1) {
            return asc * Long.valueOf(f1.lastModified()).compareTo(Long.valueOf(f2.lastModified()));
        } else if (sort == 2) {
            if (f1.isFile() && f2.isFile()) {
                return asc * Long.valueOf(f1.length()).compareTo(Long.valueOf(f2.length()));
            } else {
                return 1 * f1.getName().compareToIgnoreCase(f2.getName());
            }
        }


        return 0;

    }


}
