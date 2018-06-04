package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/*  Rename : extractWithFilter(@NonNull Filter filter)
    elements of arrayList is FileHeader Type but we can not understand it by reading only variable name
    arrayList -> fileHeaderArrayList

    Rename : extractEntry(@NonNull final Context context, Archive zipFile, FileHeader entry, String outputDirectory)
    outputDir, len and buf is not good for understandability.
    I think full word is better.
    outputDir -> outputDirectory, buf -> buffer, len -> length
 */

public class RarExtractor extends Extractor {

    public RarExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        try {
            long totalBytes = 0;
            Archive rarFile = new Archive(new File(filePath));
            ArrayList<FileHeader> fileHeaderArrayList = new ArrayList<>();

            // iterating archive elements to find file names that are to be extracted
            for (FileHeader header : rarFile.getFileHeaders()) {
                if (filter.shouldExtract(header.getFileNameString(), header.isDirectory())) {
                    // header to be extracted is at least the entry path (may be more, when it is a directory)
                    fileHeaderArrayList.add(header);
                    totalBytes += header.getFullUnpackSize();
                }
            }

            listener.onStart(totalBytes, fileHeaderArrayList.get(0).getFileNameString());

            for (FileHeader entry : fileHeaderArrayList) {
                if (!listener.isCancelled()) {
                    listener.onUpdate(entry.getFileNameString());
                    extractEntry(context, rarFile, entry, outputPath);
                }
            }
            listener.onFinish();
        } catch (RarException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void streamClose(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        outputStream.close();
        inputStream.close();
    }

    private void extractEntry(@NonNull final Context context, Archive zipFile, FileHeader entry, String outputDirectory)
            throws RarException, IOException {
        String name = entry.getFileNameString();
        name = name.replaceAll("\\\\", CompressedHelper.SEPARATOR);
        if (entry.isDirectory()) {
            FileUtil.mkdir(new File(outputDirectory, name), context);
            return;
        }
        File outputFile = getOutputFile(context, outputDirectory, name);
        //	Log.i("Amaze", "Extracting: " + entry);
        BufferedInputStream inputStream = new BufferedInputStream(
                zipFile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
        writeBuffer(inputStream, outputStream);
    }



}
