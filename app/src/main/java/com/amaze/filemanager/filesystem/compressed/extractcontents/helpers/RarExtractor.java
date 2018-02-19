package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class RarExtractor extends Extractor {

    public RarExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        try {
            long totalBytes = 0;
            Archive rarFile = new Archive(new File(filePath));
            ArrayList<FileHeader> arrayList = new ArrayList<>();

            // iterating archive elements to find file names that are to be extracted
            for (FileHeader header : rarFile.getFileHeaders()) {
                if (filter.shouldExtract(header.getFileNameString(), header.isDirectory())) {
                    // header to be extracted is atleast the entry path (may be more, when it is a directory)
                    arrayList.add(header);
                    totalBytes += header.getFullUnpackSize();
                }
            }

            listener.onStart(totalBytes, arrayList.get(0).getFileNameString());

            for (FileHeader entry : arrayList) {
                if (!listener.isCancelled()) {
                    listener.onUpdate(entry.getFileNameString());
                    unzipRAREntry(context, rarFile, entry, outputPath);
                }
            }
            listener.onFinish();
        } catch (RarException e) {
            throw new IOException(e);
        }
    }

    private void unzipRAREntry(@NonNull final Context context, Archive zipFile, FileHeader entry, String outputDir)
            throws RarException, IOException {
        String name = entry.getFileNameString();
        name = name.replaceAll("\\\\", CompressedHelper.SEPARATOR);
        if (entry.isDirectory()) {
            FileUtil.mkdir(new File(outputDir, name), context);
            return;
        }
        File outputFile = new File(outputDir, name);
        if (!outputFile.getParentFile().exists()) {
            FileUtil.mkdir(outputFile.getParentFile(), context);
        }
        //	Log.i("Amaze", "Extracting: " + entry);
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
