package com.amaze.filemanager.services.asynctasks;

/**
 * Created by Arpit on 25-01-2015.
 */
import android.os.AsyncTask;

import com.amaze.filemanager.fragments.RarViewer;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Vishal on 11/23/2014.
 */
public class RarHelperTask extends AsyncTask<File, Void, ArrayList<FileHeader>> {

    RarViewer zipViewer;
    String dir;

    public RarHelperTask(RarViewer zipViewer, String dir) {

        this.zipViewer = zipViewer;
        this.dir = dir;
    }

    @Override
    protected ArrayList<FileHeader> doInBackground(File... params) {
        ArrayList<FileHeader> elements = new ArrayList<FileHeader>();

        try {
            Archive zipfile = new Archive(params[0]);
            zipViewer.archive=zipfile;
            if (zipViewer.wholelist.size() == 0) {

                FileHeader fh = zipfile.nextFileHeader();
                while (fh != null) {
                    zipViewer.wholelist.add(fh);
                    fh = zipfile.nextFileHeader();
                }
            }
            if(dir==null || dir.trim().length()==0 || dir.equals("")){

            for(FileHeader header:zipViewer.wholelist){
                String name=header.getFileNameString();

                if(!name.contains("\\")){
                    elements.add(header);

                }
            }}else{
                for(FileHeader header:zipViewer.wholelist){
                    String name=header.getFileNameString();
                    if(name.substring(0,name.lastIndexOf("\\")).equals(dir)){
                        elements.add(header);
                    }
                }
            }
        }catch (Exception e){}
        return elements;}

    @Override
    protected void onPostExecute (ArrayList < FileHeader > zipEntries) {
        super.onPostExecute(zipEntries);
        //zipViewer.elements=zipEntries;
        Collections.sort(zipViewer.elements,new FileListSorter());
    zipViewer.createviews(zipEntries,dir);}
    class FileListSorter implements Comparator<FileHeader> {


        public FileListSorter() {

        }

        @Override
        public int compare(FileHeader file1, FileHeader file2) {
            if (file1.isDirectory() && !file2.isDirectory()) {
                return -1;


            } else if (file2.isDirectory() && !(file1).isDirectory()) {
                return 1;
            }
            return file1.getFileNameString().compareToIgnoreCase(file2.getFileNameString());
        }
    }
}

