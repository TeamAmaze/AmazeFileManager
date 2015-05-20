/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils;

import java.io.File;
import java.util.Comparator;

public class FileListSorter implements Comparator<Layoutelements> {


    private int dirsOnTop = 0;

    private int asc = 1;
    int sort = 0;
boolean rootMode;
    public FileListSorter(int dir, int sort, int asc,boolean rootMode) {
        this.dirsOnTop = dir;
        this.asc = asc;
        this.sort = sort;
        this.rootMode=rootMode;
    }

     boolean isDirectory(Layoutelements path){
    return path.isDirectory();}
    @Override
    public int compare(Layoutelements file1, Layoutelements file2) {
File f1;if(!file1.hasSymlink()){
        f1=new File(file1.getDesc());}else {  f1=new File(file1.getSymlink());}
        File f2;if(!file2.hasSymlink()){
            f2=new File(file2.getDesc());}else {  f2=new File(file1.getSymlink());}
        if (dirsOnTop == 0) {
            if (isDirectory(file1) && !isDirectory(file2)) {
                return -1;


            } else if (isDirectory(file2) && !isDirectory(file1)) {
                return 1;
            } //else {return 1;}
        } else if (dirsOnTop == 1) {
            if (isDirectory(file1) && !isDirectory(file2)) {
                return 1;


            } else if (isDirectory(file2) && !isDirectory(file1)) {
                return -1;
            }else{return 1;}
        } else {
        }

        if (sort == 0) {
            return asc * file1.getTitle().compareToIgnoreCase(file2.getTitle());
        } else if (sort == 1) {
            return asc * Long.valueOf(file1.getDate1()).compareTo(Long.valueOf(file2.getDate1()));
        } else if (sort == 2) {
            if (f1.isFile() && f2.isFile()) {
                return asc * Long.valueOf(f1.length()).compareTo(Long.valueOf(f2.length()));
            } else {
                return file1.getTitle().compareToIgnoreCase(file2.getTitle());
            }
        }
        else if(sort ==3){
if(f1.isFile() && f2.isFile()){
            final String ext_a = getExtension(file1.getTitle());
            final String ext_b = getExtension(file2.getTitle());


            final int res = asc*ext_a.compareTo(ext_b);
            if (res == 0) {
                return asc * file1.getTitle().compareToIgnoreCase(file2.getTitle());
            }
            return res;}
            else{return  file1.getTitle().compareToIgnoreCase(file2.getTitle());}
        }


        return 0;

    }

     static String getExtension(String a) {
        return a.substring(a.lastIndexOf(".") + 1).toLowerCase();
    }

}
