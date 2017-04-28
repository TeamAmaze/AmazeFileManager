package com.amaze.filemanager.utils;

import android.util.Log;

import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.BaseFile;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;

import java.util.ArrayList;

/**
 * Created by vishal on 19/4/17.
 *
 * Class provides helper methods for cloud utilities
 */

public class CloudUtil {

    public static ArrayList<BaseFile> listFiles(String path, CloudStorage cloudStorage, OpenMode openMode) {
        ArrayList<BaseFile> baseFiles = new ArrayList<>();

        String strippedPath = path;
        switch (openMode) {
            case DROPBOX:
                strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX, "");
                break;
            case BOX:
                strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX, "");
                break;
            case ONEDRIVE:
                strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE, "");
                break;
            case GDRIVE:
                strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE, "");
                break;
        }

        for (CloudMetaData cloudMetaData : cloudStorage.getChildren(strippedPath)) {

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
