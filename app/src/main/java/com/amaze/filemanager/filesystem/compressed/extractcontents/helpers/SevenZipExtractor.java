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
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import java.io.BufferedInputStream;
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
                // Entry to be extracted is atleast the entry path (may be more, when it is a directory)
                arrayList.add(entry);
                totalBytes += entry.getSize();
            }
        }

        listener.onStart(totalBytes, arrayList.get(0).getName());

        SevenZArchiveEntry entry;
        while ((entry = sevenzFile.getNextEntry()) != null) {
            if (!listener.isCancelled()) {
                listener.onUpdate(entry.getName());
                extractEntry(context, sevenzFile, entry, outputPath);
            }
        }
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
        //	Log.i("Amaze", "Extracting: " + entry);

        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
        try {
            // No specific File Streams, only for the Full Archive, therefore no buffering...
            byte content[] = new byte[(int)entry.getSize()];
            ServiceWatcherUtil.position += sevenzFile.read(content, 0, content.length);
            outputStream.write(content, 0, content.length);
        } finally {
            outputStream.close();
        }
    }

}
