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

        String strippedPath = stripPath(openMode, path);

        for (CloudMetaData cloudMetaData : cloudStorage.getChildren(strippedPath)) {

            BaseFile baseFile = new BaseFile(path + "/" + cloudMetaData.getName(),
                    "", (cloudMetaData.getModifiedAt() == null)
                    ? 0l : cloudMetaData.getModifiedAt(), cloudMetaData.getSize(),
                    cloudMetaData.getFolder());
            baseFile.setName(cloudMetaData.getName());
            baseFile.setMode(openMode);
            baseFiles.add(baseFile);
        }
        return baseFiles;
    }

    /**
     * Strips down the cloud path to remove any prefix
     * @param openMode
     * @return
     */
    public static String stripPath(OpenMode openMode, String path) {
        String strippedPath = path;
        switch (openMode) {
            case DROPBOX:
                if (path.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX + "/", "");
                }
                break;
            case BOX:
                if (path.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX + "/", "");
                }
                break;
            case ONEDRIVE:
                if (path.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/", "");
                }
                break;
            case GDRIVE:
                if (path.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/", "");
                }
                break;
            default:
                break;
        }
        return strippedPath;
    }
}
