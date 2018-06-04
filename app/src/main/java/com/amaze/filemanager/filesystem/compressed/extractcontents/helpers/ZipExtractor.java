package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*  Rename : extractWithFilter(@NonNull Filter filter)
        entry and entry1 is not good for understandability.
        we can not understand meaning of entry1.
        entry -> entryIndex, entry1 -> entryArray
*/
/*  Rename : extractEntry(@NonNull final Context context, ZipFile zipFile, ZipEntry entry, String outputDirectory)
        Although there is parameter explanation, outputDir is not good for readability.
        Dir can be directory, direction and so on.
        outputDir -> outputDirectory

        len and buf is not good for understandability.
        I think full word is better.
        buf -> buffer, len -> length
*/

public class ZipExtractor extends Extractor {

    public ZipExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        long totalBytes = 0;
        ArrayList<ZipEntry> entryArray = new ArrayList<>();
        ZipFile zipfile = new ZipFile(filePath);

        // iterating archive elements to find file names that are to be extracted
        for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry zipEntry = e.nextElement();

            if(filter.shouldExtract(zipEntry.getName(), zipEntry.isDirectory())) {
                entryArray.add(zipEntry);
                totalBytes += zipEntry.getSize();
            }
        }

        listener.onStart(totalBytes, entryArray.get(0).getName());

        for (ZipEntry entryIndex : entryArray) {
            if (!listener.isCancelled()) {
                listener.onUpdate(entryIndex.getName());
                extractEntry(context, zipfile, entryIndex, outputPath);
            }
        }
        listener.onFinish();
    }

    @Override
    protected void streamClose(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        outputStream.close();
        inputStream.close();
    }

    /**
     * Method extracts {@link ZipEntry} from {@link ZipFile}
     *
     * @param zipFile   zip file from which entriesToExtract are to be extracted
     * @param entry     zip entry that is to be extracted
     * @param outputDirectory output directory
     */
    private void extractEntry(@NonNull final Context context, ZipFile zipFile, ZipEntry entry,
                              String outputDirectory) throws IOException {
        if (entry.isDirectory()) {
            // zip entry is a directory, return after creating new directory
            FileUtil.mkdir(new File(outputDirectory, entry.getName()), context);
            return;
        }

        final File outputFile = getOutputFile(context, outputDirectory, entry.getName());

        BufferedInputStream inputStream = new BufferedInputStream(
                zipFile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
        writeBuffer(inputStream, outputStream);
    }

}
