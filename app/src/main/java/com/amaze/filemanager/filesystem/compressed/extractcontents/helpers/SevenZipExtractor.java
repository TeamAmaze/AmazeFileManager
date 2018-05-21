package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SevenZipExtractor extends Extractor {

    public SevenZipExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        long totalBytes = 0;
        SevenZFile sevenzFile = new SevenZFile(new File(filePath));
        ArrayList<SevenZArchiveEntry> arrayList = new ArrayList<>();

        // iterating archive elements to find file names that are to be extracted
        for (SevenZArchiveEntry entry : sevenzFile.getEntries()) {
            if (filter.shouldExtract(entry.getName(), entry.isDirectory())) {
                // Entry to be extracted is at least the entry path (may be more, when it is a directory)
                arrayList.add(entry);
                totalBytes += entry.getSize();
            }
        }

        listener.onStart(totalBytes, arrayList.get(0).getName());

        SevenZArchiveEntry entry;
        while ((entry = sevenzFile.getNextEntry()) != null) {
            if (!arrayList.contains(entry)) {
                continue;
            }
            if (!listener.isCancelled()) {
                listener.onUpdate(entry.getName());
                extractEntry(context, sevenzFile, entry, outputPath);
            }
        }
        sevenzFile.close();
        listener.onFinish(); 
    }

    private void extractEntry(@NonNull final Context context, SevenZFile sevenzFile, SevenZArchiveEntry entry, String outputDir)
            throws IOException {
        String name = entry.getName();

        if (entry.isDirectory()) {
            FileUtil.mkdir(new File(outputDir, name), context);
            return;
        }
        File outputFile = new File(outputDir, name);
        if (!outputFile.getParentFile().exists()) {
            FileUtil.mkdir(outputFile.getParentFile(), context);
        }

        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
 
        byte[] content = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        long progress=0;
        try {
            while (progress<entry.getSize()) {
                int length;
                int bytesLeft = new Long(entry.getSize()-progress).intValue();
                length = sevenzFile.read(content, 0,
                        bytesLeft>GenericCopyUtil.DEFAULT_BUFFER_SIZE ? GenericCopyUtil.DEFAULT_BUFFER_SIZE : bytesLeft);
                outputStream.write(content, 0, length);
                ServiceWatcherUtil.position+=length;
                progress+=length;
            }
        } finally {
            outputStream.close();
        }
    }

}
