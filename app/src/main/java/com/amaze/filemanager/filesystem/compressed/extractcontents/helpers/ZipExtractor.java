package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipExtractor extends Extractor {

    public ZipExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        long totalBytes = 0;
        ArrayList<ZipEntry> entry1 = new ArrayList<>();
        ZipFile zipfile = new ZipFile(filePath);

        // iterating archive elements to find file names that are to be extracted
        for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry zipEntry = e.nextElement();

            if(filter.shouldExtract(zipEntry.getName(), zipEntry.isDirectory())) {
                entry1.add(zipEntry);
                totalBytes += zipEntry.getSize();
            }
        }

        listener.onStart(totalBytes, entry1.get(0).getName());

        for (ZipEntry entry : entry1) {
            if (!listener.isCancelled()) {
                listener.onUpdate(entry.getName());
                unzipEntry(context, zipfile, entry, outputPath);
            }
        }
        listener.onFinish();
    }
    
    /**
     * Method extracts {@link ZipEntry} from {@link ZipFile}
     *
     * @param zipFile   zip file from which entriesToExtract are to be extracted
     * @param entry     zip entry that is to be extracted
     * @param outputDir output directory
     */
    private void unzipEntry(@NonNull final Context context, ZipFile zipFile, ZipEntry entry,
                            String outputDir) throws IOException {
        if (entry.isDirectory()) {
            // zip entry is a directory, return after creating new directory
            FileUtil.mkdir(new File(outputDir, entry.getName()), context);
            return;
        }

        final File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            // creating directory if not already exists
            FileUtil.mkdir(outputFile.getParentFile(), context);
        }

        BufferedInputStream inputStream = new BufferedInputStream(
                zipFile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
        try {
            int len;
            byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                ServiceWatcherUtil.POSITION += len;
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

}
