package com.amaze.filemanager.filesystem.compressed;

import android.content.Context;

import com.amaze.filemanager.filesystem.compressed.helpers.RarHelper;
import com.amaze.filemanager.filesystem.compressed.helpers.ZipHelper;

import java.io.File;

/**
 * @author Emmanuel
 *         on 23/11/2017, at 17:46.
 */

public class CompressedHelper {

    /**
     * To add compatibility with other compressed file types edit this method
     */
    public static CompressedInterface getCompressedInterfaceInstance(Context context, File file) {
        CompressedInterface compressedInterface;

        String path = file.getPath().toLowerCase();
        boolean isZip = path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk");
        boolean isTar = path.endsWith(".tar") || path.endsWith(".tar.gz");
        boolean isRar = path.endsWith(".rar");

        if (isZip || isTar) {
            compressedInterface = new ZipHelper(context);
        } else if (isRar) {
            compressedInterface = new RarHelper(context);
        } else {
            return null;
        }

        compressedInterface.setFilePath(file.getPath());
        return compressedInterface;
    }

}
