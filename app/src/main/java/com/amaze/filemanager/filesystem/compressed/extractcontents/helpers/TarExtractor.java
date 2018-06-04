package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/*  Rename : extractEntry(@NonNull final Context context, TarArchiveInputStream inputStream, TarArchiveEntry entry, String outputDirectory)
    outputDir, len and buf is not good for understandability.
    I think full word is better.
    outputDir -> outputDirectory, buf -> buffer, len -> length
*/

public class TarExtractor extends Extractor {

    public TarExtractor(Context context, String filePath, String outputPath, Extractor.OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        long totalBytes = 0;
        ArrayList<TarArchiveEntry> archiveEntries = new ArrayList<>();
        TarArchiveInputStream inputStream = new TarArchiveInputStream(new FileInputStream(filePath));

        TarArchiveEntry tarArchiveEntry;

        while ((tarArchiveEntry = inputStream.getNextTarEntry()) != null) {
            if(filter.shouldExtract(tarArchiveEntry.getName(), tarArchiveEntry.isDirectory())) {
                archiveEntries.add(tarArchiveEntry);
                totalBytes += tarArchiveEntry.getSize();
            }
        }

        listener.onStart(totalBytes, archiveEntries.get(0).getName());

        inputStream.close();
        inputStream = new TarArchiveInputStream(new FileInputStream(filePath));

        for (TarArchiveEntry entry : archiveEntries) {
            if (!listener.isCancelled()) {
                listener.onUpdate(entry.getName());
                //TAR is sequential, you need to walk all the way to the file you want
                while (entry.hashCode() != inputStream.getNextTarEntry().hashCode());
                extractEntry(context, inputStream, entry, outputPath);
            }
        }
        inputStream.close();

        listener.onFinish();
    }

    @Override
    protected void streamClose(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        outputStream.close();
    }

    private void extractEntry(@NonNull final Context context, TarArchiveInputStream inputStream,
                              TarArchiveEntry entry, String outputDirectory) throws IOException {
        if (entry.isDirectory()) {
            FileUtil.mkdir(new File(outputDirectory, entry.getName()), context);
            return;
        }

        File outputFile = getOutputFile(context, outputDirectory, entry.getName());


        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
        writeBuffer(inputStream,outputStream);
    }

}
