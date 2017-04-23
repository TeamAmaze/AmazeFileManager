package com.amaze.filemanager.utils;

import com.amaze.filemanager.filesystem.BaseFile;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;

import java.util.ArrayList;

/**
 * Created by vishal on 19/4/17.
 */

public class CloudUtil {

    public static ArrayList<BaseFile> listFiles(String path, CloudStorage cloudStorage, OpenMode openMode) {
        ArrayList<BaseFile> baseFiles = new ArrayList<>();

        //if (path.charAt(path.length()) != '/') path.concat("/");

        String strippedPath = path.substring(path.indexOf("/")+1, path.length());

        for (CloudMetaData cloudMetaData : cloudStorage.getChildren("/")) {

            BaseFile baseFile = new BaseFile(path + "/" + cloudMetaData.getName(),
                    "", 0l, cloudMetaData.getSize(),
                    cloudMetaData.getFolder());
            baseFile.setName(cloudMetaData.getName());
            baseFile.setMode(openMode);
            baseFiles.add(baseFile);
        }
        return baseFiles;
    }
}
