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
        String type = file.getPath().substring(file.getPath().lastIndexOf('.')+1, file.getPath().length()).toLowerCase();

        if (isZip(type)) {
            decompressor = new ZipDecompressor(context);
        } else if (isRar(type)) {
            decompressor = new RarDecompressor(context);
        } else if(isTar(type)) {
            decompressor = new TarDecompressor(context);
        } else {
            return null;
        }

        decompressor.setFilePath(file.getPath());
        return decompressor;
    }

    public static boolean isFileExtractable(File file) {
        String type = file.getPath().substring(file.getPath().lastIndexOf('.')+1, file.getPath().length()).toLowerCase();

        return isZip(type) || isTar(type) || isRar(type);
    }

    private static boolean isZip(String type) {
        return type.endsWith("zip") || type.endsWith("jar") || type.endsWith("apk");
    }

    private static boolean isTar(String type) {
         return type.endsWith("tar") || type.endsWith("tar.gz");
    }

    private static boolean isRar(String type) {
        return type.endsWith("rar");
    }

}
