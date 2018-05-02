package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public abstract class Extractor {

    protected Context context;
    protected String filePath, outputPath;
    protected OnUpdate listener;

    public Extractor(Context context, String filePath, String outputPath,
                     Extractor.OnUpdate listener) {
        this.context = context;
        this.filePath = filePath;
        this.outputPath = outputPath;
        this.listener = listener;
    }

    public void extractFiles(String[] files) throws IOException {
        HashSet<String> filesToExtract = new HashSet<>(files.length);
        Collections.addAll(filesToExtract, files);

        extractWithFilter((relativePath, isDir) -> {
            if(filesToExtract.contains(relativePath)) {
                if(!isDir) filesToExtract.remove(relativePath);
                return true;
            } else {// header to be extracted is atleast the entry path (may be more, when it is a directory)
                for (String path : filesToExtract) {
                    if(relativePath.startsWith(path)) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    public void extractEverything() throws IOException {
        extractWithFilter((relativePath, isDir) -> true);
    }

    protected abstract void extractWithFilter(@NonNull Filter filter) throws IOException;

    protected interface Filter {
        public boolean shouldExtract(String relativePath, boolean isDirectory);
    }

    public interface OnUpdate {
        public void onStart(long totalBytes, String firstEntryName);
        public void onUpdate(String entryPath);
        public void onFinish();
        public boolean isCancelled();
    }
}
