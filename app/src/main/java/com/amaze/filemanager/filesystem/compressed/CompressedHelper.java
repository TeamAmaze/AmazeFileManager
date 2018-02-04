package com.amaze.filemanager.filesystem.compressed;

import android.content.Context;

import com.amaze.filemanager.filesystem.compressed.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.helpers.ZipDecompressor;
import com.amaze.filemanager.filesystem.compressed.helpers.TarDecompressor;

import java.io.File;

/**
 * @author Emmanuel
 *         on 23/11/2017, at 17:46.
 */

public class CompressedHelper {

    /**
     * To add compatibility with other compressed file types edit this method
     */
    public static Decompressor getCompressorInstance(Context context, File file) {
        Decompressor decompressor;

        String path = file.getPath().toLowerCase();
        boolean isZip = path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk");
        boolean isTar = path.endsWith(".tar") || path.endsWith(".tar.gz");
        boolean isRar = path.endsWith(".rar");

        if (isZip) {
            decompressor = new ZipDecompressor(context);
        } else if (isRar) {
            decompressor = new RarDecompressor(context);
        } else if(isTar) {
            decompressor = new TarDecompressor(context);
        } else {
            return null;
        }

        decompressor.setFilePath(file.getPath());
        return decompressor;
    }

    public static boolean isFileExtractable(File file) {
        String path = file.getPath().toLowerCase();
        boolean isZip = path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk");
        boolean isTar = path.endsWith(".tar") || path.endsWith(".tar.gz");
        boolean isRar = path.endsWith(".rar");
        return isZip || isTar || isRar;
    }

}
