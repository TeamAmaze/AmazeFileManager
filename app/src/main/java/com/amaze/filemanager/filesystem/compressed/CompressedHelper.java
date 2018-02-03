package com.amaze.filemanager.filesystem.compressed;

import android.content.Context;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.ZipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarDecompressor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

import java.io.File;

/**
 * @author Emmanuel
 *         on 23/11/2017, at 17:46.
 */

public class CompressedHelper {

    /**
     * To add compatibility with other compressed file types edit this method
     */
    public static Extractor getExtractorInstance(Context context, File file, String outputPath,
                                                 Extractor.OnUpdate listener) {
        Extractor extractor;
        String type = file.getPath().substring(file.getPath().lastIndexOf('.')+1, file.getPath().length()).toLowerCase();

        if (isZip(type)) {
            extractor = new ZipExtractor(context, file.getPath(), outputPath, listener);
        } else if (isRar(type)) {
            extractor = new RarExtractor(context, file.getPath(), outputPath, listener);
        } else if(isTar(type)) {
            extractor = new TarExtractor(context, file.getPath(), outputPath, listener);
        } else {
            return null;
        }

        return extractor;
    }

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
