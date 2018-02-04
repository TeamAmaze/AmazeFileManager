package com.amaze.filemanager.filesystem.compressed;

import android.content.Context;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.GzipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.GzipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.ZipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarDecompressor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.utils.Utils;

import java.io.File;

/**
 * @author Emmanuel
 *         on 23/11/2017, at 17:46.
 */

public class CompressedHelper {

    public static final String fileExtensionZip = "zip", fileExtensinJar = "jar", fileExtensionApk = "apk";
    public static final String fileExtensionTar = "tar";
    public static final String fileExtensionGzipTar = "tar.gz";
    public static final String fileExtensionRar = "rar";

    /**
     * To add compatibility with other compressed file types edit this method
     */
    public static Extractor getExtractorInstance(Context context, File file, String outputPath,
                                                 Extractor.OnUpdate listener) {
        Extractor extractor;
        String type = getExtension(file.getPath());

        if (isZip(type)) {
            extractor = new ZipExtractor(context, file.getPath(), outputPath, listener);
        } else if (isRar(type)) {
            extractor = new RarExtractor(context, file.getPath(), outputPath, listener);
        } else if(isTar(type)) {
            extractor = new TarExtractor(context, file.getPath(), outputPath, listener);
        } else if(isGzippedTar(type)) {
            extractor = new GzipExtractor(context, file.getPath(), outputPath, listener);
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
        String type = getExtension(file.getPath());

        if (isZip(type)) {
            decompressor = new ZipDecompressor(context);
        } else if (isRar(type)) {
            decompressor = new RarDecompressor(context);
        } else if(isTar(type)) {
            decompressor = new TarDecompressor(context);
        } else if(isGzippedTar(type)) {
            decompressor = new GzipDecompressor(context);
        } else {
            return null;
        }

        decompressor.setFilePath(file.getPath());
        return decompressor;
    }

    public static boolean isFileExtractable(String path) {
        String type = getExtension(path);

        return isZip(type) || isTar(type) || isRar(type) || isGzippedTar(type);
    }

    /**
     * Gets the name of the file without compression extention.
     * For example:
     * "s.tar.gz" to "s"
     * "s.tar" to "s"
     */
    public static String getFileName(String compressedName) {
        compressedName = compressedName.toLowerCase();
        if(isZip(compressedName) || isTar(compressedName) || isRar(compressedName)) {
            return compressedName.substring(0, compressedName.lastIndexOf("."));
        } else if (isGzippedTar(compressedName)) {
            return compressedName.substring(0,
                    Utils.nthToLastCharIndex(2, compressedName, '.'));
        } else {
            return compressedName;
        }
    }

    private static boolean isZip(String type) {
        return type.endsWith(fileExtensionZip) || type.endsWith(fileExtensinJar)
                || type.endsWith(fileExtensionApk);
    }

    private static boolean isTar(String type) {
         return type.endsWith(fileExtensionTar);
    }

    private static boolean isGzippedTar(String type) {
         return type.endsWith(fileExtensionGzipTar);
    }

    private static boolean isRar(String type) {
        return type.endsWith(fileExtensionRar);
    }

    private static String getExtension(String path) {
        return path.substring(path.indexOf('.')+1, path.length()).toLowerCase();
    }

}
