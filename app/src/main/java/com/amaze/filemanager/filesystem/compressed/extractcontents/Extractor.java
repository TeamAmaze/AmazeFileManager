package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
/* Rename extractWithFilter((relativePath, isDirectory) ...)
    isDir is not good for understandability.
    Dir can be directory, direction and so on.
    isDir -> isDirectory
 */

/* each Extractor class had similar lines in extractEntry().
    I made it class BufferWriter.
 */

/* each Extractor class had similar lines in extractEntry().
    I made it method getOutputFile().
 */
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

        extractWithFilter((relativePath, isDirectory) -> {
            if(filesToExtract.contains(relativePath)) {
                if(!isDirectory) filesToExtract.remove(relativePath);
                return true;
            } else {// header to be extracted is at least the entry path (may be more, when it is a directory)
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
        extractWithFilter((relativePath, isDirectory) -> true);
    }

    protected abstract void extractWithFilter(@NonNull Filter filter) throws IOException;

    @NonNull
    protected File getOutputFile(@NonNull Context context, String outputDirectory, String name) {
        File outputFile = new File(outputDirectory, name);
        if (!outputFile.getParentFile().exists()) {
            FileUtil.mkdir(outputFile.getParentFile(), context);
        }
        return outputFile;
    }

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
